package cu.axel.smartdock.utils

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.UserHandle
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Display
import android.view.accessibility.AccessibilityManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import cu.axel.smartdock.services.DockService
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import android.os.UserManager

object DeviceUtils {
    const val DISPLAY_SIZE = "display_density_forced"
    const val ICON_BLACKLIST = "icon_blacklist"
    const val POLICY_CONTROL = "policy_control"
    const val IMMERSIVE_APPS = "immersive.status=apps"
    const val HEADS_UP_ENABLED = "heads_up_notifications_enabled"
    const val ENABLE_TASKBAR = "enable_taskbar"
    const val SETTING_OVERLAYS = "secure_overlay_settings"
    private const val SERVICE_NAME = "cu.axel.smartdock/cu.axel.smartdock.services.DockService"
    private const val ENABLED_ACCESSIBILITY_SERVICES = "enabled_accessibility_services"

    @get:Throws(IOException::class)
    val rootAccess: Process
        //Xtr126
        get() {
            val paths = arrayOf(
                "/sbin/su", "/system/sbin/su", "/system/bin/su", "/system/xbin/su", "/su/bin/su",
                "/magisk/.core/bin/su"
            )
            for (path in paths) {
                if (File(path).canExecute()) return Runtime.getRuntime().exec(path)
            }
            return Runtime.getRuntime().exec("/system/bin/su")
        }

    fun runAsRoot(command: String): String {
        val output = StringBuilder()
        try {
            val process = rootAccess
            val os = DataOutputStream(process.outputStream)
            os.writeBytes(command + "\n")
            os.flush()
            os.close()
            val br = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            br.close()
        } catch (e: IOException) {
            return "error"
        }
        return output.toString().trimEnd('\n')
    }

    //Device control
    fun lockScreen(context: Context) {
        val devicePolicyManager =
            context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        try {
            devicePolicyManager.lockNow()
        } catch (_: SecurityException) {
        }
    }

    fun sendKeyEvent(keycode: Int) {
        runAsRoot("input keyevent $keycode")
    }

    fun softReboot() {
        runAsRoot("setprop ctl.restart zygote")
    }

    fun reboot() {
        runAsRoot("am start -a android.intent.action.REBOOT")
    }

    fun shutdown() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) runAsRoot("am start -a android.intent.action.ACTION_REQUEST_SHUTDOWN") else runAsRoot(
            "am start -a com.android.internal.intent.action.REQUEST_SHUTDOWN"
        )
    }

    fun toggleVolume(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_SAME,
            AudioManager.FLAG_SHOW_UI
        )
    }

    fun playEventSound(context: Context, event: String) {
        val soundUri =
            PreferenceManager.getDefaultSharedPreferences(context).getString(event, "default")
        if (soundUri != "default") {
            try {
                val sound = Uri.parse(soundUri)
                if (sound != null) {
                    val mp = MediaPlayer.create(context, sound)
                    mp.start()
                    mp.setOnCompletionListener { mp.release() }
                }
            } catch (_: Exception) {
            }
        }
    }

    //Device Settings
    fun putSecureSetting(context: Context, setting: String, value: Int): Boolean {
        return try {
            Settings.Secure.putInt(context.contentResolver, setting, value)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun getSecureSetting(context: Context, setting: String, defaultValue: Int): Int {
        return try {
            Settings.Secure.getInt(context.contentResolver, setting)
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun getSecureSetting(context: Context, setting: String, defaultValue: String): String {
        return try {
            val value = Settings.Secure.getString(context.contentResolver, setting)
            value ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun putSecureSetting(context: Context, setting: String, value: String): Boolean {
        return try {
            Settings.Secure.putString(context.contentResolver, setting, value)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun putGlobalSetting(context: Context, setting: String, value: String?): Boolean {
        return try {
            Settings.Global.putString(context.contentResolver, setting, value)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun getGlobalSetting(context: Context, setting: String, defaultValue: String): String {
        return try {
            val value = Settings.Global.getString(context.contentResolver, setting)
            value ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun putGlobalSetting(context: Context, setting: String, value: Int): Boolean {
        return try {
            Settings.Global.putInt(context.contentResolver, setting, value)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun getGlobalSetting(context: Context, setting: String, defaultValue: Int): Int {
        return try {
            Settings.Global.getInt(context.contentResolver, setting)
        } catch (e: Exception) {
            defaultValue
        }
    }

    //Device info
    fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    fun getNavBarHeight(context: Context): Int {
        var result = 0
        val resourceId =
            context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    fun getUserName(context: Context): String? {
        val um = context.getSystemService(Context.USER_SERVICE) as UserManager
        try {
            return um.userName
        } catch (_: Exception) {
        }
        return null
    }

    fun getUserIcon(context: Context): Bitmap? {
        val um = context.getSystemService(Context.USER_SERVICE) as UserManager
        var userIcon: Bitmap? = null
        try {
            val getUserIcon = um.javaClass.getMethod("getUserIcon", Int::class.javaPrimitiveType)
            val myUserId = UserHandle::class.java.getMethod("myUserId")
            val id = myUserId.invoke(UserHandle::class.java) as Int
            userIcon = getUserIcon.invoke(um, id) as Bitmap?
            if (userIcon != null) userIcon = Utils.getCircularBitmap(userIcon)
        } catch (_: Exception) {
        }
        return userIcon
    }

    fun getDisplays(context: Context): Array<Display> {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        return dm.displays
    }

    fun getSecondaryDisplay(context: Context): Display {
        val displays = getDisplays(context)
        return displays[displays.size - 1]
    }

    fun getDisplayMetrics(
        context: Context,
        displayId: Int = Display.DEFAULT_DISPLAY
    ): DisplayMetrics {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = dm.getDisplay(displayId)
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)
        return metrics
    }

    fun getDisplayContext(context: Context, secondary: Boolean = false): Context {
        return if (secondary) context.createDisplayContext(getSecondaryDisplay(context)) else context
    }

    @SuppressLint("PrivateApi")
    fun getSystemProp(prop: String): String {
        val systemPropertiesClass = Class.forName("android.os.SystemProperties")
        val getMethod = systemPropertiesClass.getMethod("get", String::class.java)

        return getMethod.invoke(null, prop) as String
    }

    fun isBliss(): Boolean {
        return getSystemProp("ro.bliss.version").isNotEmpty()
    }

    fun shouldApplyNavbarFix(): Boolean {
        return Build.VERSION.SDK_INT > 31 && isNavbarEnabled()
    }

    fun isNavbarEnabled(): Boolean {
        return getSystemProp("qemu.hw.mainkeys") != "1"
    }

    //Permissions
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (enabledService in enabledServices) {
            val serviceInfo = enabledService.resolveInfo.serviceInfo
            if (serviceInfo.packageName == context.packageName && serviceInfo.name == DockService::class.java.name) {
                return true
            }
        }
        return false
    }

    fun hasStoragePermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestStoragePermissions(context: Activity) {
        ActivityCompat.requestPermissions(
            context,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            8
        )
    }

    fun hasWriteSettingsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun grantPermission(permission: String): Boolean {
        val result = runAsRoot("pm grant cu.axel.smartdock $permission")
        return result.isEmpty()
    }

    fun grantOverlayPermissions(context: Activity) {
        context.startActivityForResult(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.packageName)
            ),
            8
        )
    }

    fun requestDeviceAdminPermissions(context: Activity) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(
            DevicePolicyManager.EXTRA_DEVICE_ADMIN,
            ComponentName(context, DeviceAdminReceiver::class.java)
        )
        context.startActivityForResult(intent, 8)
    }

    fun isDeviceAdminEnabled(context: Context): Boolean {
        val devicePolicyManager =
            context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val deviceAdmins = devicePolicyManager.activeAdmins
        if (deviceAdmins != null) {
            for (deviceAdmin in deviceAdmins) {
                if (deviceAdmin.packageName == context.packageName) {
                    return true
                }
            }
        }
        return false
    }

    fun hasRecentAppsPermission(context: Context): Boolean {
        return AppUtils.isSystemApp(context, context.packageName) || checkAppOpsPermission(
            context,
            AppOpsManager.OPSTR_GET_USAGE_STATS
        )
    }

    private fun checkAppOpsPermission(context: Context, permission: String): Boolean {
        val packageManager = context.packageManager
        val applicationInfo: ApplicationInfo = try {
            packageManager.getApplicationInfo(context.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.checkOpNoThrow(
            permission,
            applicationInfo.uid,
            applicationInfo.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    //Service control
    fun enableService(context: Context) {
        val services = getSecureSetting(context, ENABLED_ACCESSIBILITY_SERVICES, "")
        if (services.contains(SERVICE_NAME)) return
        val newServices: String =
            if (services.isEmpty()) SERVICE_NAME else "$services:$SERVICE_NAME"
        putSecureSetting(context, ENABLED_ACCESSIBILITY_SERVICES, newServices)
    }

    fun disableService(context: Context) {
        val services = getSecureSetting(context, ENABLED_ACCESSIBILITY_SERVICES, "")
        if (!services.contains(SERVICE_NAME)) return
        var newServices = ""
        if (services.contains("$SERVICE_NAME:")) newServices = services.replace(
            "$SERVICE_NAME:",
            ""
        ) else if (services.contains(":$SERVICE_NAME")) newServices = services.replace(
            ":$SERVICE_NAME",
            ""
        ) else if (services.contains(SERVICE_NAME)) newServices = services.replace(SERVICE_NAME, "")
        putSecureSetting(context, ENABLED_ACCESSIBILITY_SERVICES, newServices)
    }

    fun restartService(context: Context) {
        disableService(context)
        enableService(context)
    }

    fun canDrawOverOtherApps(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    @Suppress("DEPRECATION")
    fun isServiceRunning(context: Context, serviceName: Class<*>): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceName.name == service.service.className) {
                return true
            }
        }
        return false
    }

    fun getSettingsOverlaysAllowed(context: Context): Boolean {
        return getSecureSetting(context, SETTING_OVERLAYS, 0) == 1
    }
}
