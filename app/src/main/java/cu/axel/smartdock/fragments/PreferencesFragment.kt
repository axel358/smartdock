package cu.axel.smartdock.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import cu.axel.smartdock.R

class PreferencesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(arg0: Bundle?, arg1: String?) {
        setPreferencesFromResource(R.xml.preferences_main, arg1)
    }
}