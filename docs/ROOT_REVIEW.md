# Root-path review notes

This is a limited source inspection performed with ChatGPT assistance on 2026-10-22. It is not an independent audit, penetration test or device-safety certification. Application behavior was not changed during source publication.

| Area | Observation | Further review |
|---|---|---|
| Shell execution | `Shell.run` launches `su -c`, limits captured output and waits 30 seconds | Verify timeout and child-process behavior on the target KernelSU/ROM |
| Command construction | `Policy.validPackage` excludes quotes, shell operators, whitespace and substitutions; enabled-state commands use a fixed switch | Trace every command caller and every value from imports/bridge requests |
| Mutation authorization | `Engine.allowed` combines catalog classification, static/dynamic protections, system flags and UID checks | Test default launcher/IME and firmware-specific dependencies |
| Restoration | `Engine.restore` verifies user/fingerprint, history order and package name/catalog/static protections | Its checks differ from mutation authorization; independently review dynamic-role and imported-state behavior |
| State parsing | The parser rejects missing/duplicate user blocks, ambiguous fields and unknown enabled values | Collect sanitized real `dumpsys` fixtures for tested ROMs |
| Rescue | Transactions have bounded fields/rows, allowlist, fingerprint, locks and pending-state handling | Test normal boot/manual rollback first; do not deliberately induce a bootloop without an external recovery plan |
| WebView bridge | Bundled local UI; network and navigation blocked; arbitrary JS must not reach the root bridge | Review escaping/imported content and bridge operation routing |

## Checks run during this work

The existing Python suites completed successfully: 16 tests covering fixture import and simulated rescue behavior. They replace Android commands with local functions and do not exercise real root access. Java policy/fixture and Android build checks are configured in CI; their results must be read from the workflow run, not inferred from this document.

The native Java, manifest, UI assets and rescue scripts were imported byte-for-byte from the existing project archive. Release signing is performed outside application source, using secrets that must be supplied by the maintainer. No signing key is committed.
