package cu.axel.smartdock.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import cu.axel.smartdock.R;

public class AppMenuPreferences extends PreferenceFragmentCompat {
	private Preference menuIconPref;
	private final int OPEN_REQUEST_CODE = 4;

	@Override
	public void onCreatePreferences(Bundle arg0, String arg1) {
		setPreferencesFromResource(R.xml.preferences_app_menu, arg1);

		menuIconPref = findPreference("menu_icon_uri");
		menuIconPref.setOnPreferenceClickListener((Preference p1) -> {
			startActivityForResult(
					new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*"),
					OPEN_REQUEST_CODE);

			return false;
		});
		findPreference("restore_menu_icon").setOnPreferenceClickListener((Preference p1) -> {
			menuIconPref.getSharedPreferences().edit().putString(menuIconPref.getKey(), "default").commit();
			return false;
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == OPEN_REQUEST_CODE) {
				Uri openUri = data.getData();
				getActivity().getContentResolver().takePersistableUriPermission(openUri,
						Intent.FLAG_GRANT_READ_URI_PERMISSION);
				menuIconPref.getSharedPreferences().edit().putString(menuIconPref.getKey(), openUri.toString())
						.commit();
			}
		}

	}

}
