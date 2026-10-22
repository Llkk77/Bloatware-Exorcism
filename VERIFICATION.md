# Physical-device testing update — 2026-10-22

Llkk77 reports that all tests performed on a **POCO M6 PRO** completed successfully.

**Atualização:** Llkk77 realizou testes em aparelho físico **POCO M6 PRO** e informou que todos os testes executados foram concluídos com êxito.

This report applies to the tested device and configuration. Android/ROM version, KernelSU version, app build and individual test logs were not specified in the report. It does not establish compatibility with other devices or firmware configurations.

The records below describe the earlier build-time verification environment. Statements that no device or emulator was connected refer to those historical sessions, before the subsequent physical-device testing reported above.

---

# Verification 0.3.0

- assembleDebug and lintDebug passed with no lint issues.
- PolicyTest: 26 checks passed. FixtureTest: 11 synthetic fixtures passed.
- UI bridge tests passed after replacing controls with Material Web: selected-package one-tap removal/disable, root gate, advanced review/cancellation, escaping, backups, native audit, validation checklist/select, report and rescue status.
- axe-core 4.14.0 found no violations for the tested WCAG 2 A/AA and 2.1 AA rules on the five browser-preview screens. This is an automated subset, not a complete WCAG audit or certification.
- Responsive tests passed at 320, 360, 412, 600, 1000 and 1440 px. No horizontal overflow; selected-package panel avoids bottom/side navigation. Search-screen text was also checked at 200%.
- Three seed palettes changed the rendered theme. Primary, primary-container, secondary-container, tertiary-container and surface foreground pairs met 4.5:1 contrast in these palette tests.
- Keyboard dialog containment, Escape dismissal and focus restoration passed with reduced motion enabled. Authored and component shadow-root CSS honors reduced-motion preferences; dialog travel animations are disabled in that mode.
- Browser test recorded no HTTP requests. Material Web, Material Color Utilities and Roboto are packaged locally. CSP retains blocked connections and no Android INTERNET permission was added.
- Mobile/desktop visual inspection completed with explicitly illustrative data; captures were used internally only.
- APK version 0.3.0, versionCode 5. Signature matches 0.2.2 using the preserved development key. APK and project ZIP contents verified before delivery.

No physical Android device or emulator was connected. Actual Android dynamic resources, system contrast settings, WebView rendering, font scale and HyperOS navigation-bar behavior still require on-device verification. Browser tests do not validate root commands, SELinux, firmware safety, battery gains or rescue timing. No catalog build was promoted to validated.

Catalog bytes and package mutation policies are unchanged. New native code reads theme resources and refreshes WebView colors/text scale; snapshot theme metadata now identifies material-you-dark. Native root entry points are preserved.

See docs/MATERIAL3.md for tokens, dependencies, licenses and primary design references.

Previous verification records follow.

---

# Verification 0.2.2

- assembleDebug and lintDebug passed; no lint issues.
- PolicyTest: 26 checks passed. FixtureTest: 11 synthetic fixtures passed.
- UI bridge tests passed: ordinary removal and disabling each execute with one tap after manual selection; selection alone never mutates packages; root access is required; advanced packages require review and cancellation does not mutate; HTML escaping, backups, audit, checklist, report export and rescue status work with controlled responses.
- All five screens checked at 320, 360, 393, 412 and 860 px without horizontal overflow. The selected-package action panel stays above the app navigation.
- Mobile visual inspection covered the updated home screen and selected-package panel. Screenshots used illustrative browser data only.
- Native layout now uses a FrameLayout outside the WebView to apply system/cutout/IME insets and consumes the applied insets. Navigation bars are hidden using WindowInsetsController on API 30+ and sticky immersive flags on API 29; focus regain reapplies this behavior. Status bar remains visible. A system swipe can temporarily reveal navigation controls.
- App version 0.2.2, versionCode 4. APK signature and project ZIP integrity checked before delivery.
- Catalog and native mutation/backup policies are unchanged; only snapshot reason text was corrected to English.

No physical Android device or emulator was connected. The exact HyperOS system-bar behavior shown in the user screenshot still requires verification on the device. Browser layout tests do not test Android system bars, KernelSU, PackageManager or boot recovery.

This build has a different development certificate from 0.2.1. Export backups/reports before uninstalling that version. Its original private signing key was not present among the recoverable project files. The new development key is preserved separately for future builds; never use it as a production release key.

Android implementation references:
- https://developer.android.com/develop/ui/views/layout/webapps/understand-window-insets
- https://developer.android.com/develop/ui/views/layout/immersive

Previous verification records follow.

---

# Verification 0.2.1

- assembleDebug and lintDebug passed. Lint reported no issues.
- PolicyTest: 26 checks passed after native message translation.
- FixtureTest: all 11 synthetic dumpsys fixtures passed.
- UI bridge smoke tests passed with English selectors: confirmation and cancellation, escaping, backups, advanced selection, native audit, validation checklist, report export and rescue status.
- Visual inspection covered the home screen, Deep Exorcism and About dialog in a mobile viewport. All five pages have a black background and no horizontal overflow at 320, 360, 412 and 860 px.
- All 139 catalog entries retain their package identifiers, risk levels and sources. Catalog descriptions, group names and labels are English. Known packages display their catalog label; unknown packages keep their system-provided label. Raw Android output and imported records preserve their original text.
- The native window, status/navigation icon appearance, local WebView, controls, dialogs and launcher icon use the dark blue theme.
- APK version: 0.2.1, versionCode 3. APK signature and ZIP integrity verified before delivery.

This build uses a different development certificate from 0.2.0. Android cannot install it over that build. Export Holy Backup and any reports before uninstalling the previous version; uninstalling deletes internal records. This is a test APK, not a release-signed distribution.

No physical device or emulator was connected. Root operations and bootloop rescue remain unvalidated on hardware. The color and language changes do not establish device safety or performance gains. No catalog build was marked as validated.

The previous verification record below documents 0.2.0 testing. Rescue scripts and fixture import logic were not changed in 0.2.1.

---

# Verificação 0.2.0

- Compilação assembleDebug e lintDebug: passaram, sem problemas reportados pelo lint. As duas supressões específicas da WebView local e QUERY_ALL_PACKAGES seguem justificadas no README.
- PolicyTest: 26 verificações passaram.
- FixtureTest: 11 fixtures SINTÉTICAS passaram; nenhuma é rotulada como captura real.
- Resgate: 12 testes com chamadas Android substituídas por funções locais passaram. Incluem fingerprint, transações inválidas, falha parcial, dois boots incompletos, confirmação após boot e monitor antigo diante de nova transação. O parser shell foi conferido contra as mesmas 11 fixtures.
- Importador de fixtures: 4 testes de contrato, hash, observação estável e estado independente passaram.
- Testes de interface e bridge controlado passaram, incluindo checklist, exportação de relatório e consulta de resgate. Não executam root real.
- Assinatura do APK e integridade dos ZIPs verificadas ao preparar a entrega.

Não há aparelho físico nem emulador conectado. KernelSU, SELinux, tempos de boot, formatos reais de dumpsys e os efeitos por firmware permanecem sem validação física. O módulo é EXPERIMENTAL e não garante recuperação de bootloop. Nenhum módulo foi instalado, nenhum pacote real foi alterado e nenhuma build real foi promovida a validada.

O relatório exporta observações e propõe validatedBuilds apenas com critérios explícitos; o catálogo embarcado permanece sem validações de aparelho. O coletor de fixtures está pronto para receber capturas reais Global/China.
