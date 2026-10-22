import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / 'scripts/import_fixture.py'

class FixtureImportTest(unittest.TestCase):
    def run_case(self, mutate=None):
        text = '  User 0: installed=true enabled=0\n'
        payload = {'schema':'bloatware-exorcism.fixture/1','provenance':'device_capture_sanitized','stableObservation':True,'dumpsys':text,'dumpSha256':hashlib.sha256(text.encode()).hexdigest(),'expected':{'source':'Android_PackageManager_API','installed':True,'enabled':0},'user':0}
        if mutate:
            mutate(payload)
        with tempfile.TemporaryDirectory() as folder:
            source = Path(folder)/'capture.json'
            destination = Path(folder)/'fixtures'
            source.write_text(json.dumps(payload))
            result = subprocess.run([sys.executable,str(SCRIPT),str(source),'--destination',str(destination)],capture_output=True,text=True)
            return result.returncode, (destination/'cases.tsv').exists()

    def test_supported_contract(self):
        self.assertEqual(self.run_case(), (0, True))

    def test_hash_mismatch(self):
        self.assertNotEqual(self.run_case(lambda p:p.update(dumpSha256='0'*64))[0],0)

    def test_unstable_capture(self):
        self.assertNotEqual(self.run_case(lambda p:p.update(stableObservation=False))[0],0)

    def test_missing_independent_state(self):
        self.assertNotEqual(self.run_case(lambda p:p['expected'].update(source='parser'))[0],0)

if __name__ == '__main__':
    unittest.main(verbosity=2)
