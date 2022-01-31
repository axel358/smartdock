package cu.axel.smartdock.utils;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherApps.ShortcutQuery;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.PopupMenu;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import cu.axel.smartdock.utils.DeviceUtils;
import android.view.SubMenu;
import cu.axel.smartdock.R;
import android.content.Intent;

/**
 * Performs operations related to deep shortcuts, such as querying for them, pinning them, etc.
 */
public class DeepShortcutManager {
    private static final String TAG = "DeepShortcutManager";
    private final Context context;

    // TODO: Replace this with platform constants when the new sdk is available.
    public static final int FLAG_MATCH_DYNAMIC = 1 << 0;
    public static final int FLAG_MATCH_MANIFEST = 1 << 3;
    public static final int FLAG_MATCH_PINNED = 1 << 1;

    private static final int FLAG_GET_ALL =
    FLAG_MATCH_DYNAMIC | FLAG_MATCH_PINNED | FLAG_MATCH_MANIFEST;

    private final LauncherApps launcherApps;
    public static Map<Integer, ShortcutInfo> shortcutInfoMap;

    public DeepShortcutManager(Context context) {
        launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        this.context = context;
    }

    /**
     * Queries for the shortcuts with the package name and provided ids.
     * <p>
     * This method is intended to get the full details for shortcuts when they are added or updated,
     * because we only get "key" fields in onShortcutsChanged().
     */
    public List<ShortcutInfo> queryForFullDetails(String packageName,
                                                  List<String> shortcutIds) {
        return query(FLAG_GET_ALL, packageName, null, shortcutIds);
    }

    /**
     * Gets all the manifest and dynamic shortcuts associated with the given package and user,
     * to be displayed in the shortcuts container on long press.
     */
    public List<ShortcutInfo> queryForShortcutsContainer(ComponentName activity,
                                                         List<String> ids) {
        return query(FLAG_MATCH_MANIFEST | FLAG_MATCH_DYNAMIC,
                     activity.getPackageName(), activity, ids);
    }

    @TargetApi(25)
    public void startShortcut(ShortcutInfo shortcutInfo, String id,
                              Bundle options) {
        try {
            launcherApps.startShortcut(shortcutInfo.getPackage(), id, null,
                                       options, android.os.Process.myUserHandle());
        } catch (Exception e) {
            Log.e(TAG, "Failed to start shortcut", e);
        }
    }

    @TargetApi(25)
    public Drawable getShortcutIconDrawable(ShortcutInfo shortcutInfo, int density) {
        try {
            return launcherApps.getShortcutIconDrawable(
                shortcutInfo, density);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get shortcut icon", e);
        }
        return null;
    }

    /**
     * Query the system server for all the shortcuts matching the given parameters.
     * If packageName == null, we query for all shortcuts with the passed flags, regardless of app.
     * <p>
     * TODO: Use the cache to optimize this so we don't make an RPC every time.
     */
    @TargetApi(25)
    private List<ShortcutInfo> query(int flags, String packageName,
                                     ComponentName activity, List<String> shortcutIds) {
        ShortcutQuery sq = new ShortcutQuery();
        sq.setQueryFlags(flags);
        if (packageName != null) {
            sq.setPackage(packageName);
            sq.setActivity(activity);
            sq.setShortcutIds(shortcutIds);
        }
        List<ShortcutInfo> shortcutInfos = null;
        try {
            shortcutInfos = launcherApps.getShortcuts(sq, android.os.Process.myUserHandle());
        } catch (Exception e) {
            Log.e(TAG, "Failed to query for shortcuts", e);
        }
        if (shortcutInfos == null) {
            return Collections.EMPTY_LIST;
        }
        return shortcutInfos;
    }

    public boolean hasHostPermission() {
        /*
         Will not call shortcut manager unless the launcher is set to default first
         */
        return Build.VERSION.SDK_INT > 24 && launcherApps.hasShortcutHostPermission();
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    public void addAppShortcutsToMenu(PopupMenu menu, String applicationPackage) {
        Intent launchIntentForPackage = context.getPackageManager().getLaunchIntentForPackage(applicationPackage);
        if (launchIntentForPackage == null)
            return;
        ComponentName packageItem = launchIntentForPackage.getComponent();
        DeepShortcutManager shortcutManager = new DeepShortcutManager(context);
        float iconDensity = context.getResources().getDisplayMetrics().density * 48;
        shortcutInfoMap = new HashMap<>();

        if (shortcutManager.hasHostPermission()) {
            List<ShortcutInfo> shortcuts = shortcutManager.queryForShortcutsContainer(packageItem, null);
            if (shortcuts != null && shortcuts.size() != 0) {
                SubMenu  shortcutsMenu = menu.getMenu().addSubMenu(0, 7, 0, "Shortcuts").setIcon(R.drawable.ic_shortcuts);
                int i = 0;
                for (ShortcutInfo sc : shortcuts) {
                    int shortcutMenuID = 10 + i++;
                    shortcutInfoMap.put(shortcutMenuID, sc);
                    shortcutsMenu.add(0, shortcutMenuID, 0, String.valueOf(sc.getShortLabel())).setIcon(shortcutManager.getShortcutIconDrawable(sc, (int) iconDensity));

                }
            }
        }
    }
}
