#!/usr/bin/env python3
"""Prepare and publish a common release; uses only Python's standard library and gh."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET

REPO = 'Eppo-exp/sdk-common-jdk'
CENTRAL = 'https://repo.maven.apache.org/maven2/cloud/eppo/'
FRAMEWORK = 'eppo-sdk-framework'
COMMON = 'sdk-common-jvm'
STAGING = Path('build/staging-deploy')
SEMVER = re.compile(r'(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)')


def run(*args):
    return subprocess.check_output(args, text=True).strip()


def gh_json(*args):
    return json.loads(run('gh', *args))


def stable(version):
    if not SEMVER.fullmatch(version):
        raise ValueError('Expected stable MAJOR.MINOR.PATCH, got ' + version)
    return version


def read_versions():
    result = run('./gradlew', '-q', ':releaseVersions', '--console=plain')
    versions = json.loads(next(line for line in result.splitlines() if line.startswith('{')))
    return {artifact: stable(versions[artifact]) for artifact in (FRAMEWORK, COMMON)}


def framework_input(path):
    # Conservative: new build inputs are included unless explicitly administrative.
    if path == 'scripts/release/test-data-ref':
        return True
    return not (path.startswith(('eppo-sdk-common/', '.github/', 'scripts/'))
                or path.endswith('.md') or path in ('.gitignore', '.gitattributes', 'LICENSE'))


def fingerprint(artifact):
    digest = hashlib.sha256()
    for path in sorted(run('git', 'ls-files').splitlines()):
        if not framework_input(path) and not (
                artifact == COMMON and path.startswith('eppo-sdk-common/')):
            continue
        data = Path(path).read_bytes()
        digest.update(path.encode() + b'\0' + data + b'\0')
    return digest.hexdigest()


def central_file(artifact, version, filename):
    url = CENTRAL + artifact + '/' + version + '/' + filename
    try:
        with urllib.request.urlopen(url, timeout=30) as response:
            return response.read()
    except urllib.error.HTTPError as error:
        error.close()
        if error.code == 404:
            return None
        raise  # Auth/rate-limit/server errors are not missing artifacts.


def available(artifact, version):
    return central_file(artifact, version, artifact + '-' + version + '.pom') is not None


def record_name(artifact, version):
    return artifact + '-' + version + '.json'


def releases():
    pages = gh_json('api', '--paginate', '--slurp', 'repos/' + REPO + '/releases?per_page=100')
    return [release for page in pages for release in page]


def record_from(release, artifact, version):
    name = record_name(artifact, version)
    for asset in release['assets']:
        if asset['name'] == name:
            return gh_json('api', '-H', 'Accept: application/octet-stream',
                           'repos/' + REPO + '/releases/assets/' + str(asset['id']))
    return None


def required_files(artifact, version):
    base = artifact + '-' + version
    suffixes = ['.pom', '.module', '.jar', '-sources.jar', '-javadoc.jar']
    if artifact == FRAMEWORK:
        suffixes.append('-tests.jar')
    return {base + suffix for suffix in suffixes}


def validate_record(record, artifact, version, inputs):
    if (record.get('schema') != 1 or record.get('artifact') != artifact
            or record.get('version') != version
            or record.get('state') not in ('attempting', 'published')
            or not required_files(artifact, version).issubset(record.get('files', {}))):
        raise ValueError('Publication provenance is incomplete for ' + artifact + ':' + version)
    if record.get('inputs') != inputs:
        version_file = 'build.gradle' if artifact == FRAMEWORK else 'eppo-sdk-common/build.gradle'
        raise ValueError(artifact + ':' + version + ' has new source/build inputs but no version bump. '
                         + 'Bump the version in ' + version_file + ' before releasing.')
    for name, checksum in record['files'].items():
        if '/' in name or not re.fullmatch(r'[a-f0-9]{64}', checksum):
            raise ValueError('Invalid publication record')


def verify_record(record):
    complete = True
    for filename, expected in record['files'].items():
        data = central_file(record['artifact'], record['version'], filename)
        if data is None:
            complete = False
        elif hashlib.sha256(data).hexdigest() != expected:
            raise ValueError('Central content differs from publication record: ' + filename)
    return complete


def wait_for_record(record, seconds=1200):
    deadline = time.monotonic() + seconds
    while True:
        if verify_record(record):
            return
        if time.monotonic() >= deadline:
            raise ValueError('Publication is incomplete for ' + record['artifact']
                             + '. Inspect the existing Central deployment; do not re-upload blindly.'
                             + ' Once it is published, rerun this release workflow.')
        time.sleep(20)


def choose_action(record, exists):
    if record:
        return 'reuse' if record['state'] == 'published' else 'resume'
    if exists:
        raise ValueError('Coordinate exists without a publication record; refusing to overwrite/reuse it')
    return 'publish'


def make_plan(tag=None):
    if run('git', 'status', '--porcelain', '--untracked-files=normal'):
        raise ValueError('Use a clean checkout of the committed release inputs')
    fixture_ref = Path('scripts/release/test-data-ref').read_text().strip()
    if not re.fullmatch(r'[a-f0-9]{40}', fixture_ref):
        raise ValueError('Release test-data-ref must pin a full Git commit SHA')
    versions = read_versions()
    expected_tag = 'v' + versions[COMMON]
    if tag is not None and tag != expected_tag:
        raise ValueError('Release tag must be ' + expected_tag)
    sha = run('git', 'rev-parse', 'HEAD')
    existing_tag = subprocess.run(['git', 'rev-parse', '--verify',
                                   'refs/tags/' + expected_tag + '^{commit}'],
                                  text=True, capture_output=True)
    if existing_tag.returncode == 0 and existing_tag.stdout.strip() != sha:
        raise ValueError('Existing tag points at a different commit: ' + expected_tag)
    subprocess.run(['git', 'merge-base', '--is-ancestor', sha, 'origin/main'], check=True)
    all_releases = releases()
    plan = {'schema': 1, 'tag': expected_tag, 'commit': sha, 'artifacts': {}}
    for artifact, version in versions.items():
        inputs = fingerprint(artifact)
        matches = []
        for release in all_releases:
            if release['draft'] or release['prerelease']:
                continue
            record = record_from(release, artifact, version)
            if record:
                validate_record(record, artifact, version, inputs)
                matches.append(record)
        record = next((r for r in matches if r['state'] == 'published'),
                      matches[0] if matches else None)
        if matches and any(r['files'] != record['files'] for r in matches):
            raise ValueError('Conflicting publication records for ' + artifact)
        exists = available(artifact, version)
        try:
            action = choose_action(record, exists)
        except ValueError as error:
            raise ValueError(artifact + ':' + version + ': ' + str(error)) from error
        if artifact == COMMON and record and record['commit'] != sha:
            raise ValueError('Common version already belongs to another release commit')
        plan['artifacts'][artifact] = {'version': version, 'inputs': inputs,
                                      'action': action, 'record': record}
    return plan


def save_json(path, value):
    path.write_text(json.dumps(value, indent=2) + '\n')


def upload_record(tag, record):
    with tempfile.TemporaryDirectory() as directory:
        path = Path(directory) / record_name(record['artifact'], record['version'])
        save_json(path, record)
        subprocess.run(['gh', 'release', 'upload', tag, str(path), '--clobber', '--repo', REPO], check=True)


def previous_tag(patterns):
    command = ['git', 'describe', '--tags', '--abbrev=0']
    for pattern in patterns:
        command.extend(['--match', pattern])
    result = subprocess.run(command + ['HEAD^'], text=True, capture_output=True)
    return result.stdout.strip() if result.returncode == 0 else None


def draft(plan):
    lines = ['## Packages', '', '| Package | Version | Action |', '|---|---|---|']
    for artifact, item in plan['artifacts'].items():
        lines.append('| ' + artifact + ' | ' + item['version'] + ' | ' + item['action'] + ' |')
    for artifact in (COMMON, FRAMEWORK):
        item = plan['artifacts'][artifact]
        if artifact == FRAMEWORK and item['action'] == 'reuse':
            continue
        previous = previous_tag(['v[0-9]*', 'sdk-common-jvm-v*']) if artifact == COMMON else None
        # Framework versions need not have their own tags: use their recorded source commit.
        if artifact == FRAMEWORK:
            for release in releases():
                if release['draft'] or release['prerelease']:
                    continue
                records = [a for a in release['assets'] if a['name'].startswith(FRAMEWORK + '-')
                           and a['name'].endswith('.json')]
                for asset in records:
                    record = gh_json('api', '-H', 'Accept: application/octet-stream',
                                     'repos/' + REPO + '/releases/assets/' + str(asset['id']))
                    if record.get('state') == 'published':
                        candidate = record['commit']
                        if subprocess.run(['git', 'merge-base', '--is-ancestor', candidate, 'HEAD'],
                                          capture_output=True).returncode == 0:
                            previous = candidate
                            break
                if previous:
                    break
        revision = previous + '..HEAD' if previous else 'HEAD'
        paths = ['.'] if artifact == COMMON else ['.', ':(exclude)eppo-sdk-common/',
                                                   ':(exclude).github/', ':(exclude)scripts/']
        notes = run('git', 'log', '--format=- %s (%h)', revision, '--', *paths)
        lines.extend(['', '## ' + artifact, '', notes or 'No changes recorded.'])
    lines.extend(['', 'Publishing this release starts package publication. Check the release workflow',
                  'for completion; publication records are attached after verification.'])
    with tempfile.TemporaryDirectory() as directory:
        path = Path(directory) / 'notes.md'
        path.write_text('\n'.join(lines) + '\n')
        subprocess.run(['gh', 'release', 'create', plan['tag'], '--draft', '--target', plan['commit'],
                        '--title', 'sdk-common-jvm ' + plan['artifacts'][COMMON]['version'],
                        '--notes-file', str(path), '--repo', REPO], check=True)


def stage_record(artifact, item, plan):
    shutil.rmtree(STAGING, ignore_errors=True)
    task = ':publish' if artifact == FRAMEWORK else ':eppo-sdk-common:publish'
    subprocess.run(['./gradlew', task, '-Prelease'], check=True)
    directory = STAGING / 'cloud/eppo' / artifact / item['version']
    files = {path.name: hashlib.sha256(path.read_bytes()).hexdigest()
             for path in directory.iterdir() if path.suffix in ('.jar', '.pom', '.module')}
    record = {'schema': 1, 'artifact': artifact, 'version': item['version'],
              'inputs': item['inputs'], 'commit': plan['commit'], 'state': 'attempting',
              'files': files, 'run': os.environ.get('GITHUB_RUN_ID')}
    validate_record(record, artifact, item['version'], item['inputs'])
    if artifact == COMMON:
        pom = ET.parse(directory / (artifact + '-' + item['version'] + '.pom'))
        ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
        deps = [(d.findtext('m:groupId', namespaces=ns), d.findtext('m:artifactId', namespaces=ns),
                 d.findtext('m:version', namespaces=ns)) for d in pom.findall('m:dependencies/m:dependency', ns)]
        expected = ('cloud.eppo', FRAMEWORK, plan['artifacts'][FRAMEWORK]['version'])
        if expected not in deps or any(v and v.endswith('-SNAPSHOT') for _, _, v in deps):
            raise ValueError('Common POM must reference the selected stable framework')
    return record


def publish(plan):
    # No package upload before all tests have passed in the calling workflow.
    for artifact in (FRAMEWORK, COMMON):
        item = plan['artifacts'][artifact]
        record = item['record']
        if item['action'] == 'publish':
            record = stage_record(artifact, item, plan)
            # Persist intent and exact checksums BEFORE contacting Central. If the runner
            # disappears, a rerun must reconcile this attempt instead of uploading twice.
            upload_record(plan['tag'], record)
            # Do not let JReleaser reuse framework outputs during the common deployment.
            shutil.rmtree(Path('build/jreleaser'), ignore_errors=True)
            subprocess.run(['./gradlew', 'jreleaserDeploy'], check=True)
        wait_for_record(record)
        record['state'] = 'published'
        upload_record(plan['tag'], record)
        print('Verified ' + artifact + ':' + item['version'], flush=True)
    verify_consumer(plan)
    summary = os.environ.get('GITHUB_STEP_SUMMARY')
    if summary:
        with open(summary, 'a') as output:
            output.write('## Maven Central publication verified\n\n')
            for artifact, item in plan['artifacts'].items():
                output.write('- [' + artifact + ':' + item['version'] + ']('
                             + CENTRAL + artifact + '/' + item['version'] + '/)\n')


def verify_consumer(plan, repository='https://repo.maven.apache.org/maven2'):
    # A separate build has no project dependencies or mavenLocal fallback.
    version = plan['artifacts'][COMMON]['version']
    framework = plan['artifacts'][FRAMEWORK]['version']
    with tempfile.TemporaryDirectory() as directory:
        target = Path(directory)
        (target / 'settings.gradle').write_text("rootProject.name = 'release-consumer'\n")
        (target / 'build.gradle').write_text("""
plugins { id 'java' }
repositories {
  maven { url '%s' }
  mavenCentral()
}
dependencies {
  implementation 'cloud.eppo:sdk-common-jvm:%s'
  testImplementation 'cloud.eppo:eppo-sdk-framework:%s:tests'
}
tasks.register('verifyRelease') {
  doLast {
    def artifacts = configurations.testRuntimeClasspath.resolvedConfiguration.resolvedArtifacts
    assert artifacts.any { it.moduleVersion.id.group == 'cloud.eppo' &&
      it.name == 'sdk-common-jvm' && it.moduleVersion.id.version == '%s' }
    assert artifacts.any { it.moduleVersion.id.group == 'cloud.eppo' &&
      it.name == 'eppo-sdk-framework' && it.moduleVersion.id.version == '%s' &&
      it.classifier == null }
    assert artifacts.any { it.name == 'eppo-sdk-framework' && it.classifier == 'tests' }
  }
}
""" % (repository, version, framework, version, framework))
        subprocess.run([str(Path('gradlew').resolve()), '-p', directory,
                        'verifyRelease', '--refresh-dependencies'], check=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['plan', 'draft', 'publish'])
    parser.add_argument('--tag')
    args = parser.parse_args()
    plan = make_plan(args.tag)
    print(json.dumps(plan, indent=2), flush=True)
    if args.command == 'draft':
        draft(plan)
    elif args.command == 'publish':
        if not args.tag:
            raise ValueError('Publishing requires an explicit --tag')
        if run('git', 'rev-parse', 'refs/tags/' + args.tag + '^{commit}') != plan['commit']:
            raise ValueError('Checkout does not match the release tag')
        publish(plan)


if __name__ == '__main__':
    try:
        main()
    except (ValueError, subprocess.CalledProcessError, urllib.error.URLError) as error:
        sys.exit(str(error))
