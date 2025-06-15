package cu.axel.smartdock.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cu.axel.smartdock.R
import cu.axel.smartdock.db.DBHelper
import cu.axel.smartdock.preferences.FileChooserPreference
import cu.axel.smartdock.utils.AppUtils

private const val MENU_REQUEST_CODE = 4
private const val USER_REQUEST_CODE = 5

class AppMenuPreferences : PreferenceFragmentCompat() {
    private lateinit var menuIconPref: FileChooserPreference
    private lateinit var userIconPref: FileChooserPreference
    override fun onCreatePreferences(arg0: Bundle?, arg1: String?) {
        setPreferencesFromResource(R.xml.preferences_app_menu, arg1)
        menuIconPref = findPreference("menu_icon_uri")!!
        menuIconPref.setOnPreferenceClickListener {
            startActivityForResult(
                Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("image/*"),
                MENU_REQUEST_CODE
            )
            false
        }
        userIconPref = findPreference("user_icon_uri")!!
        userIconPref.setOnPreferenceClickListener {
            startActivityForResult(
                Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("image/*"),
                USER_REQUEST_CODE
            )
            false
        }
        userIconPref.isVisible =
            !AppUtils.isSystemApp(requireContext(), requireContext().packageName)
        findPreference<Preference>("user_name")!!.isVisible = userIconPref.isVisible
        val heightPreference = findPreference<Preference>("app_menu_height")
        val widthPreference = findPreference<Preference>("app_menu_width")
        val centerPreference = findPreference<Preference>("center_app_menu")
        val fullscreenPreference = findPreference<Preference>("app_menu_fullscreen")
        val sharedPreferences = fullscreenPreference!!.sharedPreferences
        heightPreference!!.isEnabled =
            !sharedPreferences!!.getBoolean(fullscreenPreference.key, false)
        widthPreference!!.isEnabled = !sharedPreferences.getBoolean(fullscreenPreference.key, false)
        centerPreference!!.isEnabled =
            !sharedPreferences.getBoolean(fullscreenPreference.key, false)
        fullscreenPreference.setOnPreferenceChangeListener { _, newValue ->
            val checked = newValue as Boolean
            heightPreference.isEnabled = !checked
            widthPreference.isEnabled = !checked
            centerPreference.isEnabled = !checked
            true
        }

        val menuHeight: EditTextPreference = findPreference("app_menu_height")!!
        menuHeight.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.imeOptions = EditorInfo.IME_ACTION_GO
        }

        menuHeight.setOnPreferenceChangeListener { _, newValue ->
            val value = newValue as String
            value.isNotEmpty() && value.toInt() > 100
        }

        val menuWidth: EditTextPreference = findPreference("app_menu_width")!!
        menuWidth.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.imeOptions = EditorInfo.IME_ACTION_GO
        }

        menuWidth.setOnPreferenceChangeListener { _, newValue ->
            val value = newValue as String
            value.isNotEmpty() && value.toInt() > 100
        }

        val columns: EditTextPreference = findPreference("num_columns")!!
        columns.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.imeOptions = EditorInfo.IME_ACTION_GO
        }

        columns.setOnPreferenceChangeListener { _, newValue ->
            val value = newValue as String
            value.isNotEmpty() && value.toInt() > 1
        }

        val userName: EditTextPreference? = findPreference("user_name")
        userName?.setOnBindEditTextListener { editText ->
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            editText.imeOptions = EditorInfo.IME_ACTION_GO
        }

        findPreference<Preference>("forget_launch_modes")!!.setOnPreferenceClickListener {
            context?.let { ctx ->
                DBHelper(ctx).forgetLaunchModes()
            }
            false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val openUri = data!!.data
            requireContext().contentResolver.takePersistableUriPermission(
                openUri!!,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            if (requestCode == MENU_REQUEST_CODE) {
                menuIconPref.setFile(openUri.toString())
            } else if (requestCode == USER_REQUEST_CODE) {
                userIconPref.setFile(openUri.toString())
            }
        }
    }
}
