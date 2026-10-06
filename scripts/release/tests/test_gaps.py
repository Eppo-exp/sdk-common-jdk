import hashlib
import importlib.util
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('release', Path(__file__).parents[1] / 'release.py')
r = importlib.util.module_from_spec(spec)
spec.loader.exec_module(r)


def record(artifact=r.FRAMEWORK, state='attempting'):
    return {'schema': 1, 'artifact': artifact, 'version': '0.1.0', 'inputs': 'inputs',
            'commit': 'commit', 'state': state,
            'files': {name: hashlib.sha256(b'published').hexdigest()
                      for name in r.required_files(artifact, '0.1.0')}}


def common_record(commit):
    value = record(r.COMMON)
    value.update(version='4.0.0', commit=commit,
                 files={name: hashlib.sha256(b'published').hexdigest()
                        for name in r.required_files(r.COMMON, '4.0.0')})
    return value


class GapTests(unittest.TestCase):
    # ---- G1: make_plan commit ownership (kills M10, X15) ----
    def plan_with(self, records, history=None):
        with patch.object(r, 'run', side_effect=lambda *a: '' if a[:2] == ('git', 'status') else 'head-commit'), \
                patch.object(Path, 'read_text', return_value='0' * 40), \
                patch.object(r, 'read_versions', return_value={r.FRAMEWORK: '0.1.0', r.COMMON: '4.0.0'}), \
                patch.object(r.subprocess, 'run', side_effect=lambda args, **kw:
                             subprocess.CompletedProcess(args, 1 if args[1] == 'rev-parse' else 0, '')), \
                patch.object(r, 'releases', return_value=history or [{'draft': False, 'prerelease': False}]), \
                patch.object(r, 'record_from', side_effect=lambda release, a, v: release.get('records', records).get((a, v))), \
                patch.object(r, 'available', side_effect=lambda a, v: True), \
                patch.object(r, 'verify_record', return_value=True), \
                patch.object(r, 'fingerprint', return_value='inputs'):
            return r.make_plan('v4.0.0')

    def test_framework_reuse_crosses_commits_but_common_cannot(self):
        framework = dict(record(), commit='older-release-commit')
        plan = self.plan_with({(r.FRAMEWORK, '0.1.0'): framework,
                               (r.COMMON, '4.0.0'): common_record('head-commit')})
        self.assertEqual({a: v['action'] for a, v in plan['artifacts'].items()},
                         {r.FRAMEWORK: 'reuse', r.COMMON: 'reuse'})
        with self.assertRaisesRegex(ValueError, 'another release commit'):
            self.plan_with({(r.FRAMEWORK, '0.1.0'): framework,
                            (r.COMMON, '4.0.0'): common_record('older-release-commit')})

    # ---- G2: stage_record happy path / partial POM checks / task / file filter (M12b, M12c, M12d, X7, X8) ----
    def stage(self, artifact, version, dependencies=(), framework='0.1.0'):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            target = root / 'cloud/eppo' / artifact / version
            target.mkdir(parents=True)
            for name in r.required_files(artifact, version):
                (target / name).write_bytes(b'jar')
                (target / (name + '.sha1')).write_text('gradle checksum sidecar')
            (target / (artifact + '-' + version + '.pom')).write_text(
                '<project xmlns="http://maven.apache.org/POM/4.0.0"><dependencies>'
                + ''.join('<dependency><groupId>%s</groupId><artifactId>%s</artifactId>'
                          '<version>%s</version></dependency>' % d for d in dependencies)
                + '</dependencies></project>')
            with patch.object(r, 'STAGING', root), patch.object(r.shutil, 'rmtree'), \
                    patch.object(r.subprocess, 'run') as gradle:
                value = r.stage_record(artifact, {'version': version, 'inputs': 'inputs'},
                                       {'commit': 'commit', 'artifacts': {r.FRAMEWORK: {'version': framework}}})
            task = ':publish' if artifact == r.FRAMEWORK else ':eppo-sdk-common:publish'
            gradle.assert_called_once_with(['./gradlew', task, '-Prelease'], check=True)
            return value

    def test_stage_record_accepts_selected_stable_framework(self):
        value = self.stage(r.COMMON, '4.0.0', [('cloud.eppo', r.FRAMEWORK, '0.1.0'),
                                                 ('com.squareup.okhttp3', 'okhttp', '4.12.0')])
        self.assertEqual(set(value['files']), r.required_files(r.COMMON, '4.0.0'))
        self.assertEqual((value['state'], value['commit'], value['inputs']), ('attempting', 'commit', 'inputs'))
        framework = self.stage(r.FRAMEWORK, '0.1.0')
        self.assertEqual(set(framework['files']), r.required_files(r.FRAMEWORK, '0.1.0'))

    def test_stage_record_rejects_other_framework_version_and_any_snapshot(self):
        for deps in ([('cloud.eppo', r.FRAMEWORK, '0.0.9')],
                     [('cloud.eppo', r.FRAMEWORK, '0.1.0'), ('com.example', 'lib', '1.0-SNAPSHOT')]):
            with self.subTest(deps=deps), self.assertRaisesRegex(ValueError, 'stable framework'):
                self.stage(r.COMMON, '4.0.0', deps)

    # ---- G3: wait_for_record default bound and polling (M14c, M14d) ----
    def test_wait_polls_central_until_files_sync(self):
        clock = iter(range(0, 100000, 20))
        with patch.object(r, 'verify_record', side_effect=[False, False, True]) as verify, \
                patch.object(r.time, 'monotonic', side_effect=lambda: next(clock)), \
                patch.object(r.time, 'sleep') as sleep:
            r.wait_for_record(record())
        self.assertEqual(verify.call_count, 3)
        self.assertEqual(sleep.call_count, 2)
        self.assertTrue(all(c.args[0] > 0 for c in sleep.call_args_list))

    # ---- G4: draft/prerelease records ignored (X1) ----
    def test_draft_and_prerelease_records_are_not_provenance(self):
        records = {(r.FRAMEWORK, '0.1.0'): record()}
        history = [{'draft': True, 'prerelease': False, 'records': records},
                   {'draft': False, 'prerelease': True, 'records': records}]
        with self.assertRaisesRegex(ValueError, 'eppo-sdk-framework:0.1.0: .*without a publication record'):
            self.plan_with({}, history)

    # ---- G5: conflicting records (X2) ----
    def test_conflicting_records_fail_closed(self):
        other = record()
        other['files'] = dict(other['files'], **{'eppo-sdk-framework-0.1.0.jar': 'b' * 64})
        history = [{'draft': False, 'prerelease': False, 'records': {(r.FRAMEWORK, '0.1.0'): record()}},
                   {'draft': False, 'prerelease': False, 'records': {(r.FRAMEWORK, '0.1.0'): other}}]
        with self.assertRaisesRegex(ValueError, 'Conflicting publication records'):
            self.plan_with({}, history)

    # ---- G6: main() tag/checkout guard (X3) ----
    def test_publish_requires_tag_at_planned_commit(self):
        with patch.object(r.sys, 'argv', ['release.py', 'publish', '--tag', 'v4.0.0']), \
                patch.object(r, 'make_plan', return_value={'commit': 'planned'}), \
                patch.object(r, 'run', return_value='other-commit'), \
                patch.object(r, 'publish') as publish, patch('builtins.print'):
            with self.assertRaisesRegex(ValueError, 'does not match the release tag'):
                r.main()
            publish.assert_not_called()

    # ---- G7: record identity and file-name/checksum format (X4, X5) ----
    def test_record_identity_names_and_checksums_are_validated(self):
        wrong_identity = dict(record(), artifact=r.COMMON)  # framework files, wrong artifact field
        traversal = record()
        traversal['files']['../../other/evil.pom'] = 'a' * 64
        bad_checksum = record()
        bad_checksum['files']['eppo-sdk-framework-0.1.0.pom'] = 'not-a-sha256'
        for value in (wrong_identity, traversal, bad_checksum):
            with self.subTest(value=value), self.assertRaises(ValueError):
                r.validate_record(value, r.FRAMEWORK, '0.1.0', 'inputs')

    # ---- G8: JReleaser output cleared before each deploy (X6) ----
    def test_jreleaser_outputs_cleared_before_each_deploy(self):
        events = []
        plan = {'tag': 'v4.0.0', 'artifacts': {
            a: {'action': 'publish', 'record': None, 'version': '0.1.0'} for a in (r.FRAMEWORK, r.COMMON)}}
        with patch.object(r, 'stage_record', side_effect=lambda a, *_: record(a, 'attempting')), \
                patch.object(r, 'upload_record'), patch.object(r, 'wait_for_record'), \
                patch.object(r, 'verify_consumer'), \
                patch.object(r.shutil, 'rmtree', side_effect=lambda p, **_: events.append(('rmtree', str(p)))), \
                patch.object(r.subprocess, 'run', side_effect=lambda c, **_: events.append(('run', c[-1]))):
            r.publish(plan)
        calls = [i for i, event in enumerate(events) if event[0] == 'run']
        self.assertEqual([events[i] for i in calls].count(('run', 'jreleaserDeploy')), 2)
        for i in calls:  # every JReleaser call starts from cleared outputs
            self.assertEqual(events[i - 1], ('rmtree', 'build/jreleaser'))

    # ---- G9: consumer build template (X10) ----
    def test_consumer_build_resolves_planned_coordinates(self):
        plan = {'artifacts': {r.FRAMEWORK: {'version': '0.1.0'}, r.COMMON: {'version': '4.0.0'}}}
        seen = {}

        def gradle(command, **kwargs):
            seen['command'] = command
            seen['build'] = (Path(command[2]) / 'build.gradle').read_text()

        with patch.object(r.subprocess, 'run', side_effect=gradle):
            r.verify_consumer(plan, repository='file:///staging')
        build = seen['build']
        for expected in ["maven { url 'file:///staging' }",
                         "implementation 'cloud.eppo:sdk-common-jvm:4.0.0'",
                         "testImplementation 'cloud.eppo:eppo-sdk-framework:0.1.0:tests'",
                         "it.name == 'sdk-common-jvm' && it.moduleVersion.id.version == '4.0.0'",
                         "it.name == 'eppo-sdk-framework' && it.moduleVersion.id.version == '0.1.0'"]:
            self.assertIn(expected, build)
        self.assertNotIn('mavenLocal', build)
        self.assertEqual(seen['command'][3:], ['verifyRelease', '--refresh-dependencies'])

    # ---- G10: read_versions parsing and stable gate (X12) ----
    def test_read_versions_parses_gradle_json_and_rejects_snapshots(self):
        with patch.object(r, 'run', return_value='Daemon started\n{"eppo-sdk-framework":"0.1.0","sdk-common-jvm":"4.0.0"}'):
            self.assertEqual(r.read_versions(), {r.FRAMEWORK: '0.1.0', r.COMMON: '4.0.0'})
        with patch.object(r, 'run', return_value='{"eppo-sdk-framework":"0.2.0-SNAPSHOT","sdk-common-jvm":"4.0.1"}'):
            with self.assertRaises(ValueError):
                r.read_versions()

    # ---- G11: release listing paginates (X11) ----
    def test_releases_reads_every_page(self):
        with patch.object(r, 'run', return_value='[[{"id": 1}], [{"id": 2}]]') as run:
            self.assertEqual(r.releases(), [{'id': 1}, {'id': 2}])
        run.assert_called_once_with('gh', 'api', '--paginate', '--slurp',
                                    'repos/Eppo-exp/sdk-common-jdk/releases?per_page=100')


class CentralUrlGap(unittest.TestCase):
    # ---- G12: Central URL layout (X19) ----
    def test_central_coordinates_use_maven_layout(self):
        with patch.object(r.urllib.request, 'urlopen') as urlopen:
            urlopen.return_value.__enter__.return_value.read.return_value = b'pom'
            self.assertEqual(r.central_file(r.COMMON, '4.0.0', 'sdk-common-jvm-4.0.0.pom'), b'pom')
        urlopen.assert_called_once_with(
            'https://repo.maven.apache.org/maven2/cloud/eppo/sdk-common-jvm/4.0.0/sdk-common-jvm-4.0.0.pom',
            timeout=30)


class FeedbackRegressionTests(unittest.TestCase):
    def test_failed_dryrun_does_not_record_or_deploy(self):
        plan = {'tag': 'v4.0.0', 'artifacts': {
            r.FRAMEWORK: {'action': 'publish', 'record': None, 'version': '0.1.0'}}}
        error = subprocess.CalledProcessError(1, ['dryrun'])
        with patch.object(r, 'stage_record', return_value=record()), \
                patch.object(r, 'upload_record') as upload, \
                patch.object(r.shutil, 'rmtree'), \
                patch.object(r.subprocess, 'run', side_effect=error) as gradle:
            with self.assertRaises(subprocess.CalledProcessError):
                r.publish(plan)
            upload.assert_not_called()
            gradle.assert_called_once_with(['./gradlew', 'jreleaserDeploy', '--dryrun'], check=True)

    def test_diagnostics_filter_real_git_changes(self):
        previous = Path.cwd()
        with tempfile.TemporaryDirectory() as directory:
            try:
                os.chdir(directory)
                git = ['git', '-c', 'user.name=Test', '-c', 'user.email=test@example.invalid']
                subprocess.run(git + ['init', '-q'], check=True)
                for name in ['Makefile', 'README.md', 'eppo-sdk-common/common.java']:
                    path = Path(name)
                    path.parent.mkdir(parents=True, exist_ok=True)
                    path.write_text('before')
                subprocess.run(git + ['add', '.'], check=True)
                subprocess.run(git + ['commit', '-qm', 'baseline'], check=True)
                sha = subprocess.check_output(git + ['rev-parse', 'HEAD'], text=True).strip()
                for name in ['Makefile', 'README.md', 'eppo-sdk-common/common.java']:
                    Path(name).write_text('after')
                subprocess.run(git + ['commit', '-qam', 'changed'], check=True)
                self.assertEqual(r.changed_inputs(r.FRAMEWORK, sha), ['Makefile'])
                self.assertEqual(r.changed_inputs(r.COMMON, sha), ['Makefile', 'eppo-sdk-common/common.java'])
                with self.assertRaisesRegex(ValueError, r'changed: Makefile\)'):
                    r.validate_record(dict(record(), commit=sha), r.FRAMEWORK, '0.1.0', 'different')
                self.assertEqual(r.changed_inputs(r.FRAMEWORK, 'f' * 40), [])
            finally:
                os.chdir(previous)

    def test_invalid_diagnostic_commit_is_not_executed(self):
        with patch.object(r.subprocess, 'run') as command:
            for commit in [None, '--output=/tmp/test', 'HEAD', 'abc', ['bad']]:
                self.assertEqual(r.changed_inputs(r.FRAMEWORK, commit), [])
            command.assert_not_called()

    def test_only_intent_state_is_valid_provenance(self):
        for state in ['published', None, 'failed']:
            with self.subTest(state=state), self.assertRaises(ValueError):
                r.validate_record(record(state=state), r.FRAMEWORK, '0.1.0', 'inputs')


if __name__ == '__main__':
    unittest.main()
