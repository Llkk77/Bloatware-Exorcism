import hashlib
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

MODULE = Path(__file__).resolve().parents[1] / 'rescue-module'
TX = '01234567-89ab-cdef-0123-456789abcdef'
FP = hashlib.sha256(b'TEST_ROM').hexdigest()

MOCKS = r'''
ex_prop() { case "$1" in ro.build.fingerprint) cat "$EX_STATE/mock_fp";; sys.boot_completed) cat "$EX_STATE/mock_completed";; esac; }
ex_bootid() { cat "$EX_STATE/mock_boot"; }
sleep() { :; }
ex_pm() {
    printf 'pm|%s\n' "$*" >> "$EX_STATE/mock_calls"
    if [ "$1" = list ]; then
        [ ! -f "$EX_STATE/mock_unavailable" ] && printf 'package:android\n'
        return
    fi
    [ ! -f "$EX_STATE/mock_fail" ] || return 1
    case "$1" in default-state) mock_enabled=0;; enable) mock_enabled=1;; disable) mock_enabled=2;; disable-user) mock_enabled=3;; disable-until-used) mock_enabled=4;; *) return 1;; esac
    printf 'true|%s\n' "$mock_enabled" > "$EX_STATE/mock_package"
}
ex_cmd() {
    printf 'cmd|%s\n' "$*" >> "$EX_STATE/mock_calls"
    [ ! -f "$EX_STATE/mock_fail" ] || return 1
    printf 'true|3\n' > "$EX_STATE/mock_package"
}
ex_dump() {
    mock_values=$(cat "$EX_STATE/mock_package")
    printf '  User 0: installed=%s enabled=%s\n' "${mock_values%|*}" "${mock_values#*|}"
}
'''

class RescueTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name) / 'module'
        shutil.copytree(MODULE, self.root)
        self.state = self.root / 'state'
        self.state.mkdir(exist_ok=True)
        for name, value in {'ready':'1','mock_fp':'TEST_ROM','mock_boot':'boot-A','mock_completed':'0','mock_package':'false|3'}.items():
            (self.state / name).write_text(value + '\n')

    def tearDown(self):
        self.temp.cleanup()

    def run_shell(self, command, data=''):
        script = f'MODDIR="{self.root}"\n. "$MODDIR/lib.sh"\n' + MOCKS + '\n' + command
        return subprocess.run(['/bin/sh', '-c', script], input=data, text=True, capture_output=True, timeout=10)

    def arm(self, rows='com.miui.analytics|0\n', fingerprint=FP):
        return self.run_shell('ex_main arm', f'BE_RESCUE_V1\n{TX}\n{fingerprint}\n0\n{rows}')

    def test_arm_refuses_overwrite(self):
        self.assertEqual(self.arm().returncode, 0)
        before = (self.state / 'pending').read_text()
        self.assertNotEqual(self.arm('com.miui.hybrid|1\n').returncode, 0)
        self.assertEqual((self.state / 'pending').read_text(), before)

    def test_invalid_rows_and_injection_rejected(self):
        for rows in ['com.miui.x;reboot|0\n', 'com.miui.analytics|9\n', 'com.miui.analytics|0\ncom.miui.analytics|1\n', 'com.miui.powerkeeper|0\n']:
            self.assertNotEqual(self.arm(rows).returncode, 0, rows)
            self.assertFalse((self.state / 'pending').exists())

    def test_fingerprint_mismatch_refused(self):
        self.assertNotEqual(self.arm(fingerprint='0' * 64).returncode, 0)
        self.assertFalse((self.state / 'pending').exists())

    def test_rollback_restores_original_numeric_state(self):
        self.assertEqual(self.arm().returncode, 0)
        result = self.run_shell('ex_main rollback')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual((self.state / 'mock_package').read_text().strip(), 'true|0')
        self.assertFalse((self.state / 'pending').exists())
        self.assertIn('restored|', (self.state / 'last_result').read_text())

    def test_partial_failure_preserves_transaction(self):
        self.assertEqual(self.arm().returncode, 0)
        (self.state / 'mock_fail').touch()
        self.assertNotEqual(self.run_shell('ex_main rollback').returncode, 0)
        self.assertTrue((self.state / 'pending').exists())
        self.assertIn('incomplete|', (self.state / 'last_result').read_text())

    def test_ota_blocks_rollback(self):
        self.assertEqual(self.arm().returncode, 0)
        (self.state / 'mock_fp').write_text('OTHER_ROM\n')
        self.assertNotEqual(self.run_shell('ex_main rollback').returncode, 0)
        self.assertTrue((self.state / 'pending').exists())
        self.assertFalse((self.state / 'mock_calls').exists())

    def test_ack_requires_subsequent_completed_boot(self):
        self.assertEqual(self.arm().returncode, 0)
        self.assertNotEqual(self.run_shell('ex_main acknowledge').returncode, 0)
        (self.state / 'mock_boot').write_text('boot-B\n')
        self.assertNotEqual(self.run_shell('ex_main acknowledge').returncode, 0)
        (self.state / 'boot_ok').write_text('boot-B\n')
        self.assertEqual(self.run_shell('ex_main acknowledge').returncode, 0)
        self.assertFalse((self.state / 'pending').exists())

    def test_successful_boot_does_not_automatically_keep(self):
        self.assertEqual(self.arm().returncode, 0)
        (self.state / 'mock_boot').write_text('boot-B\n')
        (self.state / 'mock_completed').write_text('1\n')
        self.assertEqual(self.run_shell('ex_monitor').returncode, 0)
        self.assertTrue((self.state / 'pending').exists())
        self.assertEqual((self.state / 'attempts').read_text().strip(), '0')
        self.assertEqual((self.state / 'boot_ok').read_text().strip(), 'boot-B')

    def test_two_incomplete_boots_restore_when_pm_ready(self):
        self.assertEqual(self.arm().returncode, 0)
        (self.state / 'attempts').write_text('1\n')
        (self.state / 'mock_boot').write_text('boot-C\n')
        self.assertEqual(self.run_shell('ex_monitor').returncode, 0)
        self.assertFalse((self.state / 'pending').exists())
        self.assertTrue((self.state / 'last_restored').exists())

    def test_watchdog_preserves_when_pm_unavailable(self):
        self.assertEqual(self.arm().returncode, 0)
        (self.state / 'mock_boot').write_text('boot-B\n')
        (self.state / 'mock_unavailable').touch()
        self.assertNotEqual(self.run_shell('ex_monitor').returncode, 0)
        self.assertTrue((self.state / 'pending').exists())

    def test_stale_monitor_does_not_revert_new_transaction(self):
        self.assertEqual(self.arm().returncode, 0)
        (self.state / 'mock_boot').write_text('boot-B\n')
        new_tx = 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee'
        transaction = f'BE_RESCUE_V1\n{new_tx}\n{FP}\n0\ncom.miui.analytics|1\n'
        (self.state / 'replacement').write_text(transaction)
        command = r'''
        sleep() {
            if [ ! -f "$EX_STATE/replaced" ]; then
                ex_main rollback || return 1
                ex_main arm < "$EX_STATE/replacement" || return 1
                touch "$EX_STATE/replaced"
            fi
        }
        ex_monitor
        '''
        result = self.run_shell(command)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn(new_tx, (self.state / 'pending').read_text())

    def test_shell_parser_matches_all_fixture_cases(self):
        fixtures = MODULE.parent / 'tests/fixtures'
        for row in (fixtures / 'cases.tsv').read_text().splitlines():
            name, user, installed, enabled = row.split('\t')
            result = self.run_shell(f'ex_parse_state {int(user)}', (fixtures / name).read_text())
            if installed == 'error':
                self.assertNotEqual(result.returncode, 0, name)
            else:
                self.assertEqual(result.returncode, 0, name)
                self.assertEqual(result.stdout.strip(), installed + '|' + enabled, name)

if __name__ == '__main__':
    unittest.main(verbosity=2)
