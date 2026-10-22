package dev.bloatware.exorcism;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Policy {
    static final Set<String> CORE = new java.util.HashSet<>(java.util.Arrays.asList(
        "com.miui.powerkeeper", "com.xiaomi.powerchecker", "com.xiaomi.joyose", "com.xiaomi.ugd",
        "com.miui.securitycenter", "com.miui.securitycore", "com.miui.securityadd", "com.lbe.security.miui",
        "com.miui.packageinstaller", "com.miui.global.packageinstaller", "com.miui.core",
        "com.xiaomi.micloud.sdk", "com.xiaomi.xmsf", "com.xiaomi.account", "com.xiaomi.finddevice",
        "com.miui.home", "com.mi.android.globallauncher", "com.miui.miwallpaper", "com.miui.aod",
        "com.miui.guardprovider", "com.xiaomi.market", "com.xiaomi.discover", "com.miui.system", "com.miui.rom"
    ));

    static boolean validPackage(String name) {
        return name != null && name.length() < 256 && name.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+");
    }

    static boolean protectedName(String name) {
        return CORE.contains(name) || name.equals("android") || name.startsWith("com.android.")
            || name.startsWith("com.google.android.") || name.startsWith("com.miui.core.")
            || name.startsWith("dev.bloatware.exorcism") || name.toLowerCase(java.util.Locale.ROOT).contains("kernelsu");
    }

    static final class State {
        final boolean installed;
        final int enabled;
        State(boolean installed, int enabled) { this.installed = installed; this.enabled = enabled; }
        boolean same(State other) { return installed == other.installed && enabled == other.enabled; }
    }

    static String userBlock(String output, int user) {
        if (output == null || user < 0) throw new IllegalArgumentException("Invalid state query.");
        Pattern header = Pattern.compile("^([ \t]*)User[ \t]+([0-9]+)[ \t]*:[^\r\n]*$");
        StringBuilder block = new StringBuilder();
        boolean active = false;
        int count = 0;
        int indent = 0;
        for (String line : output.replace("\r\n", "\n").split("\n")) {
            Matcher h = header.matcher(line);
            if (h.matches()) {
                active = Integer.parseInt(h.group(2)) == user;
                if (active) { count++; indent = h.group(1).length(); block.append(line).append('\n'); }
                continue;
            }
            if (!active || line.trim().isEmpty()) continue;
            int leading = line.length() - line.replaceFirst("^[ \t]*", "").length();
            if (leading <= indent) { active = false; continue; }
            if (line.trim().matches("(?:installed|enabled|ceDataInode|deDataInode|hidden|suspended|stopped|notLaunched)=[^ ]+(?:[ \t]+[^ ]+)*")) block.append(line).append('\n');
            else active = false;
        }
        if (count != 1) throw new IllegalStateException(count == 0 ? "User state not found; change canceled." : "Ambiguous state: duplicate user; change canceled.");
        return block.toString();
    }

    static State parseState(String output, int user) {
        String block = userBlock(output, user);
        String installed = field(block, "installed");
        String enabled = field(block, "enabled");
        if (!installed.equals("true") && !installed.equals("false")) throw new IllegalStateException("Invalid installation state.");
        if (!enabled.matches("[0-4]")) throw new IllegalStateException("Unknown enabled state.");
        return new State(Boolean.parseBoolean(installed), Integer.parseInt(enabled));
    }

    static String field(String block, String key) {
        Matcher m = Pattern.compile("(?:^|[ \t\r\n])" + key + "=([^ \t\r\n]+)").matcher(block);
        if (!m.find()) throw new IllegalStateException("Incomplete package state: " + key);
        String value = m.group(1);
        if (m.find()) throw new IllegalStateException("Duplicate field: " + key);
        return value;
    }

    static String enabledCommand(String name, int user, int enabled) {
        if (!validPackage(name) || user < 0) throw new IllegalArgumentException("Invalid package or user.");
        String action;
        switch (enabled) {
            case 0: action = "default-state"; break;
            case 1: action = "enable"; break;
            case 2: action = "disable"; break;
            case 3: action = "disable-user"; break;
            case 4: action = "disable-until-used"; break;
            default: throw new IllegalArgumentException("Invalid state.");
        }
        return "pm " + action + " --user " + user + " '" + name + "'";
    }
}
