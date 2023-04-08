package cu.axel.smartdock.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import cu.axel.smartdock.R;
import androidx.preference.PreferenceManager;

public class SoundsPreferences extends PreferenceFragmentCompat {

	private Preference startSoundPref, usbSoundPref, notifSoundPref, chargeSoundPref, chargeCompleteSoundPref;

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

			if (requestCode == 0) {
				saveSound(startSoundPref.getKey(), openUri.toString());
			} else if (requestCode == 1) {
				saveSound(usbSoundPref.getKey(), openUri.toString());
			} else if (requestCode == 2) {
				saveSound(notifSoundPref.getKey(), openUri.toString());
			} else if (requestCode == 3) {
				saveSound(chargeSoundPref.getKey(), openUri.toString());
			} else if (requestCode == 4) {
				saveSound(chargeCompleteSoundPref.getKey(), openUri.toString());
			}
		}

	}

	public void openSound(int code) {
		startActivityForResult(
				new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("audio/*"), code);
	}

	public void saveSound(String key, String uri) {
		PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString(key, uri).commit();
	}
}
