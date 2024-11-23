package cu.axel.smartdock.fragments

import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import cu.axel.smartdock.R

class HotCornersPreferences : PreferenceFragmentCompat() {
    override fun onCreatePreferences(arg0: Bundle?, arg1: String?) {
        setPreferencesFromResource(R.xml.preferences_hot_corners, arg1)

        val activationDelay: EditTextPreference = findPreference("hot_corners_delay")!!
        activationDelay.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.imeOptions = EditorInfo.IME_ACTION_GO
        }
    }
}
