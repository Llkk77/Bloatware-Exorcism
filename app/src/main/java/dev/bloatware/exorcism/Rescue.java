package dev.bloatware.exorcism;

import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

final class Rescue {
    static final String COMMAND = "/data/adb/ksu/bin/busybox ash /data/adb/modules/bloatware_exorcism_rescue/rescue.sh ";

    static String digest(String text) throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) hex.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        return hex.toString();
    }

    JSONObject status() throws Exception {
        Shell.Result r = Shell.run(COMMAND + "status");
        boolean available = r.code == 0 && r.output.contains("protocol=1");
        return new JSONObject().put("available", available).put("details", available ? r.output : "Module not installed, disabled, not started or inaccessible. Reboot after installation; late loading is not supported.");
    }

    String arm(int user, String fingerprint, JSONArray packages) throws Exception {
        JSONObject status = status();
        if (!status.getBoolean("available")) throw new IllegalStateException(status.getString("details"));
        String id = UUID.randomUUID().toString();
        if (user < 0 || user > 9999 || packages.length() < 1 || packages.length() > 30) throw new IllegalArgumentException("Invalid rescue batch.");
        StringBuilder text = new StringBuilder("BE_RESCUE_V1\n").append(id).append('\n').append(digest(fingerprint)).append('\n').append(user).append('\n');
        for (int i = 0; i < packages.length(); i++) {
            JSONObject row = packages.getJSONObject(i);
            String name = row.getString("package");
            int enabled = row.getInt("enabled");
            if (!Policy.validPackage(name) || Policy.protectedName(name) || !row.getBoolean("installed") || enabled < 0 || enabled > 4)
                throw new IllegalArgumentException("Unsupported rescue state.");
            text.append(name).append('|').append(enabled).append('\n');
        }
        Shell.Result r = Shell.run(COMMAND + "arm", text.toString());
        if (r.code != 0 || !r.output.equals("armed=" + id)) throw new IllegalStateException("The module did not confirm rescue. No new packages will be changed. " + r.output);
        return id;
    }

    JSONObject acknowledge() throws Exception {
        Shell.Result r = Shell.run(COMMAND + "acknowledge");
        if (r.code != 0 || !r.output.startsWith("kept=")) throw new IllegalStateException("Reboot successfully and check device functions before keeping the batch. " + r.output);
        return new JSONObject().put("message", "Batch kept. Rescue record closed.");
    }

    JSONObject rollback() throws Exception {
        Shell.Result r = Shell.run(COMMAND + "rollback");
        if (r.code != 0) throw new IllegalStateException("Rescue incomplete. The record was preserved for another attempt. " + r.output);
        return new JSONObject().put("message", "Rollback confirmed by the module. Review the state and report.");
    }
}
