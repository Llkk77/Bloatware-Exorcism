# Contributing

Credits: **Llkk77** and **ChatGPT**. ChatGPT assisted with implementation and documentation. Llkk77 reports the physical-device tests; broader human code review and Android Studio testing are in progress. No independent review is claimed.

Use small, descriptive commits. Keep behavior changes separate from formatting and generated dependency bundles. For root-path changes, explain command inputs, protected-package behavior, rollback and failure handling. Never include signing keys, unredacted device exports or personal data.

Run `python3 -m unittest discover -s tests -p 'test_*.py'`, the Java tests and Android lint/build described in [build instructions](docs/BUILD_AND_RELEASE.md). Synthetic tests do not replace physical-device validation. Use [the device checklist](docs/DEVICE_TESTING.md) when reporting results, including failures.

Report ordinary bugs through Issues with app/build/device versions, expected behavior, reproduction and redacted evidence. Use Discussions for questions and ideas. Report security concerns using [SECURITY.md](SECURITY.md).
