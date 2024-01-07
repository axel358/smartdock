package cu.axel.smartdock.utils

import android.annotation.TargetApi
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process

/**
 * Performs operations related to deep shortcuts, such as querying for them, pinning them, etc.
 */
object DeepShortcutManager {
    //TODO: Add free form support
    @TargetApi(25)
    fun startShortcut(shortcutInfo: ShortcutInfo, context: Context) {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        try {
            launcherApps.startShortcut(shortcutInfo.getPackage(), shortcutInfo.id, null, null,
                    Process.myUserHandle())
        } catch (_: Exception) {
        }
    }

    @TargetApi(25)
    fun getShortcutIcon(shortcutInfo: ShortcutInfo, context: Context): Drawable? {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        try {
            val density = context.resources.displayMetrics.density * 48
            return launcherApps.getShortcutIconDrawable(shortcutInfo, density.toInt())
        } catch (_: Exception) {
        }
        return null
    }

    @TargetApi(25)
    fun getShortcuts(app: String, context: Context): List<ShortcutInfo>? {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val queryParams = LauncherApps.ShortcutQuery()
        queryParams.setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC)
        queryParams.setPackage(app)
        return try {
            launcherApps.getShortcuts(queryParams, Process.myUserHandle())
        } catch (_: Exception) {
            null
        }
    }

    fun hasHostPermission(context: Context): Boolean {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        return Build.VERSION.SDK_INT > 24 && launcherApps.hasShortcutHostPermission()
    }
}
