package dev.bloatware.exorcism;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    WebView web;
    FrameLayout viewport;
    Engine engine;
    final ExecutorService worker = Executors.newSingleThreadExecutor();
    String pendingId;
    String pendingExport;
    String startupError;

    @android.annotation.SuppressLint("SetJavaScriptEnabled")
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Theme.surface(this));
        getWindow().setNavigationBarColor(Theme.surface(this));
        if (android.os.Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        web = new WebView(this);
        web.setBackgroundColor(Theme.surface(this));
        viewport = new FrameLayout(this);
        viewport.setBackgroundColor(Theme.surface(this));
        viewport.addView(web, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(viewport);
        viewport.setOnApplyWindowInsetsListener((v, insets) -> {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return WindowInsets.CONSUMED;
            } else v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets().consumeDisplayCutout();
        });
        viewport.post(() -> { hideNavigationBar(); viewport.requestApplyInsets(); });
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setTextZoom(Math.round(100 * getResources().getConfiguration().fontScale));
        s.setDomStorageEnabled(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setBlockNetworkLoads(true);
        s.setSupportMultipleWindows(false);
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return true; }
        });
        web.addJavascriptInterface(new Bridge(), "Exorcism");
        worker.execute(() -> {
            try { engine = new Engine(getApplicationContext()); }
            catch (Exception e) { startupError = Engine.message(e); }
            runOnUiThread(() -> { if (!isFinishing()) web.loadUrl("file:///android_asset/index.html"); });
        });
    }

    void hideNavigationBar() {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.hide(WindowInsets.Type.navigationBars());
            }
        } else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && viewport != null) {
            hideNavigationBar();
            viewport.requestApplyInsets();
            refreshTheme();
        }
    }

    void refreshTheme() {
        if (web == null) return;
        web.getSettings().setTextZoom(Math.round(100 * getResources().getConfiguration().fontScale));
        viewport.setBackgroundColor(Theme.surface(this));
        getWindow().setStatusBarColor(Theme.surface(this));
        getWindow().setNavigationBarColor(Theme.surface(this));
        try { web.evaluateJavascript("window.applyDeviceTheme && window.applyDeviceTheme(" + Theme.describe(this).toString() + ")", null); }
        catch (Exception ignored) { }
    }

    @Override public void onConfigurationChanged(android.content.res.Configuration configuration) {
        super.onConfigurationChanged(configuration);
        refreshTheme();
    }

    void reply(String id, Object data, String error) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            try {
                JSONObject envelope = new JSONObject().put("id", id).put("ok", error == null)
                    .put("data", data == null ? JSONObject.NULL : data).put("error", error == null ? JSONObject.NULL : error);
                web.evaluateJavascript("window.receive(" + envelope.toString() + ")", null);
            } catch (Exception ignored) { }
        });
    }

    final class Bridge {
        @JavascriptInterface public void request(String op, String payload, String id) {
            if (id == null || !id.matches("[0-9]{1,12}")) return;
            worker.execute(() -> {
                try {
                    if (startupError != null) throw new IllegalStateException("Could not open history: " + startupError);
                    JSONObject args = new JSONObject(payload);
                    Object result;
                    switch (op) {
                        case "scan": result = engine.scan(); break;
                        case "recordValidation": result = engine.recordValidation(args); break;
                        case "rescueStatus": engine.requireRoot(); result = engine.rescue.status(); break;
                        case "rescueKeep": engine.requireRoot(); result = engine.rescue.acknowledge(); break;
                        case "rescueRollback": engine.requireRoot(); result = engine.rescue.rollback(); break;
                        case "exportReport": {
                            String text = engine.validationReport().toString(2);
                            runOnUiThread(() -> pickDocument(id, text, true, "exorcism-validation-")); return;
                        }
                        case "exportFixture": {
                            String text = engine.captureFixture(args.getString("package")).toString(2);
                            runOnUiThread(() -> pickDocument(id, text, true, "exorcism-fixture-")); return;
                        }
                        case "connect": result = engine.connect(); break;
                        case "apply": result = engine.apply(args); break;
                        case "restore": result = engine.restore(args.getString("id")); break;
                        case "backup": result = engine.createSnapshot("Manual Holy Backup"); break;
                        case "inspect": result = engine.inspect(args.getString("package")); break;
                        case "native": result = engine.nativeServices(); break;
                        case "compareBackup": result = engine.compareSnapshot(args.getString("id")); break;
                        case "restoreBackup": result = engine.restoreSnapshot(args.getString("id")); break;
                        case "export": {
                            String text = engine.snapshot(args.getString("id")).toString(2);
                            runOnUiThread(() -> pickDocument(id, text, true));
                            return;
                        }
                        case "import": runOnUiThread(() -> pickDocument(id, null, false)); return;
                        default: throw new IllegalArgumentException("Operation unavailable.");
                    }
                    reply(id, result, null);
                } catch (Exception e) { reply(id, null, Engine.message(e)); }
            });
        }
    }

    void pickDocument(String id, String text, boolean export) { pickDocument(id, text, export, "holy-backup-"); }

    void pickDocument(String id, String text, boolean export, String prefix) {
        if (pendingId != null) { reply(id, null, "A file picker is already open."); return; }
        pendingId = id;
        pendingExport = text;
        Intent intent = new Intent(export ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(export ? "application/json" : "*/*");
        if (export) intent.putExtra(Intent.EXTRA_TITLE, prefix + System.currentTimeMillis() + ".json");
        try { startActivityForResult(intent, export ? 71 : 72); }
        catch (Exception e) { pendingId = null; pendingExport = null; reply(id, null, "No file picker available."); }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != 71 && request != 72) return;
        String id = pendingId;
        String text = pendingExport;
        pendingId = null; pendingExport = null;
        if (id == null) return;
        if (result != RESULT_OK || data == null || data.getData() == null) { reply(id, null, "Selection canceled."); return; }
        Uri uri = data.getData();
        worker.execute(() -> {
            try {
                if (request == 71) {
                    try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
                        if (out == null) throw new IllegalStateException("Could not open the file.");
                        out.write(text.getBytes(StandardCharsets.UTF_8));
                    }
                    reply(id, new JSONObject().put("message", "JSON file exported."), null);
                } else {
                    try (InputStream in = getContentResolver().openInputStream(uri)) {
                        if (in == null) throw new IllegalStateException("File unavailable.");
                        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                        byte[] bytes = new byte[4096]; int n;
                        while ((n = in.read(bytes)) != -1) {
                            if (buffer.size() + n > 4194304) throw new IllegalArgumentException("Backup exceeds 4 MB.");
                            buffer.write(bytes, 0, n);
                        }
                        reply(id, engine.importSnapshot(buffer.toString(StandardCharsets.UTF_8.name())), null);
                    }
                }
            } catch (Exception e) { reply(id, null, Engine.message(e)); }
        });
    }

    @Override public void onBackPressed() {
        web.evaluateJavascript("window.backAction && window.backAction()", value -> { if (!"true".equals(value)) finish(); });
    }

    @Override protected void onDestroy() {
        worker.shutdown();
        if (web != null) { web.removeJavascriptInterface("Exorcism"); web.destroy(); }
        super.onDestroy();
    }
}
