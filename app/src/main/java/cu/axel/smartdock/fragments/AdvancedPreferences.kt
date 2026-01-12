package cu.axel.smartdock.fragments

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.core.content.edit
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.LabelFormatter
import cu.axel.smartdock.R
import cu.axel.smartdock.preferences.SliderPreference
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
        var hasWriteSettingsPermission = DeviceUtils.hasWriteSettingsPermission(requireContext())

        findPreference<Preference>("soft_reboot")!!.setOnPreferenceClickListener {
            DeviceUtils.softReboot()
            false
        }

        findPreference<Preference>("share_display_info")!!.setOnPreferenceClickListener {
            val displayInfo = StringBuilder()
            DeviceUtils.getDisplays(requireContext()).forEach { displayInfo.appendLine(it)  }
            startActivity(
                Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, displayInfo.toString())
                    .setType("text/plain")
            )
            false
        }

        val hideNav = findPreference<SwitchPreferenceCompat>("hide_nav_buttons")
        val result = DeviceUtils.runAsRoot("cat /system/build.prop")
        hideNav!!.isChecked = result.contains("qemu.hw.mainkeys=1")
        rootAvailable = result != "error"

        findPreference<Preference>("root_category")!!.isEnabled = rootAvailable
        if (rootAvailable && !hasWriteSettingsPermission) {
            DeviceUtils.grantPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
            hasWriteSettingsPermission = DeviceUtils.hasWriteSettingsPermission(requireContext())
        }

        if (hasWriteSettingsPermission) {
            findPreference<Preference>("secure_category")!!.isEnabled = true
            findPreference<Preference>("custom_display_size")!!.setOnPreferenceClickListener {
                showDisplaySizeDialog(requireContext())
                true
            }
            findPreference<Preference>("status_icon_blacklist")!!.setOnPreferenceClickListener {
                showIBDialog(requireContext())
                false
            }
            val disableHeadsUp = findPreference<SwitchPreferenceCompat>("disable_heads_up")!!
            disableHeadsUp.isChecked = DeviceUtils.getGlobalSetting(
                requireContext(),
                DeviceUtils.HEADS_UP_ENABLED,
                1
            ) == 0
            disableHeadsUp.setOnPreferenceChangeListener { _, isChecked ->
                DeviceUtils.putGlobalSetting(
                    requireContext(),
                    DeviceUtils.HEADS_UP_ENABLED,
                    if (isChecked as Boolean) 0 else 1
                )
            }
        }

        if (rootAvailable) {
            hideNav.setOnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean) {
                    val status =
                        DeviceUtils.runAsRoot("echo qemu.hw.mainkeys=1 >> /system/build.prop")
                    if (status != "error")
                        showRebootDialog(requireContext(), false)
                } else {
                    val status =
                        DeviceUtils.runAsRoot("sed -i /qemu.hw.mainkeys=1/d /system/build.prop")
                    if (status != "error")
                        showRebootDialog(requireContext(), false)
                }
                false
            }

            //ROM specific settings
            if (DeviceUtils.isBliss() && Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
                val disableTaskbar = findPreference<SwitchPreferenceCompat>("disable_taskbar")!!
                disableTaskbar.isVisible = true
                disableTaskbar.isChecked =
                    DeviceUtils.runAsRoot("settings get system ${DeviceUtils.ENABLE_TASKBAR}") == "0"
                disableTaskbar.setOnPreferenceChangeListener { _, isChecked ->
                    return@setOnPreferenceChangeListener DeviceUtils.runAsRoot("settings put system ${DeviceUtils.ENABLE_TASKBAR} ${if (isChecked as Boolean) "0" else "1"}") != "error"
                }
            }
        }

        findPreference<Preference>("backup_preferences")!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                startActivityForResult(
                    Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("*/*").putExtra(
                            Intent.EXTRA_TITLE,
                            requireContext().packageName + "_backup_" + Utils.currentDateString + ".sdp"
                        ),
                    SAVE_REQUEST_CODE
                )
                false
            }
        findPreference<Preference>("restore_preferences")!!.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                startActivityForResult(
                    Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("*/*"),
                    OPEN_REQUEST_CODE
                )
                false
            }
        val dockHeight = findPreference<SliderPreference>("dock_height")!!
        dockHeight.setOnDialogShownListener(object : SliderPreference.OnDialogShownListener {
            override fun onDialogShown() {
                val slider = dockHeight.slider
                slider.isTickVisible = false
                slider.labelBehavior = LabelFormatter.LABEL_GONE
                slider.stepSize = 1f
                slider.value =
                    dockHeight.sharedPreferences!!.getString(dockHeight.key, "56")!!.toFloat()
                slider.valueFrom = 50f
                slider.valueTo = 70f
                slider.addOnChangeListener { _, value, _
                    ->
                    dockHeight.sharedPreferences!!.edit {
                        putString(dockHeight.key, value.toInt().toString())
                    }
                }
            }
        })

        val windowScale: EditTextPreference = findPreference("scale_factor")!!
        windowScale.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            editText.imeOptions = EditorInfo.IME_ACTION_GO
        }

        windowScale.setOnPreferenceChangeListener { _, newValue ->
            val value = newValue as String
            value.isNotEmpty() && value.toFloat() > 0
        }

        val iconPadding: EditTextPreference = findPreference("icon_padding")!!
        iconPadding.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.imeOptions = EditorInfo.IME_ACTION_GO
        }
        iconPadding.setOnPreferenceChangeListener { _, newValue ->
            (newValue as String).isNotEmpty()
        }
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
            if (DeviceUtils.putSecureSetting(
                    context,
                    DeviceUtils.DISPLAY_SIZE,
                    size
                ) && rootAvailable
            ) showRebootDialog(requireContext(), true)
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
            DeviceUtils.putSecureSetting(
                context,
                DeviceUtils.ICON_BLACKLIST, contentEt.text.toString()
            )
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
