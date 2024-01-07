package cu.axel.smartdock.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import cu.axel.smartdock.R
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
                    Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*"),
                    MENU_REQUEST_CODE)
            false
        }
        userIconPref = findPreference("user_icon_uri")!!
        userIconPref.setOnPreferenceClickListener {
            startActivityForResult(
                    Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*"),
                    USER_REQUEST_CODE)
            false
        }
        userIconPref.isVisible = !AppUtils.isSystemApp(requireContext(), requireContext().packageName)
        findPreference<Preference>("user_name")!!.isVisible = userIconPref.isVisible
        val heightPreference = findPreference<Preference>("app_menu_height")
        val widthPreference = findPreference<Preference>("app_menu_width")
        val centerPreference = findPreference<Preference>("center_app_menu")
        val fullscreenPreference = findPreference<Preference>("app_menu_fullscreen")
        val sp = fullscreenPreference!!.sharedPreferences
        heightPreference!!.isEnabled = !sp!!.getBoolean(fullscreenPreference.key, false)
        widthPreference!!.isEnabled = !sp.getBoolean(fullscreenPreference.key, false)
        centerPreference!!.isEnabled = !sp.getBoolean(fullscreenPreference.key, false)
        fullscreenPreference.setOnPreferenceChangeListener { _, newValue ->
            val checked = newValue as Boolean
            heightPreference.isEnabled = !checked
            widthPreference.isEnabled = !checked
            centerPreference.isEnabled = !checked
            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val openUri = data!!.data
            requireContext().contentResolver.takePersistableUriPermission(openUri!!,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (requestCode == MENU_REQUEST_CODE) {
                menuIconPref.setFile(openUri.toString())
            } else if (requestCode == USER_REQUEST_CODE) {
                userIconPref.setFile(openUri.toString())
            }
        }
    }
}
