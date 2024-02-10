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

        val notificationTimeout: EditTextPreference? = findPreference("notification_timeout")
        notificationTimeout?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.imeOptions = EditorInfo.IME_ACTION_GO
        }

        val ignoredNotifications: EditTextPreference? = findPreference("blocked_notifications")
        ignoredNotifications?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_TEXT
            editText.imeOptions = EditorInfo.IME_ACTION_GO
        }
    }
}
