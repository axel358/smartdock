package cu.axel.smartdock.fragments
import androidx.preference.PreferenceFragmentCompat;

class PreferencesFragment: PreferenceFragmentCompat() {
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_main, rootKey)
    }
}