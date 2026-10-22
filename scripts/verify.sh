#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test_classes=$(mktemp -d)
trap 'rm -rf "$test_classes"' EXIT
javac -d "$test_classes" app/src/main/java/dev/bloatware/exorcism/Policy.java tests/PolicyTest.java tests/FixtureTest.java
java -cp "$test_classes" dev.bloatware.exorcism.PolicyTest
java -cp "$test_classes" dev.bloatware.exorcism.FixtureTest tests/fixtures
if [ "$(id -u)" -eq 0 ]; then
    python3 -m unittest discover -s tests -p 'test_*.py' -v
else
    sudo python3 -m unittest discover -s tests -p 'test_*.py' -v
fi
