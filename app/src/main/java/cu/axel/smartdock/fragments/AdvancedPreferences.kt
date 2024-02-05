package cu.axel.smartdock.fragments

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.EditText
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider
import cu.axel.smartdock.R
import cu.axel.smartdock.preferences.SliderPreference
import cu.axel.smartdock.utils.AppUtils
import cu.axel.smartdock.utils.DeviceUtils
import cu.axel.smartdock.utils.Utils

private const val SAVE_REQUEST_CODE = 236
private const val OPEN_REQUEST_CODE = 632

class AdvancedPreferences : PreferenceFragmentCompat() {
    private var rootAvailable = false
    override fun onCreatePreferences(arg0: Bundle?, arg1: String?) {
        setPreferencesFromResource(R.xml.preferences_advanced, arg1)
        val preferLastDisplay = findPreference<Preference>("prefer_last_display")
        preferLastDisplay!!.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        var hasWriteSettingsPermission = DeviceUtils.hasWriteSettingsPermission(requireActivity())
        preferLastDisplay.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            showAccessibilityDialog(requireContext())
            true
        }
        findPreference<Preference>("soft_reboot")!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            DeviceUtils.softReboot()
            false
        }
        val moveToSystem = findPreference<Preference>("move_to_system")
        moveToSystem!!.isVisible = !AppUtils.isSystemApp(requireContext(), requireContext().packageName)
        moveToSystem.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            try {
                val appInfo = requireActivity().packageManager
                        .getApplicationInfo(requireActivity().packageName, 0)
                val appDir = appInfo.sourceDir.substring(0, appInfo.sourceDir.lastIndexOf("/"))
                DeviceUtils.runAsRoot("mv $appDir /system/priv-app/")
                DeviceUtils.reboot()
            } catch (_: PackageManager.NameNotFoundException) {
            }
            false
        }
        val hideNav = findPreference<CheckBoxPreference>("hide_nav_buttons")
        val result = DeviceUtils.runAsRoot("cat /system/build.prop")
        hideNav!!.isChecked = result.contains("qemu.hw.mainkeys=1")
        rootAvailable = result != "error"
        findPreference<Preference>("root_category")!!.isEnabled = rootAvailable
        if (rootAvailable && !hasWriteSettingsPermission) {
            DeviceUtils.grantPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
            hasWriteSettingsPermission = DeviceUtils.hasWriteSettingsPermission(requireActivity())
        }
        val hideStatus = findPreference<CheckBoxPreference>("hide_status_bar")
        hideStatus!!.isVisible = Build.VERSION.SDK_INT < 31
        if (hasWriteSettingsPermission) {
            findPreference<Preference>("secure_category")!!.isEnabled = true
            findPreference<Preference>("custom_display_size")!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                showDisplaySizeDialog(requireContext())
                true
            }
            hideStatus.isChecked = (DeviceUtils.getGlobalSetting(requireActivity(), DeviceUtils.POLICY_CONTROL, "")
                    == DeviceUtils.IMMERSIVE_APPS)
            hideStatus.setOnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean) {
                    if (DeviceUtils.putGlobalSetting(requireActivity(), DeviceUtils.POLICY_CONTROL,
                                    DeviceUtils.IMMERSIVE_APPS)) {
                        if (rootAvailable)
                            showRebootDialog(requireContext(), true)
                    }
                } else {
                    if (DeviceUtils.putGlobalSetting(requireActivity(), DeviceUtils.POLICY_CONTROL, null)) {
                        if (rootAvailable)
                            showRebootDialog(requireActivity(), true)
                    }
                }
                false
            }
            findPreference<Preference>("status_icon_blacklist")!!.setOnPreferenceClickListener {
                showIBDialog(requireContext())
                false
            }
            val disableHeadsUp = findPreference<CheckBoxPreference>("disable_heads_up")!!
            disableHeadsUp.isChecked = DeviceUtils.getGlobalSetting(requireActivity(), DeviceUtils.HEADS_UP_ENABLED, 1) == 0
            disableHeadsUp.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference?, p2: Any ->
                if (p2 as Boolean) {
                    DeviceUtils.putGlobalSetting(requireActivity(), DeviceUtils.HEADS_UP_ENABLED, 0)
                } else {
                    DeviceUtils.putGlobalSetting(requireActivity(), DeviceUtils.HEADS_UP_ENABLED, 1)
                }
            }
        }
        hideNav.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                val status = DeviceUtils.runAsRoot("echo qemu.hw.mainkeys=1 >> /system/build.prop")
                if (status != "error") {
                    hideNav.sharedPreferences!!.edit().putBoolean("navbar_fix", false).apply()
                    showRebootDialog(requireContext(), false)
                }
            } else {
                val status = DeviceUtils.runAsRoot("sed -i /qemu.hw.mainkeys=1/d /system/build.prop")
                if (status != "error") {
                    hideNav.sharedPreferences!!.edit().putBoolean("navbar_fix", true).apply()
                    showRebootDialog(requireContext(), false)
                }
            }
            false
        }
        findPreference<Preference>("navbar_fix")!!.isVisible = Build.VERSION.SDK_INT > 31
        findPreference<Preference>("backup_preferences")!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("*/*").putExtra(Intent.EXTRA_TITLE,
                            requireActivity().packageName + "_backup_" + Utils.currentDateString + ".sdp"),
                    SAVE_REQUEST_CODE)
            false
        }
        findPreference<Preference>("restore_preferences")!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            startActivityForResult(
                    Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"),
                    OPEN_REQUEST_CODE)
            false
        }
        val dockHeight = findPreference<SliderPreference>("dock_height")!!
        dockHeight.setOnDialogShownListener(object : SliderPreference.OnDialogShownListener {
            override fun onDialogShown() {
                val slider = dockHeight.slider
                slider.labelBehavior = LabelFormatter.LABEL_GONE
                slider.stepSize = 1f
                slider.value = dockHeight.sharedPreferences!!.getString("dock_height", "58")!!.toFloat()
                slider.valueFrom = 50f
                slider.valueTo = 70f
                slider.addOnChangeListener { _, value, _
                    ->
                    dockHeight.sharedPreferences!!.edit().putString("dock_height", value.toInt().toString()).apply()
                }
            }

        })
    }

    private fun showDisplaySizeDialog(context: Context) {
        val dialog = MaterialAlertDialogBuilder(context)
        dialog.setTitle(R.string.custom_display_size_title)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_display_size, null)
        val contentEt = view.findViewById<EditText>(R.id.display_size_et)
        contentEt.setText(DeviceUtils.getSecureSetting(context, DeviceUtils.DISPLAY_SIZE, "") + "")
        dialog.setPositiveButton(R.string.ok) { _, _ ->
            val value = contentEt.text.toString()
            val size = if (value == "0") "" else value
            if (DeviceUtils.putSecureSetting(context, DeviceUtils.DISPLAY_SIZE, size) && rootAvailable) showRebootDialog(requireContext(), true)
        }
        dialog.setNegativeButton(getString(R.string.cancel), null)
        dialog.setView(view)
        dialog.show()
    }

    private fun showIBDialog(context: Context) {
        val dialog = MaterialAlertDialogBuilder(context)
        dialog.setTitle(R.string.icon_blacklist)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_icon_blacklist, null)
        val contentEt = view.findViewById<EditText>(R.id.icon_blacklist_et)
        contentEt.setText(DeviceUtils.getSecureSetting(context, DeviceUtils.ICON_BLACKLIST, ""))
        dialog.setPositiveButton(R.string.ok) { _, _ ->
            DeviceUtils.putSecureSetting(context,
                    DeviceUtils.ICON_BLACKLIST, contentEt.text.toString())
        }
        dialog.setNegativeButton(getString(R.string.cancel), null)
        dialog.setView(view)
        dialog.show()
    }

    private fun showRebootDialog(context: Context, softReboot: Boolean) {
        val dialog = MaterialAlertDialogBuilder(context)
        dialog.setTitle(getString(R.string.reboot_required_title))
        dialog.setMessage(getString(R.string.reboot_required_text))
        dialog.setPositiveButton(getString(R.string.ok)) { _, _ ->
            if (softReboot) DeviceUtils.softReboot() else DeviceUtils.reboot()
        }
        dialog.setNegativeButton(getString(R.string.cancel), null)
        dialog.show()
    }

    private fun showAccessibilityDialog(context: Context) {
        val dialog = MaterialAlertDialogBuilder(context)
        dialog.setTitle(R.string.restart)
        dialog.setMessage(R.string.restart_accessibility)
        dialog.setNegativeButton(getString(R.string.cancel), null)
        dialog.setPositiveButton(getString(R.string.open_accessibility)
        ) { _, _ -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val data = intent!!.data!!
            if (requestCode == SAVE_REQUEST_CODE) {
                Utils.backupPreferences(requireContext(), data)
            } else if (requestCode == OPEN_REQUEST_CODE) {
                Utils.restorePreferences(requireContext(), data)
            }
        }
    }
}
