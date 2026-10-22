import argparse
import hashlib
import json
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument('capture', type=Path)
parser.add_argument('--destination', type=Path, default=Path('tests/fixtures/device'))
args = parser.parse_args()
payload = json.loads(args.capture.read_text())
if payload.get('schema') != 'bloatware-exorcism.fixture/1' or payload.get('provenance') != 'device_capture_sanitized':
    raise SystemExit('Capture provenance not supported')
if payload.get('stableObservation') is not True:
    raise SystemExit('Observation was not stable; capture again')
text = payload['dumpsys']
if hashlib.sha256(text.encode()).hexdigest() != payload['dumpSha256']:
    raise SystemExit('Capture hash mismatch')
expected = payload['expected']
if expected.get('source') != 'Android_PackageManager_API' or type(expected['installed']) is not bool or type(expected['enabled']) is not int or expected['enabled'] not in range(5):
    raise SystemExit('Independent state is invalid')
user = payload['user']
if type(user) is not int or user < 0:
    raise SystemExit('Invalid user')
args.destination.mkdir(parents=True, exist_ok=True)
name = payload['dumpSha256'][:24]
(args.destination / (name + '.txt')).write_text(text)
(args.destination / (name + '.json')).write_text(json.dumps(payload, ensure_ascii=False, indent=2))
case = f"{name}.txt\t{user}\t{str(expected['installed']).lower()}\t{expected['enabled']}\n"
cases = args.destination / 'cases.tsv'
if not cases.exists() or case not in cases.read_text():
    with cases.open('a') as out:
        out.write(case)
print(f'Imported {name}. Preserve provenance and investigate parser discrepancies.')
