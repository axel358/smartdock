package cu.axel.smartdock.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import cu.axel.smartdock.R

class HelpAboutPreferences : PreferenceFragmentCompat() {
    override fun onCreatePreferences(arg0: Bundle?, arg1: String?) {
        setPreferencesFromResource(R.xml.preferences_help_about, arg1)
        findPreference<Preference>("join_telegram")!!.setOnPreferenceClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW).setData("tg://resolve?domain=smartdock358".toUri()))
            } catch (_: ActivityNotFoundException) {
            }
            false
        }
        findPreference<Preference>("show_help")!!.setOnPreferenceClickListener {
            val dialog = MaterialAlertDialogBuilder(requireContext())
            dialog.setTitle(R.string.help)
            dialog.setView(R.layout.dialog_help)
            dialog.setPositiveButton(R.string.ok, null)
            dialog.setNegativeButton(R.string.more_help) { _, _ -> startActivity(Intent(Intent.ACTION_VIEW,
                "https://github.com/axel358/smartdock".toUri())) }
            dialog.show()
            false
        }
    }
}
