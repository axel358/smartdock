package cu.axel.smartdock.utils

import android.app.ActivityManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.SystemClock
import androidx.appcompat.content.res.AppCompatResources
import androidx.preference.PreferenceManager
import cu.axel.smartdock.models.App
import cu.axel.smartdock.models.AppTask
import cu.axel.smartdock.models.DockApp
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

object AppUtils {
    const val PINNED_LIST = "pinned.lst"
    const val DOCK_PINNED_LIST = "dock_pinned.lst"
    const val DESKTOP_LIST = "desktop.lst"
    var currentApp = ""
    fun getInstalledApps(packageManager: PackageManager): ArrayList<App> {
        val apps = ArrayList<App>()
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val appsInfo = packageManager.queryIntentActivities(intent, 0)

        //TODO: Filter Google App
        for (appInfo in appsInfo) {
            val label = appInfo.activityInfo.loadLabel(packageManager).toString()
            val icon = appInfo.activityInfo.loadIcon(packageManager)
            val packageName = appInfo.activityInfo.packageName
            apps.add(App(label, packageName, icon))
        }
        apps.sortWith { app: App, app2: App -> app.name.compareTo(app2.name, ignoreCase = true) }
        return apps
    }

    fun getPinnedApps(context: Context, packageManager: PackageManager, type: String): ArrayList<App> {
        val apps = ArrayList<App>()
        try {
            val br = BufferedReader(FileReader(File(context.filesDir, type)))
            var applist: String
            try {
                if (br.readLine().also { applist = it } != null) {
                    val applist2 = applist.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    for (app in applist2) {
                        try {
                            val appInfo = packageManager.getApplicationInfo(app, 0)
                            apps.add(App(packageManager.getApplicationLabel(appInfo).toString(), app,
                                    packageManager.getApplicationIcon(app)))
                        } catch (e: PackageManager.NameNotFoundException) {
                            //app is no longer available, lets unpin it
                            unpinApp(context, app, type)
                        }
                    }
                }
            } catch (_: IOException) {
            }
        } catch (_: FileNotFoundException) {
        }
        return apps
    }

    fun pinApp(context: Context, app: String, type: String) {
        try {
            val file = File(context.filesDir, type)
            val fw = FileWriter(file, true)
            fw.write("$app ")
            fw.close()
        } catch (_: IOException) {
        }
    }

    fun unpinApp(context: Context, app: String, type: String) {
        try {
            val file = File(context.filesDir, type)
            val br = BufferedReader(FileReader(file))
            var applist: String
            if (br.readLine().also { applist = it } != null) {
                applist = applist.replace("$app ", "")
                val fw = FileWriter(file, false)
                fw.write(applist)
                fw.close()
            }
        } catch (_: IOException) {
        }
    }

    fun moveApp(context: Context, app: String, type: String, direction: Int) {
        try {
            val file = File(context.filesDir, type)
            val br = BufferedReader(FileReader(file))
            var applist: String
            var what = ""
            var with = ""
            if (br.readLine().also { applist = it } != null) {
                val apps = applist.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val pos = findInArray(app, apps)
                if (direction == 0 && pos > 0) {
                    what = apps[pos - 1] + " " + app
                    with = app + " " + apps[pos - 1]
                } else if (direction == 1 && pos < apps.size - 1) {
                    what = app + " " + apps[pos + 1]
                    with = apps[pos + 1] + " " + app
                }
                applist = applist.replace(what, with)
                val fw = FileWriter(file, false)
                fw.write(applist)
                fw.close()
            }
        } catch (_: IOException) {
        }
    }

    private fun findInArray(key: String, array: Array<String>): Int {
        for (i in array.indices) {
            if (array[i].contains(key)) return i
        }
        return -1
    }

    fun isPinned(context: Context, app: String, type: String): Boolean {
        try {
            val br = BufferedReader(FileReader(File(context.filesDir, type)))
            var applist: String
            if (br.readLine().also { applist = it } != null) {
                return applist.contains(app)
            }
        } catch (_: IOException) {
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
            val setWindowMode = activityManager.javaClass.getMethod("setTaskWindowingMode", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
            setWindowMode.invoke(activityManager, taskId, mode, false)
        } catch (_: Exception) {
        }
    }

    fun getRunningTasks(activityManager: ActivityManager, packageManager: PackageManager, max: Int): ArrayList<AppTask> {
        val tasksInfo = activityManager.getRunningTasks(max)
        currentApp = tasksInfo[0].baseActivity!!.packageName
        val appTasks = ArrayList<AppTask>()
        for (taskInfo in tasksInfo) {
            try {
                //Exclude systemui, launcher and other system apps from the tasklist
                if (taskInfo.baseActivity!!.packageName.contains("com.android.systemui")
                        || taskInfo.baseActivity!!.packageName.contains("com.google.android.packageinstaller")) continue

                //Hack to save Dock settings activity ftom being excluded
                if (!(taskInfo.topActivity!!.className == "cu.axel.smartdock.activities.MainActivity" || taskInfo.topActivity!!.className == "cu.axel.smartdock.activities.DebugActivity") && taskInfo.topActivity!!.packageName == getCurrentLauncher(packageManager)) continue
                if (Build.VERSION.SDK_INT > 29) {
                    try {
                        val isRunning = taskInfo.javaClass.getField("isRunning")
                        val running = isRunning.getBoolean(taskInfo)
                        if (!running) continue
                    } catch (_: Exception) {
                    }
                }
                appTasks.add(
                        AppTask(taskInfo.id, packageManager.getActivityInfo(taskInfo.topActivity!!, 0).loadLabel(packageManager).toString(),
                                taskInfo.topActivity!!.packageName, packageManager.getActivityIcon(taskInfo.topActivity!!)))
            } catch (_: PackageManager.NameNotFoundException) {
            }
        }
        return appTasks
    }

    fun getRecentTasks(context: Context, max: Int): ArrayList<AppTask> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val start = System.currentTimeMillis() - SystemClock.elapsedRealtime()
        val usageStats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start,
                System.currentTimeMillis())
        val appTasks = ArrayList<AppTask>()
        usageStats.sortWith { usageStats1: UsageStats, usageStats2: UsageStats -> usageStats2.lastTimeUsed.compareTo(usageStats1.lastTimeUsed) }
        for (stat in usageStats) {
            val app = stat.packageName
            try {
                if (isLaunchable(context, app) && app != getCurrentLauncher(context.packageManager)) appTasks.add(AppTask(-1, getPackageLabel(context, app), app,
                        context.packageManager.getApplicationIcon(app)))
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
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(app), 0)
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

    fun makeLaunchBounds(context: Context, mode: String, dockHeight: Int, secondary: Boolean): Rect {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        var left = 0
        var top = 0
        var right = 0
        var bottom = 0
        val deviceWidth = DeviceUtils.getDisplayMetrics(context, secondary).widthPixels
        val deviceHeight = DeviceUtils.getDisplayMetrics(context, secondary).heightPixels
        val statusHeight = DeviceUtils.getStatusBarHeight(context)
        val navHeight = DeviceUtils.getNavBarHeight(context)
        val diff = if (dockHeight - navHeight > 0) dockHeight - navHeight else 0
        val usableHeight = if (Build.VERSION.SDK_INT > 31 && sharedPreferences.getBoolean("navbar_fix", true))
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

    fun resizeTask(context: Context, mode: String, taskId: Int, dockHeight: Int, secondary: Boolean) {
        if (taskId < 0) return
        val bounds = makeLaunchBounds(context, mode, dockHeight, secondary)
        DeviceUtils.runAsRoot("am task resize " + taskId + " " + bounds.left + " " + bounds.top + " " + bounds.right
                + " " + bounds.bottom)
    }

    fun containsTask(apps: ArrayList<DockApp>, task: AppTask): Int {
        for (i in apps.indices) {
            if (apps[i].packageName == task.packageName) return i
        }
        return -1
    }

}
