import hashlib
import importlib.util
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import urllib.error

spec = importlib.util.spec_from_file_location('release', Path(__file__).parents[1] / 'release.py')
r = importlib.util.module_from_spec(spec)
spec.loader.exec_module(r)


def record(artifact=r.FRAMEWORK, state='published'):
    return {'schema': 1, 'artifact': artifact, 'version': '0.1.0', 'inputs': 'inputs',
            'commit': 'commit', 'state': state,
            'files': {name: hashlib.sha256(b'published').hexdigest()
                      for name in r.required_files(artifact, '0.1.0')}}


class ReleaseTests(unittest.TestCase):
    def test_stable_versions_only(self):
        for version in ['0.1.0', '4.0.0', '12.31.0']:
            self.assertEqual(r.stable(version), version)
        for version in ['4.0.0-SNAPSHOT', 'v4.0.0', '04.0.0', '4.0.0-rc.1', '4.0.0\n', '$(id)']:
            with self.assertRaises(ValueError):
                r.stable(version)

    def test_first_release_publishes_both(self):
        self.assertEqual(r.choose_action(None, False), 'publish')

    def test_common_only_reuses_framework(self):
        self.assertEqual(r.choose_action(record(), True), 'reuse')
        self.assertEqual(r.choose_action(None, False), 'publish')

    def test_unknown_existing_coordinate_fails_closed(self):
        with self.assertRaisesRegex(ValueError, 'without a publication record'):
            r.choose_action(None, True)

    def test_changed_framework_without_bump_fails(self):
        with self.assertRaisesRegex(ValueError, 'no version bump'):
            r.validate_record(record(), r.FRAMEWORK, '0.1.0', 'changed inputs')

    def test_framework_tests_classifier_required(self):
        value = record()
        del value['files']['eppo-sdk-framework-0.1.0-tests.jar']
        with self.assertRaises(ValueError):
            r.validate_record(value, r.FRAMEWORK, '0.1.0', 'inputs')

    def test_shared_build_and_fixture_pin_are_framework_inputs(self):
        for path in ['build.gradle', 'settings.gradle', 'gradle.properties', 'Makefile',
                     'gradle/wrapper/gradle-wrapper.properties', 'scripts/release/test-data-ref',
                     'src/main/java/Example.java', 'src/test/java/ExampleTest.java', 'new-build-input']:
            self.assertTrue(r.framework_input(path), path)
        for path in ['eppo-sdk-common/build.gradle', 'README.md', '.github/workflows/test.yml',
                     'scripts/release/release.py']:
            self.assertFalse(r.framework_input(path), path)

    def test_only_404_is_absence(self):
        for code in [401, 403, 429, 500]:
            with patch.object(r.urllib.request, 'urlopen', side_effect=urllib.error.HTTPError(
                    'https://example.test', code, 'error', {}, None)):
                with self.assertRaises(urllib.error.HTTPError):
                    r.available(r.FRAMEWORK, '0.1.0')
        with patch.object(r.urllib.request, 'urlopen', side_effect=urllib.error.HTTPError(
                'https://example.test', 404, 'missing', {}, None)):
            self.assertFalse(r.available(r.FRAMEWORK, '0.1.0'))

    def test_resume_requires_all_matching_files(self):
        value = record(state='attempting')
        self.assertEqual(r.choose_action(value, False), 'resume')
        with patch.object(r, 'central_file', return_value=b'published'):
            self.assertTrue(r.verify_record(value))
        with patch.object(r, 'central_file', return_value=None):
            self.assertFalse(r.verify_record(value))
        with patch.object(r, 'central_file', return_value=b'different'):
            with self.assertRaisesRegex(ValueError, 'differs'):
                r.verify_record(value)

    def test_pending_deployment_is_not_uploaded_again(self):
        value = record(state='attempting')
        plan = {'tag': 'v4.0.0', 'artifacts': {
            r.FRAMEWORK: {'action': 'resume', 'record': value, 'version': '0.1.0'}}}
        with patch.object(r, 'wait_for_record', side_effect=ValueError('pending')), \
                patch.object(r, 'stage_record') as stage, patch.object(r, 'run') as run:
            with self.assertRaisesRegex(ValueError, 'pending'):
                r.publish(plan)
            stage.assert_not_called()
            run.assert_not_called()

    def test_resume_framework_success_then_publish_common(self):
        framework = record()
        common = record(r.COMMON, 'attempting')
        plan = {'tag': 'v4.0.0', 'artifacts': {
            r.FRAMEWORK: {'action': 'reuse', 'record': framework, 'version': '0.1.0'},
            r.COMMON: {'action': 'publish', 'record': None, 'version': '4.0.0'}}}
        with patch.object(r, 'wait_for_record') as wait, \
                patch.object(r, 'stage_record', return_value=common) as stage, \
                patch.object(r, 'upload_record') as upload, \
                patch.object(r, 'verify_consumer') as consumer, \
                patch.object(r.subprocess, 'run') as deploy, patch.object(r.shutil, 'rmtree'):
            r.publish(plan)
            stage.assert_called_once_with(r.COMMON, plan['artifacts'][r.COMMON], plan)
            deploy.assert_called_once_with(['./gradlew', 'jreleaserDeploy'], check=True)
            self.assertEqual(wait.call_count, 2)
            self.assertEqual(upload.call_count, 3)
            consumer.assert_called_once_with(plan)

    def test_persist_intent_before_deploy(self):
        plan = {'tag': 'v4.0.0', 'artifacts': {
            r.FRAMEWORK: {'action': 'publish', 'record': None, 'version': '0.1.0'}}}
        with patch.object(r, 'stage_record', return_value=record(state='attempting')), \
                patch.object(r, 'upload_record', side_effect=ValueError('cannot persist')), \
                patch.object(r.subprocess, 'run') as deploy:
            with self.assertRaisesRegex(ValueError, 'cannot persist'):
                r.publish(plan)
            deploy.assert_not_called()

    def test_plan_rejects_wrong_tag_and_detached_version(self):
        with patch.object(r, 'run', return_value=''), \
                patch.object(r, 'read_versions', return_value={r.FRAMEWORK: '0.1.0', r.COMMON: '4.0.0'}):
            with self.assertRaisesRegex(ValueError, 'must be v4.0.0'):
                r.make_plan('v0.1.0')

    def test_plan_rejects_dirty_checkout(self):
        with patch.object(r, 'run', return_value=' M build.gradle'):
            with self.assertRaisesRegex(ValueError, 'clean checkout'):
                r.make_plan()

    def test_pom_verification_rejects_snapshot_dependency(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            target = root / 'cloud/eppo/sdk-common-jvm/0.1.0'
            target.mkdir(parents=True)
            for name in r.required_files(r.COMMON, '0.1.0'):
                (target / name).write_bytes(b'jar')
            (target / 'sdk-common-jvm-0.1.0.pom').write_text('''
<project xmlns="http://maven.apache.org/POM/4.0.0"><dependencies><dependency>
<groupId>cloud.eppo</groupId><artifactId>eppo-sdk-framework</artifactId>
<version>0.1.0-SNAPSHOT</version></dependency></dependencies></project>''')
            with patch.object(r, 'STAGING', root), patch.object(r.shutil, 'rmtree'), \
                    patch.object(r.subprocess, 'run'):
                with self.assertRaisesRegex(ValueError, 'stable framework'):
                    r.stage_record(r.COMMON, {'version': '0.1.0', 'inputs': 'inputs'},
                                   {'commit': 'commit', 'artifacts': {r.FRAMEWORK: {'version': '0.1.0'}}})


    def test_common_changes_without_version_bump_fail(self):
        with self.assertRaisesRegex(ValueError, 'sdk-common-jvm.*no version bump'):
            r.validate_record(record(r.COMMON), r.COMMON, '0.1.0', 'changed common inputs')

    def test_full_plan_first_release_common_only_and_missing_bumps(self):
        versions = {r.FRAMEWORK: '0.1.0', r.COMMON: '4.0.0'}
        framework = record()
        old_common = record(r.COMMON)
        old_common['version'] = '4.0.0'
        old_common['files'] = {name: hashlib.sha256(b'published').hexdigest()
                               for name in r.required_files(r.COMMON, '4.0.0')}
        history = [{'draft': False, 'prerelease': False}]

        def command(*args):
            if args[:2] == ('git', 'status'):
                return ''
            return 'commit'

        def provenance(release, artifact, version):
            return records.get((artifact, version))

        cases = [
            ({}, {}, {r.FRAMEWORK: 'publish', r.COMMON: 'publish'}, None),
            ({(r.FRAMEWORK, '0.1.0'): framework}, {},
             {r.FRAMEWORK: 'reuse', r.COMMON: 'publish'}, None),
            ({(r.FRAMEWORK, '0.1.0'): framework}, {r.FRAMEWORK: 'changed'}, None,
             'eppo-sdk-framework.*no version bump'),
            ({(r.FRAMEWORK, '0.1.0'): framework, (r.COMMON, '4.0.0'): old_common},
             {r.COMMON: 'changed'}, None, 'sdk-common-jvm.*no version bump'),
        ]
        for records, changed, expected, error in cases:
            with self.subTest(expected=expected, error=error), \
                    patch.object(r, 'run', side_effect=command), \
                    patch.object(r, 'read_versions', return_value=versions), \
                    patch.object(r.subprocess, 'run', return_value=subprocess.CompletedProcess([], 1, '')), \
                    patch.object(r, 'releases', return_value=history), \
                    patch.object(r, 'record_from', side_effect=provenance), \
                    patch.object(r, 'available', side_effect=lambda a, v: (a, v) in records), \
                    patch.object(r, 'fingerprint', side_effect=lambda a: changed.get(a, 'inputs')):
                if error:
                    with self.assertRaisesRegex(ValueError, error):
                        r.make_plan('v4.0.0')
                else:
                    plan = r.make_plan('v4.0.0')
                    self.assertEqual({a: v['action'] for a, v in plan['artifacts'].items()}, expected)

    def test_fingerprint_detects_real_source_build_and_pin_changes(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            tracked = ['src/main/Test.java', 'eppo-sdk-common/src/main/Common.java',
                       'build.gradle', 'scripts/release/test-data-ref', 'README.md']
            for name in tracked:
                path = root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text('original')
            import os
            previous = Path.cwd()
            try:
                os.chdir(root)
                with patch.object(r, 'run', return_value='\n'.join(tracked)):
                    framework = r.fingerprint(r.FRAMEWORK)
                    common = r.fingerprint(r.COMMON)
                    (root / tracked[1]).write_text('common change')
                    self.assertEqual(r.fingerprint(r.FRAMEWORK), framework)
                    self.assertNotEqual(r.fingerprint(r.COMMON), common)
                    (root / 'README.md').write_text('documentation change')
                    self.assertEqual(r.fingerprint(r.FRAMEWORK), framework)
                    for name in [tracked[0], 'build.gradle', 'scripts/release/test-data-ref']:
                        (root / name).write_text('changed')
                        self.assertNotEqual(r.fingerprint(r.FRAMEWORK), framework, name)
                        (root / name).write_text('original')
            finally:
                os.chdir(previous)


    def test_draft_is_pinned_and_contains_both_versions(self):
        plan = {'tag': 'v4.0.0', 'commit': 'exact-reviewed-sha', 'artifacts': {
            r.FRAMEWORK: {'version': '0.1.0', 'action': 'publish'},
            r.COMMON: {'version': '4.0.0', 'action': 'publish'}}}
        captured = {}

        def create(command, **kwargs):
            captured['command'] = command
            captured['notes'] = Path(command[command.index('--notes-file') + 1]).read_text()

        with patch.object(r, 'previous_tag', return_value='v3.13.2'), \
                patch.object(r, 'releases', return_value=[]), \
                patch.object(r, 'run', return_value='- Changes'), \
                patch.object(r.subprocess, 'run', side_effect=create):
            r.draft(plan)
        command = captured['command']
        self.assertIn('--draft', command)
        self.assertEqual(command[command.index('--target') + 1], 'exact-reviewed-sha')
        self.assertIn('sdk-common-jvm | 4.0.0 | publish', captured['notes'])
        self.assertIn('eppo-sdk-framework | 0.1.0 | publish', captured['notes'])

    def test_mutable_fixture_reference_is_rejected(self):
        with patch.object(r, 'run', return_value=''), patch.object(Path, 'read_text', return_value='main'):
            with self.assertRaisesRegex(ValueError, 'full Git commit SHA'):
                r.make_plan()

    def test_existing_tag_must_match_checkout(self):
        with patch.object(r, 'run', side_effect=['', 'new-sha']), \
                patch.object(r, 'read_versions', return_value={r.FRAMEWORK: '0.1.0', r.COMMON: '4.0.0'}), \
                patch.object(r.subprocess, 'run', return_value=subprocess.CompletedProcess([], 0, 'old-sha')):
            with self.assertRaisesRegex(ValueError, 'different commit'):
                r.make_plan('v4.0.0')

    def test_main_ancestry_failure_stops_before_publication_lookup(self):
        with patch.object(r, 'run', side_effect=['', 'off-main-sha']), \
                patch.object(r, 'read_versions', return_value={r.FRAMEWORK: '0.1.0', r.COMMON: '4.0.0'}), \
                patch.object(r.subprocess, 'run', side_effect=[subprocess.CompletedProcess([], 1, ''),
                    subprocess.CalledProcessError(1, ['git', 'merge-base'])]), \
                patch.object(r, 'releases') as releases:
            with self.assertRaises(subprocess.CalledProcessError):
                r.make_plan('v4.0.0')
            releases.assert_not_called()

    def test_pending_publication_wait_is_bounded(self):
        with patch.object(r, 'verify_record', return_value=False), patch.object(r.time, 'sleep') as sleep:
            with self.assertRaisesRegex(ValueError, 'Inspect the existing Central deployment'):
                r.wait_for_record(record(), seconds=0)
            sleep.assert_not_called()


if __name__ == '__main__':
    unittest.main()
