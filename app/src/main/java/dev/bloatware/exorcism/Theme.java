package dev.bloatware.exorcism;

import android.app.UiModeManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import org.json.JSONObject;
import java.util.Locale;

final class Theme {
    static final String[] ROLES = {"primary", "on-primary", "primary-container", "on-primary-container", "secondary", "on-secondary", "secondary-container", "on-secondary-container", "tertiary", "on-tertiary", "tertiary-container", "on-tertiary-container", "error", "on-error", "error-container", "on-error-container", "surface", "on-surface", "surface-variant", "on-surface-variant", "surface-dim", "surface-bright", "surface-container-lowest", "surface-container-low", "surface-container", "surface-container-high", "surface-container-highest", "outline", "outline-variant", "inverse-surface", "inverse-on-surface", "inverse-primary"};

    static String hex(int color) { return String.format(Locale.ROOT, "#%06x", color & 0xffffff); }

    static int surface(Context context) {
        if (Build.VERSION.SDK_INT >= 34) return context.getColor(android.R.color.system_surface_dark);
        return Color.parseColor("#111318");
    }

    static JSONObject describe(Context context) throws Exception {
        boolean dynamic = Build.VERSION.SDK_INT >= 31;
        int seed = dynamic ? context.getColor(android.R.color.system_accent1_500) : Color.parseColor("#4563eb");
        JSONObject colors = new JSONObject();
        if (Build.VERSION.SDK_INT >= 34) fillSystemColors(context, colors);
        float contrast = Build.VERSION.SDK_INT >= 34 ? context.getSystemService(UiModeManager.class).getContrast() : 0;
        return new JSONObject().put("seed", hex(seed)).put("dynamic", dynamic).put("contrast", contrast).put("colors", colors);
    }

    static void fillSystemColors(Context context, JSONObject colors) throws Exception {
        if (Build.VERSION.SDK_INT < 34) return;
        int[] ids = {android.R.color.system_primary_dark, android.R.color.system_on_primary_dark, android.R.color.system_primary_container_dark, android.R.color.system_on_primary_container_dark, android.R.color.system_secondary_dark, android.R.color.system_on_secondary_dark, android.R.color.system_secondary_container_dark, android.R.color.system_on_secondary_container_dark, android.R.color.system_tertiary_dark, android.R.color.system_on_tertiary_dark, android.R.color.system_tertiary_container_dark, android.R.color.system_on_tertiary_container_dark, android.R.color.system_error_dark, android.R.color.system_on_error_dark, android.R.color.system_error_container_dark, android.R.color.system_on_error_container_dark, android.R.color.system_surface_dark, android.R.color.system_on_surface_dark, android.R.color.system_surface_variant_dark, android.R.color.system_on_surface_variant_dark, android.R.color.system_surface_dim_dark, android.R.color.system_surface_bright_dark, android.R.color.system_surface_container_lowest_dark, android.R.color.system_surface_container_low_dark, android.R.color.system_surface_container_dark, android.R.color.system_surface_container_high_dark, android.R.color.system_surface_container_highest_dark, android.R.color.system_outline_dark, android.R.color.system_outline_variant_dark, android.R.color.system_surface_light, android.R.color.system_on_surface_light, android.R.color.system_primary_light};
        for (int i = 0; i < ids.length; i++) colors.put(ROLES[i], hex(context.getColor(ids[i])));
    }
}
