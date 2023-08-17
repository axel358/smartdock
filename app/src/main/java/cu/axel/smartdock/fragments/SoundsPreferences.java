package cu.axel.smartdock.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import cu.axel.smartdock.R;
import androidx.preference.PreferenceManager;
import cu.axel.smartdock.preferences.FileChooserPreference;

public class SoundsPreferences extends PreferenceFragmentCompat {

	private FileChooserPreference startSoundPref, usbSoundPref, notifSoundPref, chargeSoundPref,
			chargeCompleteSoundPref;

	@Override
	public void onCreatePreferences(Bundle arg0, String arg1) {
		setPreferencesFromResource(R.xml.preferences_sounds, arg1);

		startSoundPref = findPreference("startup_sound");
		startSoundPref.setOnPreferenceClickListener((Preference p1) -> {
			openSound(0);
			return false;
		});
		usbSoundPref = findPreference("usb_sound");
		usbSoundPref.setOnPreferenceClickListener((Preference p1) -> {
			openSound(1);
			return false;
		});
		notifSoundPref = findPreference("notification_sound");
		notifSoundPref.setOnPreferenceClickListener((Preference p1) -> {
			openSound(2);
			return false;
		});
		chargeSoundPref = findPreference("charge_sound");
		chargeSoundPref.setOnPreferenceClickListener((Preference p1) -> {
			openSound(3);
			return false;
		});
		chargeCompleteSoundPref = findPreference("charge_complete_sound");
		chargeCompleteSoundPref.setOnPreferenceClickListener((Preference p1) -> {
			openSound(4);
			return false;
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			Uri openUri = data.getData();
			getActivity().getContentResolver().takePersistableUriPermission(openUri,
					Intent.FLAG_GRANT_READ_URI_PERMISSION);
			String file = openUri.toString();
			if (requestCode == 0) {
				startSoundPref.setFile(file);
			} else if (requestCode == 1) {
				usbSoundPref.setFile(file);
			} else if (requestCode == 2) {
				notifSoundPref.setFile(file);
			} else if (requestCode == 3) {
				chargeSoundPref.setFile(file);
			} else if (requestCode == 4) {
				chargeCompleteSoundPref.setFile(file);
			}
		}

	}

	public void openSound(int code) {
		startActivityForResult(
				new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("audio/*"), code);
	}
}
