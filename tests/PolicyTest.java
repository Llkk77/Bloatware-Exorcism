package dev.bloatware.exorcism;

public final class PolicyTest {
    static int checks;
    static void check(boolean value) { checks++; if (!value) throw new AssertionError("Check " + checks); }
    static void rejected(Runnable action) { checks++; try { action.run(); } catch (IllegalArgumentException | IllegalStateException e) { return; } throw new AssertionError("Expected rejection " + checks); }
    public static void main(String[] args) {
        check(Policy.validPackage("com.miui.msa.global"));
        check(Policy.validPackage("com.miui.voiceassistProxy"));
        for (String bad : new String[]{"", "-x", "com.miui.x;reboot", "com.miui.x\nreboot", "com.miui.'x", "com.miui.$(id)", "../system", "android", "com..x"}) check(!Policy.validPackage(bad));
        check(Policy.protectedName("com.miui.powerkeeper"));
        check(Policy.protectedName("com.android.phone"));
        check(Policy.protectedName("com.miui.core.internal.services"));
        check(Policy.protectedName("me.weishu.kernelsu"));
        check(!Policy.protectedName("com.miui.analytics"));
        String dump = "Package [com.miui.analytics]:\n  User 0: installed=true hidden=false enabled=0\n  User 10: installed=false hidden=false enabled=3\n";
        Policy.State zero = Policy.parseState(dump, 0);
        Policy.State ten = Policy.parseState(dump, 10);
        check(zero.installed && zero.enabled == 0);
        check(!ten.installed && ten.enabled == 3);
        rejected(() -> Policy.parseState(dump, 1));
        rejected(() -> Policy.parseState("User 0: installed=true", 0));
        rejected(() -> Policy.parseState("User 0: installed=true enabled=9", 0));
        check(Policy.enabledCommand("com.miui.analytics", 10, 0).equals("pm default-state --user 10 'com.miui.analytics'"));
        check(Policy.enabledCommand("com.miui.analytics", 10, 3).equals("pm disable-user --user 10 'com.miui.analytics'"));
        rejected(() -> Policy.enabledCommand("com.miui.x;id", 0, 1));
        rejected(() -> Policy.enabledCommand("com.miui.x", -1, 1));
        rejected(() -> Policy.enabledCommand("com.miui.x", 0, 8));
        System.out.println(checks + " checks: injection, protected packages, exact user parsing, restoration commands OK");
    }
}
