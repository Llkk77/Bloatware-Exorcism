import '@material/web/button/filled-button.js';
import '@material/web/button/filled-tonal-button.js';
import '@material/web/button/outlined-button.js';
import '@material/web/button/text-button.js';
import '@material/web/iconbutton/icon-button.js';
import '@material/web/checkbox/checkbox.js';
import '@material/web/textfield/outlined-text-field.js';
import '@material/web/chips/chip-set.js';
import '@material/web/chips/filter-chip.js';
import '@material/web/dialog/dialog.js';
import '@material/web/select/outlined-select.js';
import '@material/web/select/select-option.js';
import '@material/web/progress/circular-progress.js';
import '@material/web/ripple/ripple.js';
import {Hct, SchemeTonalSpot, MaterialDynamicColors, argbFromHex, hexFromArgb} from '@material/material-color-utilities';

window.applyDeviceTheme = (theme = {}) => {
  const seed = /^#[0-9a-f]{6}$/i.test(theme.seed || '') ? theme.seed : '#4563eb';
  const scheme = new SchemeTonalSpot(Hct.fromInt(argbFromHex(seed)), true, Math.max(0, Math.min(1, Number(theme.contrast) || 0)));
  const colors = {};
  for (const role of ['primary','onPrimary','primaryContainer','onPrimaryContainer','secondary','onSecondary','secondaryContainer','onSecondaryContainer','tertiary','onTertiary','tertiaryContainer','onTertiaryContainer','error','onError','errorContainer','onErrorContainer','surface','onSurface','surfaceVariant','onSurfaceVariant','surfaceDim','surfaceBright','surfaceContainerLowest','surfaceContainerLow','surfaceContainer','surfaceContainerHigh','surfaceContainerHighest','outline','outlineVariant','inverseSurface','inverseOnSurface','inversePrimary','scrim']) {
    const token = role.replace(/[A-Z]/g, letter => '-' + letter.toLowerCase());
    const supplied = theme.colors?.[token];
    colors[token] = /^#[0-9a-f]{6}$/i.test(supplied || '') ? supplied : hexFromArgb(MaterialDynamicColors[role].getArgb(scheme));
    document.documentElement.style.setProperty('--md-sys-color-' + token, colors[token]);
  }
  document.documentElement.dataset.colorSource = theme.dynamic ? 'device' : 'exorcism-blue';
  window.materialColors = colors;
};
window.applyDeviceTheme();

const motionStyle = new CSSStyleSheet();
motionStyle.replaceSync('@media(prefers-reduced-motion:reduce){*,*::before,*::after{animation-duration:.01ms!important;transition-duration:.01ms!important;scroll-behavior:auto!important}:host{--md-ripple-pressed-opacity:0}}');
window.enhanceMaterial = async (root = document) => {
  for (const element of root.querySelectorAll('*')) {
    if (!element.localName.startsWith('md-')) continue;
    if (element.updateComplete) await element.updateComplete;
    const shadow = element.shadowRoot;
    if (shadow && !shadow.adoptedStyleSheets.includes(motionStyle)) {
      shadow.adoptedStyleSheets = [...shadow.adoptedStyleSheets, motionStyle];
      await window.enhanceMaterial(shadow);
    }
  }
};
