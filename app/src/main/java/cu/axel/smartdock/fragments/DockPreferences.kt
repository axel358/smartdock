package cu.axel.smartdock.fragments

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.LabelFormatter
import cu.axel.smartdock.R
import cu.axel.smartdock.preferences.SliderPreference
import androidx.core.content.edit

class DockPreferences : PreferenceFragmentCompat() {
    override fun onCreatePreferences(arg0: Bundle?, arg1: String?) {
        setPreferencesFromResource(R.xml.preferences_dock, arg1)
        findPreference<Preference>("auto_pin")!!.setOnPreferenceClickListener {
            showAutopinDialog(requireContext())
            false
        }
        val activationArea = findPreference<SliderPreference>("dock_activation_area")
        activationArea!!.isVisible =
            activationArea.sharedPreferences!!.getString("activation_method", "swipe") == "swipe"
        activationArea.setOnDialogShownListener(object : SliderPreference.OnDialogShownListener {
            override fun onDialogShown() {
                val slider = activationArea.slider
                slider.isTickVisible = false
                slider.labelBehavior = LabelFormatter.LABEL_GONE
                slider.stepSize = 1f
                slider.value =
                    activationArea.sharedPreferences!!.getString(activationArea.key, "10")!!
                        .toFloat()
                slider.valueFrom = 1f
                slider.valueTo = 50f
                slider.addOnChangeListener { _, value, _ ->
                    activationArea.sharedPreferences!!.edit {
                        putString(activationArea.key, value.toInt().toString())
                    }
                }
            }
        })
        val handleOpacity = findPreference<SliderPreference>("handle_opacity")
        handleOpacity!!.isVisible =
            handleOpacity.sharedPreferences!!.getString("activation_method", "swipe") == "handle"
        handleOpacity.setOnDialogShownListener(object : SliderPreference.OnDialogShownListener {
            override fun onDialogShown() {
                val slider = handleOpacity.slider
                slider.isTickVisible = false
                slider.labelBehavior = LabelFormatter.LABEL_GONE
                slider.stepSize = 0.1f
                slider.value =
                    handleOpacity.sharedPreferences!!.getString(handleOpacity.key, "0.5f")!!
                        .toFloat()
                slider.valueFrom = 0.2f
                slider.valueTo = 1f
                slider.addOnChangeListener { _, value, _ ->
                    handleOpacity.sharedPreferences!!.edit {
                        putString(handleOpacity.key, value.toString())
                    }
                }
            }
        })
        val handlePosition = findPreference<Preference>("handle_position")
        handlePosition!!.isVisible = handleOpacity.isVisible
        val activationMethod = findPreference<Preference>("activation_method")
        activationMethod!!.setOnPreferenceChangeListener { _, newValue ->
            handleOpacity.isVisible = newValue.toString() == "handle"
            activationArea.isVisible = newValue.toString() == "swipe"
            handlePosition.isVisible = handleOpacity.isVisible
            true
        }

        val maxRunningApps: EditTextPreference = findPreference("max_running_apps")!!
        maxRunningApps.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.imeOptions = EditorInfo.IME_ACTION_GO
        }
        maxRunningApps.setOnPreferenceChangeListener { _, newValue ->
            val value = newValue as String
            value.isNotEmpty() && value.toInt() < 50
        }
    }

    private fun showAutopinDialog(context: Context) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sharedPreferences.edit()
        val dialogBuilder = MaterialAlertDialogBuilder(context)
        dialogBuilder.setTitle(R.string.auto_pin_summary)
        val view = layoutInflater.inflate(R.layout.dialog_auto_pin, null)
        val startupSwitch = view.findViewById<MaterialSwitch>(R.id.pin_startup_switch)
        val windowedSwitch = view.findViewById<MaterialSwitch>(R.id.pin_window_switch)
        val fullscreenSwitch = view.findViewById<MaterialSwitch>(R.id.unpin_fullscreen_switch)
        startupSwitch.isChecked = sharedPreferences.getBoolean("pin_dock", true)
        windowedSwitch.isChecked = sharedPreferences.getBoolean("auto_pin", true)
        fullscreenSwitch.isChecked = sharedPreferences.getBoolean("auto_unpin", true)
        startupSwitch.setOnCheckedChangeListener { _, checked ->
            editor.putBoolean(
                "pin_dock",
                checked
            ).apply()
        }
        windowedSwitch.setOnCheckedChangeListener { _, checked ->
            editor.putBoolean(
                "auto_pin",
                checked
            ).commit()
        }
        fullscreenSwitch.setOnCheckedChangeListener { _, checked ->
            editor.putBoolean(
                "auto_unpin",
                checked
            ).commit()
        }
        dialogBuilder.setView(view)
        dialogBuilder.setPositiveButton(R.string.ok, null)
        dialogBuilder.show()
    }
}
