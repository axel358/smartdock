package cu.axel.smartdock.fragments

import android.content.Context
import android.os.Bundle
import android.widget.CheckBox
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cu.axel.smartdock.R

class DockPreferences : PreferenceFragmentCompat() {
    override fun onCreatePreferences(arg0: Bundle?, arg1: String?) {
        setPreferencesFromResource(R.xml.preferences_dock, arg1)
        findPreference<Preference>("auto_pin")!!.setOnPreferenceClickListener {
            showAutopinDialog(requireContext())
            false
        }
        val activationArea = findPreference<Preference>("dock_activation_area")
        activationArea!!.isVisible = activationArea.sharedPreferences!!.getString("activation_method", "swipe") == "swipe"
        val handleOpacity = findPreference<Preference>("handle_opacity")
        handleOpacity!!.isVisible = handleOpacity.sharedPreferences!!.getString("activation_method", "swipe") == "handle"
        val handlePosition = findPreference<Preference>("handle_position")
        handlePosition!!.isVisible = handleOpacity.isVisible
        val activationMethod = findPreference<Preference>("activation_method")
        activationMethod!!.setOnPreferenceChangeListener { _, newValue ->
            handleOpacity.isVisible = newValue.toString() == "handle"
            activationArea.isVisible = newValue.toString() == "swipe"
            handlePosition.isVisible = handleOpacity.isVisible
            true
        }
    }

    private fun showAutopinDialog(context: Context) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sharedPreferences.edit()
        val dialogBuilder = MaterialAlertDialogBuilder(context)
        dialogBuilder.setTitle(R.string.auto_pin_summary)
        val view = layoutInflater.inflate(R.layout.dialog_auto_pin, null)
        val startupChkbx = view.findViewById<CheckBox>(R.id.pin_startup_chkbx)
        val windowedChkbx = view.findViewById<CheckBox>(R.id.pin_window_chkbx)
        val fullscreenChkbx = view.findViewById<CheckBox>(R.id.unpin_fullscreen_chkbx)
        startupChkbx.isChecked = sharedPreferences.getBoolean("pin_dock", true)
        windowedChkbx.isChecked = sharedPreferences.getBoolean("auto_pin", true)
        fullscreenChkbx.isChecked = sharedPreferences.getBoolean("auto_unpin", true)
        startupChkbx.setOnCheckedChangeListener { _, checked -> editor.putBoolean("pin_dock", checked).apply() }
        windowedChkbx.setOnCheckedChangeListener { _, checked -> editor.putBoolean("auto_pin", checked).commit() }
        fullscreenChkbx.setOnCheckedChangeListener { _, checked -> editor.putBoolean("auto_unpin", checked).commit() }
        dialogBuilder.setView(view)
        dialogBuilder.setPositiveButton(R.string.ok, null)
        dialogBuilder.show()
    }
}
