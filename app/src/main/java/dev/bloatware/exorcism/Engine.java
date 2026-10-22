package dev.bloatware.exorcism;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.AtomicFile;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class Engine {
    final Context context;
    final PackageManager pm;
    final int user = android.os.Process.myUid() / 100000;
    final Map<String, JSONObject> catalog = new HashMap<>();
    final AtomicFile storage;
    JSONObject store;
    boolean root;
    final Rescue rescue = new Rescue();

    Engine(Context context) throws Exception {
        this.context = context;
        pm = context.getPackageManager();
        storage = new AtomicFile(new File(context.getFilesDir(), "state.json"));
        JSONArray packages = new JSONObject(new String(readBytes(context.getAssets().open("catalog.json")), StandardCharsets.UTF_8)).getJSONArray("packages");
        for (int i = 0; i < packages.length(); i++) catalog.put(packages.getJSONObject(i).getString("package"), packages.getJSONObject(i));
        if (storage.getBaseFile().exists() || new File(storage.getBaseFile() + ".bak").exists()) {
            try (java.io.InputStream in = storage.openRead()) { store = new JSONObject(new String(readBytes(in), StandardCharsets.UTF_8)); }
        } else store = new JSONObject().put("history", new JSONArray()).put("snapshots", new JSONArray());
        if (!store.has("validations")) store.put("validations", new JSONArray());
    }

    void save() throws Exception {
        FileOutputStream out = null;
        try {
            out = storage.startWrite();
            out.write(store.toString().getBytes(StandardCharsets.UTF_8));
            storage.finishWrite(out);
        } catch (Exception e) {
            if (out != null) storage.failWrite(out);
            throw e;
        }
    }

    JSONObject device() throws Exception {
        JSONObject d = new JSONObject().put("model", Build.MANUFACTURER + " " + Build.MODEL)
            .put("android", Build.VERSION.RELEASE).put("api", Build.VERSION.SDK_INT).put("fingerprint", Build.FINGERPRINT)
            .put("build", Build.DISPLAY).put("rom", Build.VERSION.INCREMENTAL).put("romRegion", Build.VERSION.INCREMENTAL.contains("CNXM") ? "China" : Build.VERSION.INCREMENTAL.contains("MIXM") ? "Global" : "unknown").put("deviceCode", Build.DEVICE).put("user", user).put("root", root).put("catalogCount", catalog.size());
        return d.put("theme", Theme.describe(context));
    }

    Set<String> dynamicProtected() {
        Set<String> names = new HashSet<>();
        try {
            android.content.pm.ResolveInfo home = pm.resolveActivity(new android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY);
            if (home != null && home.activityInfo != null) names.add(home.activityInfo.packageName);
            String dialer = context.getSystemService(android.telecom.TelecomManager.class).getDefaultDialerPackage();
            if (dialer != null) names.add(dialer);
            String sms = android.provider.Telephony.Sms.getDefaultSmsPackage(context);
            if (sms != null) names.add(sms);
        } catch (Exception ignored) { }
        for (String key : new String[]{"default_input_method", "enabled_accessibility_services", "assistant", "voice_interaction_service", "always_on_vpn_app"}) {
            try {
                String value = Settings.Secure.getString(context.getContentResolver(), key);
                if (value != null) for (String component : value.split(":")) names.add(component.split("/")[0]);
            } catch (Exception ignored) { }
        }
        try {
            List<ComponentName> admins = context.getSystemService(DevicePolicyManager.class).getActiveAdmins();
            if (admins != null) for (ComponentName admin : admins) names.add(admin.getPackageName());
        } catch (Exception ignored) { }
        return names;
    }

    boolean system(ApplicationInfo info) {
        return (info.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }

    JSONObject scan() throws Exception {
        Set<String> protectedApps = dynamicProtected();
        JSONArray apps = new JSONArray();
        List<ApplicationInfo> infos = pm.getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS);
        for (ApplicationInfo info : infos) {
            if (!system(info)) continue;
            String name = info.packageName;
            JSONObject record = catalog.get(name);
            boolean locked = Policy.protectedName(name) || protectedApps.contains(name) || (info.flags & ApplicationInfo.FLAG_PERSISTENT) != 0 || info.uid % 100000 < 10000;
            String risk = locked ? "protected" : record == null ? "unknown" : record.getString("risk");
            int enabled;
            try { enabled = pm.getApplicationEnabledSetting(name); } catch (Exception e) { enabled = -1; }
            apps.put(new JSONObject().put("package", name).put("label", record == null ? info.loadLabel(pm).toString() : record.getString("label"))
                .put("group", record == null ? "Unclassified" : record.getString("group"))
                .put("risk", risk).put("enabled", info.enabled).put("enabledSetting", enabled)
                .put("note", locked ? "Protected by core dependencies, a default role, UID or persistent configuration."
                    : record == null ? "Not assessed in the catalog. Inspection only."
                    : record.getString("note"))
                .put("source", record == null ? "" : record.getString("source"))
                .put("validated", false));
        }
        return new JSONObject().put("validations", store.getJSONArray("validations")).put("device", device()).put("apps", apps)
            .put("history", store.getJSONArray("history")).put("snapshots", snapshotList());
    }

    JSONArray snapshotList() throws Exception {
        JSONArray result = new JSONArray();
        JSONArray snapshots = store.getJSONArray("snapshots");
        for (int i = snapshots.length() - 1; i >= 0; i--) {
            JSONObject s = snapshots.getJSONObject(i);
            result.put(new JSONObject().put("id", s.getString("id")).put("created", s.getLong("created"))
                .put("reason", s.getString("reason")).put("count", s.getJSONArray("packages").length())
                .put("compatible", s.getString("fingerprint").equals(Build.FINGERPRINT) && s.getInt("user") == user));
        }
        return result;
    }

    void requireRoot() throws Exception {
        Shell.Result id = Shell.run("id -u");
        root = id.code == 0 && id.output.equals("0");
        if (!root) throw new IllegalStateException("Authorize Bloatware Exorcism in KernelSU with UID 0 and try again.");
        Shell.Result current = Shell.run("am get-current-user");
        if (current.code != 0 || !current.output.equals(Integer.toString(user)))
            throw new IllegalStateException("The active user differs from this app. Open the app as the active user to continue.");
    }

    JSONObject connect() throws Exception { requireRoot(); return device(); }

    Policy.State state(String name) throws Exception {
        if (!Policy.validPackage(name)) throw new IllegalArgumentException("Invalid package name.");
        Shell.Result r = Shell.run("dumpsys package '" + name + "'");
        if (r.code != 0) throw new IllegalStateException("Could not query " + name);
        return Policy.parseState(r.output, user);
    }

    void allowed(String name, boolean advanced) throws Exception {
        if (!Policy.validPackage(name)) throw new IllegalArgumentException("Invalid package.");
        JSONObject c = catalog.get(name);
        ApplicationInfo info = pm.getApplicationInfo(name, PackageManager.MATCH_UNINSTALLED_PACKAGES);
        if (c == null || !system(info)) throw new IllegalStateException("Only classified system packages can be changed.");
        if (Policy.protectedName(name) || c.getString("risk").equals("protected") || dynamicProtected().contains(name)
            || (info.flags & ApplicationInfo.FLAG_PERSISTENT) != 0 || info.uid % 100000 < 10000)
            throw new IllegalStateException("Protected component: " + name);
        if (c.getString("risk").equals("advanced") && !advanced) throw new IllegalStateException("Use Deep Exorcism for this component.");
    }

    JSONObject createSnapshot(String reason) throws Exception {
        JSONObject snapshot = new JSONObject().put("schema", "bloatware-exorcism/1").put("id", UUID.randomUUID().toString())
            .put("created", System.currentTimeMillis()).put("reason", reason).put("fingerprint", Build.FINGERPRINT)
            .put("model", Build.MODEL).put("build", Build.DISPLAY).put("user", user).put("theme", "material-you-dark");
        JSONArray packages = new JSONArray();
        for (ApplicationInfo info : pm.getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS)) {
            if (!system(info)) continue;
            int enabled = pm.getApplicationEnabledSetting(info.packageName);
            packages.put(new JSONObject().put("package", info.packageName).put("installed", true).put("enabled", enabled));
        }
        Set<String> already = new HashSet<>();
        for (int i = 0; i < packages.length(); i++) already.add(packages.getJSONObject(i).getString("package"));
        JSONArray history = store.getJSONArray("history");
        for (int i = history.length() - 1; i >= 0; i--) {
            JSONObject entry = history.getJSONObject(i);
            String name = entry.getString("package");
            if (already.add(name)) {
                Policy.State s = root ? state(name) : new Policy.State(false, entry.getInt("beforeEnabled"));
                packages.put(new JSONObject().put("package", name).put("installed", s.installed).put("enabled", s.enabled));
            }
        }
        snapshot.put("packages", packages).put("history", new JSONArray(history.toString()));
        store.getJSONArray("snapshots").put(snapshot);
        save();
        return snapshot;
    }

    JSONObject snapshot(String id) throws Exception {
        JSONArray snapshots = store.getJSONArray("snapshots");
        for (int i = 0; i < snapshots.length(); i++) if (snapshots.getJSONObject(i).getString("id").equals(id)) return snapshots.getJSONObject(i);
        throw new IllegalArgumentException("Backup not found.");
    }

    JSONObject apply(JSONObject args) throws Exception {
        requireRoot();
        JSONObject rescueStatus = rescue.status();
        if (rescueStatus.getBoolean("available") && !rescueStatus.getString("details").contains("pending=none"))
            throw new IllegalStateException("A rescue batch is pending. Roll it back or reboot and acknowledge it before making more changes.");
        String action = args.getString("action");
        if (!action.equals("disable") && !action.equals("remove")) throw new IllegalArgumentException("Invalid action.");
        JSONArray input = args.getJSONArray("packages");
        if (input.length() < 1 || input.length() > 30) throw new IllegalArgumentException("Select 1 to 30 packages.");
        Set<String> names = new LinkedHashSet<>();
        for (int i = 0; i < input.length(); i++) { String name = input.getString(i); allowed(name, args.optBoolean("advanced")); names.add(name); }
        JSONObject snap = createSnapshot(action.equals("remove") ? "Before removing packages" : "Before disabling packages");
        String rescueId = "";
        if (args.optBoolean("rescue")) {
            JSONArray original = new JSONArray();
            for (String name : names) {
                Policy.State before = state(name);
                original.put(new JSONObject().put("package", name).put("installed", before.installed).put("enabled", before.enabled));
            }
            rescueId = rescue.arm(user, Build.FINGERPRINT, original);
        }
        JSONArray results = new JSONArray();
        boolean uncertain = false;
        for (String name : names) {
            try {
                requireRoot();
                allowed(name, args.optBoolean("advanced"));
                Policy.State before = state(name);
                if (!before.installed) throw new IllegalStateException("The package is already removed for this user.");
                if (action.equals("disable") && (before.enabled == 2 || before.enabled == 3 || before.enabled == 4))
                    throw new IllegalStateException("The package is already disabled; original state preserved.");
                JSONObject e = new JSONObject().put("id", UUID.randomUUID().toString()).put("created", System.currentTimeMillis())
                    .put("package", name).put("action", action).put("beforeInstalled", before.installed).put("beforeEnabled", before.enabled)
                    .put("user", user).put("fingerprint", Build.FINGERPRINT).put("snapshot", snap.getString("id"))
                    .put("status", "pending").put("restored", false);
                e.put("device", device()).put("rescueId", rescueId).put("operationBootId", bootId());
                store.getJSONArray("history").put(e);
                save();
                try {
                    String cmd = action.equals("remove") ? "pm uninstall -k --user " + user + " '" + name + "'" : Policy.enabledCommand(name, user, 3);
                    Shell.Result r = Shell.run(cmd);
                    Policy.State after = state(name);
                    boolean ok = action.equals("remove") ? !after.installed : after.installed && after.enabled == 3;
                    e.put("status", ok ? "done" : "failed").put("output", r.output).put("afterInstalled", after.installed).put("afterEnabled", after.enabled);
                    results.put(new JSONObject().put("package", name).put("ok", ok).put("message", ok ? "State verified" : r.output));
                } catch (Exception ex) {
                    e.put("status", "uncertain").put("output", message(ex));
                    uncertain = true;
                    results.put(new JSONObject().put("package", name).put("ok", false).put("message", message(ex)));
                }
                save();
            } catch (Exception ex) {
                results.put(new JSONObject().put("package", name).put("ok", false).put("message", message(ex)));
            }
            if (uncertain) break;
        }
        return new JSONObject().put("results", results).put("snapshot", snap.getString("id"));
    }

    JSONObject restore(String id) throws Exception {
        requireRoot();
        JSONArray history = store.getJSONArray("history");
        JSONObject entry = null;
        for (int i = 0; i < history.length(); i++) if (history.getJSONObject(i).getString("id").equals(id)) entry = history.getJSONObject(i);
        if (entry == null || entry.optBoolean("restored")) throw new IllegalStateException("Change unavailable for restoration.");
        if (entry.getInt("user") != user || !entry.getString("fingerprint").equals(Build.FINGERPRINT))
            throw new IllegalStateException("The ROM or user has changed. Check compatibility before restoring.");
        String name = entry.getString("package");
        if (!Policy.validPackage(name) || catalog.get(name) == null || Policy.protectedName(name)) throw new IllegalStateException("Invalid restoration entry.");
        int position = -1;
        for (int i = 0; i < history.length(); i++) if (history.getJSONObject(i) == entry) position = i;
        for (int i = position + 1; i < history.length(); i++) {
            JSONObject newer = history.getJSONObject(i);
            if (newer.getString("package").equals(name) && !newer.optBoolean("restored") && !newer.optString("status").equals("failed"))
                throw new IllegalStateException("Restore the most recent change for this package first.");
        }
        Policy.State current = state(name);
        boolean beforeInstalled = entry.getBoolean("beforeInstalled");
        int beforeEnabled = entry.getInt("beforeEnabled");
        createSnapshot("Before Resurrection");
        entry.put("restoreStatus", "pending"); save();
        try {
            if (!beforeInstalled) {
                Shell.run(Policy.enabledCommand(name, user, beforeEnabled));
                Shell.Result removal = Shell.run("pm uninstall -k --user " + user + " '" + name + "'");
                Policy.State reverted = state(name);
                if (reverted.installed || reverted.enabled != beforeEnabled) throw new IllegalStateException(removal.output);
                entry.put("restored", true).put("restoreStatus", "done"); save();
                return new JSONObject().put("package", name).put("message", "Previous state restored.");
            }
            if (!current.installed) {
                Shell.Result install = Shell.run("cmd package install-existing --user " + user + " '" + name + "'");
                if (!state(name).installed) throw new IllegalStateException(install.output);
            }
            Shell.Result enable = Shell.run(Policy.enabledCommand(name, user, beforeEnabled));
            Policy.State verified = state(name);
            if (!verified.installed || verified.enabled != beforeEnabled) throw new IllegalStateException("Unconfirmed state: " + enable.output);
            entry.put("restored", true).put("restoreStatus", "done"); save();
            return new JSONObject().put("package", name).put("message", "Original state restored and verified.");
        } catch (Exception ex) { entry.put("restoreStatus", "uncertain"); save(); throw ex; }
    }

    JSONObject inspect(String name) throws Exception {
        if (!Policy.validPackage(name)) throw new IllegalArgumentException("Invalid package.");
        PackageInfo info = pm.getPackageInfo(name, PackageManager.GET_SERVICES | PackageManager.GET_RECEIVERS | PackageManager.GET_PROVIDERS | PackageManager.MATCH_DISABLED_COMPONENTS | PackageManager.MATCH_UNINSTALLED_PACKAGES);
        JSONArray components = new JSONArray();
        if (info.services != null) for (android.content.pm.ServiceInfo c : info.services) components.put(new JSONObject().put("type", "Service").put("name", c.name).put("enabled", c.enabled));
        if (info.receivers != null) for (android.content.pm.ActivityInfo c : info.receivers) components.put(new JSONObject().put("type", "Receiver").put("name", c.name).put("enabled", c.enabled));
        if (info.providers != null) for (android.content.pm.ProviderInfo c : info.providers) components.put(new JSONObject().put("type", "Provider").put("name", c.name).put("enabled", c.enabled));
        JSONObject result = new JSONObject().put("package", name).put("version", info.versionName).put("components", components);
        if (root) {
            requireRoot();
            Shell.Result r = Shell.run("dumpsys activity services '" + name + "'");
            result.put("runtime", r.output.substring(0, Math.min(r.output.length(), 18000)));
        }
        return result;
    }

    JSONObject nativeServices() throws Exception {
        requireRoot();
        Shell.Result props = Shell.run("getprop");
        if (props.code != 0) throw new IllegalStateException(props.output);
        JSONArray services = new JSONArray();
        for (String line : props.output.split("\\n")) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^\\[init\\.svc\\.([^]]+)]: \\[([^]]+)]$").matcher(line.trim());
            if (m.find()) services.put(new JSONObject().put("name", m.group(1)).put("state", m.group(2)));
        }
        Shell.Result binder = Shell.run("service list");
        return new JSONObject().put("services", services).put("binder", binder.output).put("readOnly", true);
    }

    JSONObject importSnapshot(String text) throws Exception {
        JSONObject s = new JSONObject(text);
        if (!s.optString("schema").equals("bloatware-exorcism/1")) throw new IllegalArgumentException("Unknown backup format.");
        JSONArray packages = s.getJSONArray("packages");
        if (packages.length() > 4000) throw new IllegalArgumentException("Backup exceeds the package limit.");
        for (int i = 0; i < packages.length(); i++) {
            JSONObject p = packages.getJSONObject(i);
            p.getBoolean("installed");
            if (!Policy.validPackage(p.getString("package")) || p.getInt("enabled") < 0 || p.getInt("enabled") > 4) throw new IllegalArgumentException("Invalid package state.");
        }
        s.getString("fingerprint"); s.getInt("user"); s.getLong("created");
        s.put("id", UUID.randomUUID().toString()).put("reason", "Holy Backup imported").put("history", new JSONArray());
        store.getJSONArray("snapshots").put(s); save();
        return new JSONObject().put("message", "Backup imported. Compare states before restoring. No changes applied.");
    }

    JSONObject compareSnapshot(String id) throws Exception {
        requireRoot();
        JSONObject snapshot = snapshot(id);
        if (!snapshot.getString("fingerprint").equals(Build.FINGERPRINT) || snapshot.getInt("user") != user)
            throw new IllegalStateException("This backup belongs to a different ROM or user; restoration blocked.");
        JSONArray changes = new JSONArray();
        JSONArray packages = snapshot.getJSONArray("packages");
        for (int i = 0; i < packages.length(); i++) {
            JSONObject p = packages.getJSONObject(i);
            String name = p.getString("package");
            if (!p.getBoolean("installed") || !catalog.containsKey(name)) continue;
            try { allowed(name, true); } catch (Exception e) { continue; }
            Policy.State now = state(name);
            if (!now.installed || now.enabled != p.getInt("enabled")) changes.put(new JSONObject()
                .put("package", name).put("enabled", p.getInt("enabled")).put("wasInstalled", now.installed).put("wasEnabled", now.enabled));
        }
        return new JSONObject().put("changes", changes).put("note", "Only classified, unprotected packages. Packages added after the backup are not removed.");
    }

    JSONObject restoreSnapshot(String id) throws Exception {
        JSONArray changes = compareSnapshot(id).getJSONArray("changes");
        if (changes.length() > 30) throw new IllegalStateException("More than 30 differences. Restore changes from history in smaller batches.");
        if (changes.length() == 0) return new JSONObject().put("results", new JSONArray());
        JSONObject safety = createSnapshot("Before restoring Holy Backup");
        JSONArray results = new JSONArray();
        for (int i = 0; i < changes.length(); i++) {
            JSONObject change = changes.getJSONObject(i);
            String name = change.getString("package");
            requireRoot(); allowed(name, true);
            Policy.State before = state(name);
            JSONObject e = new JSONObject().put("id", UUID.randomUUID().toString()).put("created", System.currentTimeMillis())
                .put("package", name).put("action", "snapshot").put("beforeInstalled", before.installed).put("beforeEnabled", before.enabled)
                .put("user", user).put("fingerprint", Build.FINGERPRINT).put("snapshot", safety.getString("id"))
                .put("status", "pending").put("restored", false);
            store.getJSONArray("history").put(e); save();
            try {
                if (!before.installed) Shell.run("cmd package install-existing --user " + user + " '" + name + "'");
                Shell.Result r = Shell.run(Policy.enabledCommand(name, user, change.getInt("enabled")));
                Policy.State after = state(name);
                boolean ok = after.installed && after.enabled == change.getInt("enabled");
                e.put("status", ok ? "done" : "failed").put("output", r.output).put("afterInstalled", after.installed).put("afterEnabled", after.enabled);
                results.put(new JSONObject().put("package", name).put("ok", ok).put("message", ok ? "Backup state verified" : r.output));
                save();
            } catch (Exception ex) {
                e.put("status", "uncertain").put("output", message(ex)); save();
                results.put(new JSONObject().put("package", name).put("ok", false).put("message", message(ex))); break;
            }
        }
        return new JSONObject().put("results", results);
    }

    String bootId() throws Exception {
        Shell.Result r = Shell.run("cat /proc/sys/kernel/random/boot_id");
        if (r.code != 0 || !r.output.matches("[0-9a-f-]{36}")) throw new IllegalStateException("Boot identifier unavailable.");
        return r.output;
    }

    JSONObject historyEntry(String id) throws Exception {
        JSONArray history = store.getJSONArray("history");
        for (int i = 0; i < history.length(); i++) if (history.getJSONObject(i).getString("id").equals(id)) return history.getJSONObject(i);
        throw new IllegalArgumentException("Operation not found.");
    }

    JSONObject recordValidation(JSONObject args) throws Exception {
        requireRoot();
        JSONObject entry = historyEntry(args.getString("id"));
        if (!entry.getString("fingerprint").equals(Build.FINGERPRINT) || entry.getInt("user") != user)
            throw new IllegalStateException("Validation requires the original ROM and user.");
        String name = entry.getString("package");
        JSONObject checks = args.getJSONObject("checks");
        JSONObject clean = new JSONObject();
        boolean functionsPassed = true;
        boolean anyFailure = false;
        for (String key : new String[]{"camera", "wifi", "calls", "notifications", "bluetooth", "nfc", "reboot"}) {
            String value = checks.getString(key);
            if (!value.matches("pass|fail|not_tested|not_applicable")) throw new IllegalArgumentException("Invalid checklist response.");
            boolean optional = key.equals("nfc") || key.equals("bluetooth");
            if (!value.equals("pass") && !(optional && value.equals("not_applicable"))) functionsPassed = false;
            if (value.equals("fail")) anyFailure = true;
            clean.put(key, value);
        }
        Policy.State now = state(name);
        boolean stateMatches = entry.optString("status").equals("done") && !entry.optBoolean("restored")
            && entry.has("afterInstalled") && now.installed == entry.getBoolean("afterInstalled") && now.enabled == entry.getInt("afterEnabled");
        boolean subsequentBoot = !entry.optString("operationBootId").isEmpty() && !entry.getString("operationBootId").equals(bootId());
        Shell.Result bootProperty = Shell.run("getprop sys.boot_completed");
        boolean bootCompleted = bootProperty.code == 0 && bootProperty.output.equals("1");
        JSONObject validation = new JSONObject().put("id", UUID.randomUUID().toString()).put("created", System.currentTimeMillis())
            .put("operationId", entry.getString("id")).put("package", name).put("action", entry.getString("action"))
            .put("device", device()).put("checks", clean).put("evidenceType", "user_report_with_package_state_check")
            .put("stateMatchesOperation", stateMatches).put("subsequentBootObserved", subsequentBoot)
            .put("bootCompleted", bootCompleted).put("qualifiesForValidatedBuild", functionsPassed && stateMatches && subsequentBoot && bootCompleted)
            .put("outcome", anyFailure ? "reported_failure" : functionsPassed ? "reported_pass" : "incomplete")
            .put("note", args.optString("note", "").substring(0, Math.min(args.optString("note", "").length(), 500)));
        store.getJSONArray("validations").put(validation); save();
        return validation;
    }

    JSONObject validationReport() throws Exception {
        JSONArray patches = new JSONArray();
        JSONArray validations = store.getJSONArray("validations");
        java.util.Map<String, JSONObject> latest = new java.util.LinkedHashMap<>();
        for (int i = 0; i < validations.length(); i++) {
            JSONObject v = validations.getJSONObject(i);
            JSONObject d = v.getJSONObject("device");
            latest.put(v.getString("package") + "|" + d.getString("fingerprint") + "|" + d.getInt("user") + "|" + v.getString("action"), v);
        }
        for (JSONObject v : latest.values()) if (v.optBoolean("qualifiesForValidatedBuild")) {
            patches.put(new JSONObject().put("package", v.getString("package")).put("validatedBuilds", new JSONArray()
                .put(new JSONObject().put("device", v.getJSONObject("device")).put("action", v.getString("action"))
                    .put("validationId", v.getString("id")).put("evidenceType", "self_reported_functional_test")
                    .put("created", v.getLong("created")))));
        }
        return new JSONObject().put("schema", "bloatware-exorcism.validation/1").put("appVersion", BuildConfig.VERSION_NAME)
            .put("created", System.currentTimeMillis()).put("device", device()).put("operations", store.getJSONArray("history"))
            .put("validations", validations).put("catalogValidationPatch", patches)
            .put("warning", "Local records and checklists are not independent evidence or a safety guarantee. The bundled catalog is not promoted automatically.");
    }

    JSONObject captureFixture(String name) throws Exception {
        requireRoot();
        if (!Policy.validPackage(name)) throw new IllegalArgumentException("Invalid package.");
        ApplicationInfo before = pm.getApplicationInfo(name, PackageManager.MATCH_DISABLED_COMPONENTS);
        int enabledBefore = pm.getApplicationEnabledSetting(name);
        Shell.Result r = Shell.run("dumpsys package '" + name + "'");
        if (r.code != 0) throw new IllegalStateException("Could not capture dumpsys.");
        String block = Policy.userBlock(r.output, user).replaceAll("(ceDataInode|deDataInode)=-?[0-9]+", "$1=0");
        int enabledAfter = pm.getApplicationEnabledSetting(name);
        ApplicationInfo after = pm.getApplicationInfo(name, PackageManager.MATCH_DISABLED_COMPONENTS);
        boolean installedBefore = (before.flags & ApplicationInfo.FLAG_INSTALLED) != 0;
        boolean installedAfter = (after.flags & ApplicationInfo.FLAG_INSTALLED) != 0;
        JSONObject parsed = new JSONObject();
        try { Policy.State value = Policy.parseState(block, user); parsed.put("installed", value.installed).put("enabled", value.enabled); }
        catch (Exception e) { parsed.put("error", message(e)); }
        return new JSONObject().put("schema", "bloatware-exorcism.fixture/1").put("provenance", "device_capture_sanitized")
            .put("captured", System.currentTimeMillis()).put("device", device()).put("package", name).put("user", user)
            .put("dumpsys", block).put("dumpSha256", Rescue.digest(block)).put("parser", parsed)
            .put("expected", new JSONObject().put("installed", installedAfter).put("enabled", enabledAfter).put("source", "Android_PackageManager_API"))
            .put("stableObservation", enabledBefore == enabledAfter && installedBefore == installedAfter)
            .put("sanitization", "User scalar block only; ceDataInode/deDataInode zeroed. No full dump or serial number.");
    }

    static byte[] readBytes(java.io.InputStream in) throws Exception {
        try (java.io.InputStream source = in; java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            byte[] bytes = new byte[4096]; int n;
            while ((n = source.read(bytes)) != -1) out.write(bytes, 0, n);
            return out.toByteArray();
        }
    }

    static String message(Exception e) { return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
}
