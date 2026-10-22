# Material 3 interface

Version 0.3.0 uses the official Material Web components, bundled locally. The APK remains offline and does not request INTERNET. Its Android shell and root operations remain Java; this change does not migrate the project to Compose.

## Components and tokens

| Element | Implementation |
|---|---|
| Filled, tonal, outlined and text buttons | Material Web buttons with 40 px visual containers and wrapper touch targets |
| Icon buttons | Material Web icon buttons |
| Package and advanced selection | Material Web checkboxes, 18 px visuals and 48 px touch targets |
| Search | Material Web outlined text field with floating label |
| Filters | Material Web filter chips and chip set |
| Checklist choices | Material Web outlined select and select options |
| Dialogs | Material Web dialog with native modal behavior, focus containment, Escape and focus restoration |
| Loading | Material Web indeterminate circular progress |
| Interactive cards and navigation | Semantic buttons with Material Web ripple; application CSS implements M3 surface, shape, state and navigation tokens |
| Typography | Bundled variable Roboto, OFL license included; Material type roles and rem-based text |

The M3 roles are exposed through `--md-sys-color-*`. Surfaces use the lowest, low, container, high and highest roles rather than decorative gradients. Primary, secondary and tertiary containers always use their corresponding foreground roles.

Android 14+ supplies public system M3 dark color resources directly. Android 12–13 supplies the system accent seed, from which Material Color Utilities creates a dark tonal-spot scheme. Earlier Android versions and browser previews use an Exorcism blue seed. Theme refreshes on window focus and configuration changes; native window backgrounds also follow the surface role. Device palette and system contrast handling still require verification on physical ROMs.

## Adaptive layout and interaction

- Compact: bottom navigation with an 80 px baseline, active tonal indicator, icons and labels.
- At 600 px: navigation rail; at 1440 px: expanded side navigation.
- At 1000 px: two-column dashboard; package lists remain readable across widths.
- The selected-package panel avoids the bottom navigation or side rail. Text can scale without horizontal overflow in the tested search layout.
- Page entry, state changes and selection-panel transitions use `cubic-bezier(0.2, 0, 0, 1)`. Material components provide their own interaction feedback.
- Reduced-motion preferences disable authored motion, shorten shadow-root CSS transitions and remove dialog travel animations. Keyboard focus remains visible.
- Busy operations mark the main content busy and inert; root actions retain their original protections. Ordinary selected-package actions still execute in one tap, while advanced actions retain review.

## Build and checks

The compiled bundle is committed as an asset. Android builds do not require Node or a CDN. To rebuild the UI dependency bundle after changing its entry point:

```sh
npm ci
npm run build:ui
```

Pinned runtime dependencies: Material Web 2.5.0 and Material Color Utilities 0.4.0. `package-lock.json` records transitive dependencies; licenses are in `third-party/` and the bundled legal-notices asset.

`tests/ui.cjs` covers the native bridge with controlled responses. `tests/material.cjs` covers WCAG A/AA rules through axe, six responsive widths, action-panel placement, search, 200% text in the search screen, three seed palettes and contrast ratios, modal keyboard behavior and absence of HTTP requests. Automated checks do not establish full WCAG conformance or physical Android compatibility.

## Primary references

- https://m3.material.io/styles/color/the-color-system
- https://m3.material.io/styles/typography/applying-type
- https://m3.material.io/components/navigation-bar/specs
- https://github.com/material-components/material-web
- https://github.com/material-components/material-components-android/blob/master/docs/theming/Color.md
- https://github.com/material-foundation/material-color-utilities
- https://github.com/google/fonts/tree/main/ofl/roboto
