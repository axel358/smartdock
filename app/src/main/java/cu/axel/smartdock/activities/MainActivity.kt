package cu.axel.smartdock.activities

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import android.widget.ViewSwitcher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cu.axel.smartdock.R
import cu.axel.smartdock.fragments.PreferencesFragment
import cu.axel.smartdock.services.NotificationService
import cu.axel.smartdock.utils.ColorUtils
import cu.axel.smartdock.utils.DeviceUtils


class MainActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var permissionsDialog: AlertDialog
    private lateinit var overlayBtn: MaterialButton
    private lateinit var storageBtn: MaterialButton
    private lateinit var adminBtn: MaterialButton
    private lateinit var notificationsBtn: MaterialButton
    private lateinit var accessibilityBtn: MaterialButton
    private lateinit var locationBtn: MaterialButton
    private lateinit var usageBtn: MaterialButton
    private lateinit var secureBtn: MaterialButton
    private var canDrawOverOtherApps = false
    private var hasStoragePermission = false
    private var isDeviceAdminEnabled = false
    private var hasLocationPermission = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        supportFragmentManager.beginTransaction().replace(R.id.settings_container, PreferencesFragment())
                .commit()
        if (!DeviceUtils.hasStoragePermission(this)) {
            DeviceUtils.requestStoragePermissions(this)
        }
        if (!DeviceUtils.canDrawOverOtherApps(this) || !DeviceUtils.isAccessibilityServiceEnabled(this))
            showPermissionsDialog()
        if (sharedPreferences.getInt("dock_layout", -1) == -1)
            showDockLayoutsDialog()
    }

    override fun onResume() {
        super.onResume()
        if (permissionsDialog.isShowing) {
            updatePermissionsStatus()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_grant_permissions -> showPermissionsDialog()
            R.id.action_switch_layout -> showDockLayoutsDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showPermissionsDialog() {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(R.string.manage_permissions)
        val view = layoutInflater.inflate(R.layout.dialog_permissions, null)
        val viewSwitcher = view.findViewById<ViewSwitcher>(R.id.permissions_view_switcher)
        val requiredBtn = view.findViewById<Button>(R.id.show_required_button)
        val optionalBtn = view.findViewById<Button>(R.id.show_optional_button)
        overlayBtn = view.findViewById(R.id.btn_grant_overlay)
        storageBtn = view.findViewById(R.id.btn_grant_storage)
        adminBtn = view.findViewById(R.id.btn_grant_admin)
        notificationsBtn = view.findViewById(R.id.btn_grant_notifications)
        accessibilityBtn = view.findViewById(R.id.btn_manage_service)
        locationBtn = view.findViewById(R.id.btn_grant_location)
        usageBtn = view.findViewById(R.id.btn_manage_usage)
        secureBtn = view.findViewById(R.id.btn_manage_secure)
        builder.setView(view)
        permissionsDialog = builder.create()
        overlayBtn.setOnClickListener {
            showPermissionInfoDialog(R.string.display_over_other_apps, R.string.display_over_other_apps_desc,
                    (DeviceUtils::grantOverlayPermissions)(this), canDrawOverOtherApps)
        }
        storageBtn.setOnClickListener {
            showPermissionInfoDialog(R.string.storage, R.string.storage_desc,
                    DeviceUtils.requestStoragePermissions(this), hasStoragePermission)
        }
        adminBtn.setOnClickListener {
            showPermissionInfoDialog(R.string.device_administrator, R.string.device_administrator_desc,
                    DeviceUtils.requestDeviceAdminPermissions(this), isDeviceAdminEnabled)
        }
        notificationsBtn.setOnClickListener { showNotificationsDialog() }
        accessibilityBtn.setOnClickListener { showAccessibilityDialog() }
        locationBtn.setOnClickListener {
            showPermissionInfoDialog(R.string.location, R.string.location_desc,
                    DeviceUtils.requestLocationPermissions(this), hasLocationPermission)
        }
        usageBtn.setOnClickListener {
            showPermissionInfoDialog(R.string.usage_stats, R.string.usage_stats_desc,
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)),
                    DeviceUtils.hasUsageStatsPermission(this))
        }
        secureBtn.setOnClickListener { showPermissionInfoDialog(R.string.write_secure, R.string.write_secure_desc, null, true) }
        requiredBtn.setOnClickListener { viewSwitcher.showPrevious() }
        optionalBtn.setOnClickListener { viewSwitcher.showNext() }
        updatePermissionsStatus()
        permissionsDialog.show()
    }

    private fun showDockLayoutsDialog() {
        val editor = sharedPreferences.edit()
        val dialog = MaterialAlertDialogBuilder(this)
        dialog.setTitle(R.string.choose_dock_layout)
        val layout = sharedPreferences.getInt("dock_layout", -1)
        dialog.setSingleChoiceItems(R.array.layouts, layout) { _: DialogInterface?, which: Int ->
            editor.putBoolean("enable_nav_back", which != 0)
            editor.putBoolean("enable_nav_home", which != 0)
            editor.putBoolean("enable_nav_recents", which != 0)
            editor.putBoolean("enable_qs_wifi", which != 0)
            editor.putBoolean("enable_qs_vol", which != 0)
            editor.putBoolean("enable_qs_date", which != 0)
            editor.putBoolean("enable_qs_notif", which != 0)
            editor.putBoolean("app_menu_fullscreen", which != 2)
            editor.putString("launch_mode", if (which != 2) "fullscreen" else "standard")
            editor.putString("max_running_apps", if (which == 0) "4" else "10")
            editor.putString("dock_activation_area", if (which == 2) "5" else "25")
            editor.putInt("dock_layout", which)
            editor.putString("activation_method", if (which != 2) "handle" else "swipe")
            editor.putBoolean("show_notifications", which != 0)
            editor.apply()
        }
        dialog.setPositiveButton(R.string.ok, null)
        dialog.show()
    }

    private fun updatePermissionsStatus() {
        canDrawOverOtherApps = DeviceUtils.canDrawOverOtherApps(this)
        accessibilityBtn.isEnabled = canDrawOverOtherApps
        if (canDrawOverOtherApps) {
            overlayBtn.setIconResource(R.drawable.ic_granted)
            overlayBtn.iconTint = ColorStateList.valueOf(ColorUtils.getThemeColors(this, false)[0])
        }
        if (DeviceUtils.isAccessibilityServiceEnabled(this)) {
            accessibilityBtn.setIconResource(R.drawable.ic_settings)
            accessibilityBtn.setIconTint(ColorStateList.valueOf(ColorUtils.getThemeColors(this, false)[0]))
        } else {
            accessibilityBtn.setIconResource(R.drawable.ic_alert)
            accessibilityBtn.setIconTint(ColorStateList.valueOf(ColorUtils.getThemeColors(this, false)[2]))
        }
        if (DeviceUtils.hasUsageStatsPermission(this)) {
            usageBtn.setIconResource(R.drawable.ic_granted)
            usageBtn.setIconTint(ColorStateList.valueOf(ColorUtils.getThemeColors(this, false)[0]))
        }
        if (DeviceUtils.isServiceRunning(this, NotificationService::class.java)) {
            notificationsBtn.setIconResource(R.drawable.ic_settings)
            notificationsBtn.setIconTint(ColorStateList.valueOf(ColorUtils.getThemeColors(this, false)[0]))
        }
        isDeviceAdminEnabled = DeviceUtils.isDeviceAdminEnabled(this)
        if (isDeviceAdminEnabled) {
            adminBtn.setIconResource(R.drawable.ic_granted)
            adminBtn.setIconTint(ColorStateList.valueOf(ColorUtils.getThemeColors(this, false)[0]))
        }
        hasStoragePermission = DeviceUtils.hasStoragePermission(this)
        if (hasStoragePermission) {
            storageBtn.setIconResource(R.drawable.ic_granted)
            storageBtn.setIconTint(ColorStateList.valueOf(ColorUtils.getThemeColors(this, false)[0]))
        }
        hasLocationPermission = DeviceUtils.hasLocationPermission(this)
        if (hasLocationPermission) {
            locationBtn.setIconResource(R.drawable.ic_granted)
            locationBtn.setIconTint(ColorStateList.valueOf(ColorUtils.getThemeColors(this, false)[0]))
        }
        val hasWriteSettingsPermission = DeviceUtils.hasWriteSettingsPermission(this)
        if (hasWriteSettingsPermission) {
            secureBtn.setIconResource(R.drawable.ic_granted)
            secureBtn.setIconTint(ColorStateList.valueOf(ColorUtils.getThemeColors(this, false)[0]))
        }
    }

    private fun showPermissionInfoDialog(permission: Int, description: Int, grantMethod: Unit?, granted: Boolean) {
        val dialogBuilder = MaterialAlertDialogBuilder(this)
        dialogBuilder.setTitle(permission)
        dialogBuilder.setMessage(description)
        if (!granted)
            dialogBuilder.setPositiveButton(R.string.grant) { _, _ -> grantMethod!!.run {} }
        else dialogBuilder.setPositiveButton(R.string.ok, null)
        dialogBuilder.show()
    }

    private fun showAccessibilityDialog() {
        val dialogBuilder = MaterialAlertDialogBuilder(this)
        dialogBuilder.setTitle(R.string.accessibility_service)
        dialogBuilder.setMessage(R.string.accessibility_service_desc)
        if (DeviceUtils.hasWriteSettingsPermission(this)) {
            dialogBuilder.setPositiveButton(R.string.enable) { _, _ ->
                DeviceUtils.enableService(this)
                Handler(mainLooper).postDelayed({ updatePermissionsStatus() }, 500)
            }
            dialogBuilder.setNegativeButton(R.string.disable) { _, _ ->
                DeviceUtils.disableService(this)
                Handler(mainLooper).postDelayed({ updatePermissionsStatus() }, 500)
            }
        } else {
            dialogBuilder.setPositiveButton(R.string.manage) { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                Toast.makeText(this, R.string.enable_access_help, Toast.LENGTH_LONG).show()
            }
        }
        dialogBuilder.setNeutralButton(R.string.help) { _, _ ->
            startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/axel358/smartdock#grant-restricted-permissions")))
        }
        dialogBuilder.show()
    }

    private fun showNotificationsDialog() {
        val dialogBuilder = MaterialAlertDialogBuilder(this)
        dialogBuilder.setTitle(R.string.notification_access)
        dialogBuilder.setMessage(R.string.notification_access_desc)
        dialogBuilder.setPositiveButton(R.string.manage) { _, _ ->
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            Toast.makeText(this, R.string.enable_access_help, Toast.LENGTH_LONG).show()
        }
        dialogBuilder.setNeutralButton(R.string.help) { _, _ ->
            startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/axel358/smartdock#grant-restricted-permissions")))
        }
        dialogBuilder.show()
    }
}
