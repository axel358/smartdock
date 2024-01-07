package cu.axel.smartdock.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import cu.axel.smartdock.R
import cu.axel.smartdock.preferences.FileChooserPreference

class SoundsPreferences : PreferenceFragmentCompat() {
    private lateinit var startSoundPref: FileChooserPreference
    private lateinit var usbSoundPref: FileChooserPreference
    private lateinit var notifSoundPref: FileChooserPreference
    private lateinit var chargeSoundPref: FileChooserPreference
    private lateinit var chargeCompleteSoundPref: FileChooserPreference
    override fun onCreatePreferences(arg0: Bundle?, arg1: String?) {
        setPreferencesFromResource(R.xml.preferences_sounds, arg1)
        startSoundPref = findPreference("startup_sound")!!
        startSoundPref.setOnPreferenceClickListener {
            openSound(0)
            false
        }
        usbSoundPref = findPreference("usb_sound")!!
        usbSoundPref.setOnPreferenceClickListener {
            openSound(1)
            false
        }
        notifSoundPref = findPreference("notification_sound")!!
        notifSoundPref.setOnPreferenceClickListener {
            openSound(2)
            false
        }
        chargeSoundPref = findPreference("charge_sound")!!
        chargeSoundPref.setOnPreferenceClickListener {
            openSound(3)
            false
        }
        chargeCompleteSoundPref = findPreference("charge_complete_sound")!!
        chargeCompleteSoundPref.setOnPreferenceClickListener {
            openSound(4)
            false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val openUri = data!!.data
            requireContext().contentResolver.takePersistableUriPermission(openUri!!,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val file = openUri.toString()
            when (requestCode) {
                0 -> startSoundPref.setFile(file)
                1 -> usbSoundPref.setFile(file)
                2 -> notifSoundPref.setFile(file)
                3 -> chargeSoundPref.setFile(file)
                4 -> chargeCompleteSoundPref.setFile(file)
            }
        }
    }

    private fun openSound(code: Int) {
        startActivityForResult(
                Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("audio/*"), code)
    }
}
