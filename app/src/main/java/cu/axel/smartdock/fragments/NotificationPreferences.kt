package cu.axel.smartdock.fragments

import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import cu.axel.smartdock.R

class NotificationPreferences : PreferenceFragmentCompat() {
    override fun onCreatePreferences(arg0: Bundle?, arg1: String?) {
        setPreferencesFromResource(R.xml.preferences_notification, arg1)

        val notificationTimeout: EditTextPreference = findPreference("notification_display_time")!!
        notificationTimeout.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.imeOptions = EditorInfo.IME_ACTION_GO
        }

        notificationTimeout.setOnPreferenceChangeListener { _, newValue ->
            val value = newValue as String
            value.isNotEmpty() && value.toInt() > 0
        }
    }
}
