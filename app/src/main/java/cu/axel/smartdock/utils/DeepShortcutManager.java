package cu.axel.smartdock.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherApps.ShortcutQuery;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;

import java.util.Collections;
import java.util.List;

/**
 * Performs operations related to deep shortcuts, such as querying for them, pinning them, etc.
 */
public class DeepShortcutManager {

    //TODO: Add free form support
    @TargetApi(25)
    public static void startShortcut(ShortcutInfo shortcutInfo, Context context) {
        LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        try {
            launcherApps.startShortcut(shortcutInfo.getPackage(), shortcutInfo.getId(), null, null,
                    android.os.Process.myUserHandle());
        } catch (Exception e) {
        }
    }

    @TargetApi(25)
    public static Drawable getShortcutIcon(ShortcutInfo shortcutInfo, Context context) {
        LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        try {
            float density = context.getResources().getDisplayMetrics().density * 48;
            return launcherApps.getShortcutIconDrawable(shortcutInfo, (int) density);
        } catch (Exception e) {
        }
        return null;
    }

    @TargetApi(25)
    public static List<ShortcutInfo> getShortcuts(String app, Context context) {
        LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);

        ShortcutQuery queryParams = new ShortcutQuery();
        queryParams.setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST | LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC);
        queryParams.setPackage(app);

        List<ShortcutInfo> shortcutInfos = Collections.EMPTY_LIST;
        try {
            shortcutInfos = launcherApps.getShortcuts(queryParams, android.os.Process.myUserHandle());
        } catch (Exception e) {
        }

        return shortcutInfos;
    }

    public static boolean hasHostPermission(Context context) {
        LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        return Build.VERSION.SDK_INT > 24 && launcherApps.hasShortcutHostPermission();
    }

}
