package cu.axel.smartdock.utils

import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.SystemClock
import android.os.UserManager
import android.view.Display
import androidx.appcompat.content.res.AppCompatResources
import androidx.preference.PreferenceManager
import cu.axel.smartdock.models.App
import cu.axel.smartdock.models.AppTask
import cu.axel.smartdock.models.DockApp
import java.io.File

object AppUtils {
    const val PINNED_LIST = "pinned.lst"
    const val DOCK_PINNED_LIST = "dock_pinned.lst"
    const val DESKTOP_LIST = "desktop.lst"
    var currentApp = ""
    fun getInstalledApps(context: Context): ArrayList<App> {
        val apps = ArrayList<App>()
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        var appsInfo = mutableListOf<LauncherActivityInfo>()
        for (profile in userManager.userProfiles)
            appsInfo.addAll(launcherApps.getActivityList(null, profile))

        appsInfo = appsInfo.sortedWith(compareBy { it.label.toString() }).toMutableList()


        //TODO: Filter Google App
        for (appInfo in appsInfo) {
            apps.add(
                App(
                    appInfo.label.toString(), appInfo.componentName.packageName, appInfo.getIcon(0),
                    appInfo.componentName, appInfo.user
                )
            )
        }

        return apps
    }

    fun getPinnedApps(context: Context, type: String): ArrayList<App> {
        val file = File(context.filesDir, type)
        val apps = ArrayList<App>()
        val appsInfo = mutableListOf<LauncherActivityInfo>()
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        if (file.exists()) {
            for (line in file.readLines()) {
                if (line.isBlank())
                    continue
                val info = line.split(" ")
                val packageName = info[0]
                val userHandle = userManager.getUserForSerialNumber(info[1].toLong())
                val list = launcherApps.getActivityList(packageName, userHandle)
                if (list.isNullOrEmpty())
                    unpinApp(context, packageName, type)
                appsInfo.addAll(list)
            }
        }

        for (appInfo in appsInfo) {
            apps.add(
                App(
                    appInfo.label.toString(), appInfo.componentName.packageName, appInfo.getIcon(0),
                    appInfo.componentName, appInfo.user
                )
            )
        }

        return apps
    }

    fun pinApp(context: Context, app: App, type: String) {
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val file = File(context.filesDir, type)
        file.appendText("${app.packageName} ${userManager.getSerialNumberForUser(app.userHandle)}\n")
    }

    fun unpinApp(context: Context, packageName: String, type: String) {
        val file = File(context.filesDir, type)
        val updatedList = file.readLines().filter { it.split(" ")[0] != packageName }
        if (updatedList.isNotEmpty())
            file.writeText(updatedList.joinToString("\n") + "\n")
        else {
            file.writeText("")
        }
    }

    fun moveApp(context: Context, app: App, type: String, direction: Int) {
        val file = File(context.filesDir, type)
        val lines = file.readLines().toMutableList()

        val lineIndex = lines.indexOfFirst { it.split(" ")[0] == app.packageName }

        if (lineIndex != -1) {
            if (direction == 0 && lineIndex > 0) {
                val line = lines.removeAt(lineIndex)
                lines.add(lineIndex - 1, line)
            } else if (direction == 1 && lineIndex < lines.size - 1) {
                val line = lines.removeAt(lineIndex)
                lines.add(lineIndex + 1, line)
            }

            file.writeText(lines.joinToString("\n") + "\n")
        }
    }

    fun isPinned(context: Context, app: App, type: String): Boolean {
        val file = File(context.filesDir, type)
        if (!file.exists())
            return false
        file.readLines().forEach { line ->
            if (line.split(" ")[0] == app.packageName)
                return true
        }
        return false
    }

    fun isGame(packageManager: PackageManager, packageName: String): Boolean {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                info.category == ApplicationInfo.CATEGORY_GAME
            } else {
                info.flags and ApplicationInfo.FLAG_IS_GAME == ApplicationInfo.FLAG_IS_GAME
            }
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun getCurrentLauncher(packageManager: PackageManager): String {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo!!.activityInfo.packageName
    }

    fun setWindowMode(activityManager: ActivityManager, taskId: Int, mode: Int) {
        try {
            val setWindowMode = activityManager.javaClass.getMethod(
                "setTaskWindowingMode",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType
            )
            setWindowMode.invoke(activityManager, taskId, mode, false)
        } catch (_: Exception) {
        }
    }

    fun getRunningTasks(
        activityManager: ActivityManager,
        packageManager: PackageManager,
        max: Int
    ): ArrayList<AppTask> {
        val tasksInfo = activityManager.getRunningTasks(max)
        currentApp = tasksInfo[0].baseActivity!!.packageName
        val appTasks = ArrayList<AppTask>()
        for (taskInfo in tasksInfo) {
            try {
                //Exclude systemui, launcher and other system apps from the tasklist
                if (taskInfo.baseActivity!!.packageName.contains("com.android.systemui")
                    || taskInfo.baseActivity!!.packageName.contains("com.google.android.packageinstaller")
                    || taskInfo.baseActivity!!.className == "com.android.quickstep.RecentsActivity"
                ) continue

                //Hack to save Dock settings activity ftom being excluded
                if (!(taskInfo.topActivity!!.className == "cu.axel.smartdock.activities.MainActivity" || taskInfo.topActivity!!.className == "cu.axel.smartdock.activities.DebugActivity") && taskInfo.topActivity!!.packageName == getCurrentLauncher(
                        packageManager
                    )
                ) continue
                if (Build.VERSION.SDK_INT > 29) {
                    try {
                        val isRunning = taskInfo.javaClass.getField("isRunning")
                        val running = isRunning.getBoolean(taskInfo)
                        if (!running) continue
                    } catch (_: Exception) {
                    }
                }
                appTasks.add(
                    AppTask(
                        taskInfo.id,
                        packageManager.getActivityInfo(taskInfo.topActivity!!, 0)
                            .loadLabel(packageManager).toString(),
                        taskInfo.topActivity!!.packageName,
                        packageManager.getActivityIcon(taskInfo.topActivity!!)
                    )
                )
            } catch (_: PackageManager.NameNotFoundException) {
            }
        }
        return appTasks
    }

    fun getRecentTasks(context: Context, max: Int): ArrayList<AppTask> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val start = System.currentTimeMillis() - SystemClock.elapsedRealtime()
        val usageStats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST, start,
            System.currentTimeMillis()
        )
        val appTasks = ArrayList<AppTask>()
        usageStats.sortWith { usageStats1: UsageStats, usageStats2: UsageStats ->
            usageStats2.lastTimeUsed.compareTo(
                usageStats1.lastTimeUsed
            )
        }
        for (stat in usageStats) {
            val app = stat.packageName
            try {
                if (isLaunchable(
                        context,
                        app
                    ) && app != getCurrentLauncher(context.packageManager)
                ) appTasks.add(
                    AppTask(
                        -1, getPackageLabel(context, app), app,
                        context.packageManager.getApplicationIcon(app)
                    )
                )
            } catch (_: PackageManager.NameNotFoundException) {
            }
            if (appTasks.size >= max) break
        }
        return appTasks
    }

    fun isSystemApp(context: Context, app: String): Boolean {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(app, 0)
            appInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun isLaunchable(context: Context, app: String): Boolean {
        val resolveInfo = context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(app), 0
        )
        return resolveInfo.size > 0
    }

    fun getPackageLabel(context: Context, packageName: String): String {
        try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            return packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
        }
        return ""
    }

    fun getAppIcon(context: Context, app: String): Drawable {
        return try {
            context.packageManager.getApplicationIcon(app)
        } catch (_: PackageManager.NameNotFoundException) {
            AppCompatResources.getDrawable(context, android.R.drawable.sym_def_app_icon)!!
        }
    }

    private fun makeLaunchBounds(
        context: Context,
        mode: String,
        dockHeight: Int,
        displayId: Int = Display.DEFAULT_DISPLAY
    ): Rect {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        var left = 0
        var top = 0
        var right = 0
        var bottom = 0
        val deviceWidth = DeviceUtils.getDisplayMetrics(context, displayId).widthPixels
        val deviceHeight = DeviceUtils.getDisplayMetrics(context, displayId).heightPixels
        val statusHeight = DeviceUtils.getStatusBarHeight(context)
        val navHeight = DeviceUtils.getNavBarHeight(context)
        val diff = if (dockHeight - navHeight > 0) dockHeight - navHeight else 0

        val usableHeight =
            if (DeviceUtils.shouldApplyNavbarFix())
                deviceHeight - diff - DeviceUtils.getStatusBarHeight(context)
            else
                deviceHeight - dockHeight - DeviceUtils.getStatusBarHeight(context)
        val scaleFactor = sharedPreferences.getString("scale_factor", "1.0")!!.toFloat()
        when (mode) {
            "standard" -> {
                left = (deviceWidth / (5 * scaleFactor)).toInt()
                top = ((usableHeight + statusHeight) / (7 * scaleFactor)).toInt()
                right = deviceWidth - left
                bottom = usableHeight + dockHeight - top
            }

            "maximized" -> {
                right = deviceWidth
                bottom = usableHeight
            }

            "portrait" -> {
                left = deviceWidth / 3
                top = usableHeight / 15
                right = deviceWidth - left
                bottom = usableHeight + dockHeight - top
            }

            "tiled-left" -> {
                right = deviceWidth / 2
                bottom = usableHeight
            }

            "tiled-top" -> {
                right = deviceWidth
                bottom = (usableHeight + statusHeight) / 2
            }

            "tiled-right" -> {
                left = deviceWidth / 2
                right = deviceWidth
                bottom = usableHeight
            }

            "tiled-bottom" -> {
                right = deviceWidth
                top = (usableHeight + statusHeight) / 2
                bottom = usableHeight + statusHeight
            }
        }
        return Rect(left, top, right, bottom)
    }

    fun makeActivityOptions(
        context: Context,
        mode: String,
        dockHeight: Int,
        displayId: Int
    ): ActivityOptions {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val secondary = sharedPreferences.getBoolean("prefer_last_display", false)

        val display: Int =
            if (displayId != Display.DEFAULT_DISPLAY) displayId else (if (secondary) DeviceUtils.getSecondaryDisplay(
                context
            ).displayId else displayId)
        val options: ActivityOptions = ActivityOptions.makeBasic()

        val windowMode: Int
        if (mode == "fullscreen")
            windowMode = 1
        else {
            windowMode = if (Build.VERSION.SDK_INT >= 28) 5 else 2
            options.setLaunchBounds(
                makeLaunchBounds(
                    context,
                    mode,
                    dockHeight,
                    display
                )
            )
        }
        if (Build.VERSION.SDK_INT > 28)
            options.setLaunchDisplayId(display)
        val methodName =
            if (Build.VERSION.SDK_INT >= 28) "setLaunchWindowingMode" else "setLaunchStackId"
        val method =
            ActivityOptions::class.java.getMethod(methodName, Int::class.javaPrimitiveType)
        method.invoke(options, windowMode)

        return options
    }

    fun resizeTask(context: Context, mode: String, taskId: Int, dockHeight: Int) {
        if (taskId < 0)
            return
        val bounds = makeLaunchBounds(context, mode, dockHeight)
        DeviceUtils.runAsRoot(
            "am task resize " + taskId + " " + bounds.left + " " + bounds.top + " " + bounds.right
                    + " " + bounds.bottom
        )
    }

    fun containsTask(apps: ArrayList<DockApp>, task: AppTask): Int {
        for (i in apps.indices) {
            if (apps[i].packageName == task.packageName) return i
        }
        return -1
    }

}
