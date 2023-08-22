package cu.axel.smartdock.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import cu.axel.smartdock.R;
import cu.axel.smartdock.preferences.FileChooserPreference;
import cu.axel.smartdock.utils.AppUtils;

public class AppMenuPreferences extends PreferenceFragmentCompat {
	private FileChooserPreference menuIconPref, userIconPref;
	private final int MENU_REQUEST_CODE = 4;
	private final int USER_REQUEST_CODE = 5;

	@Override
	public void onCreatePreferences(Bundle arg0, String arg1) {
		setPreferencesFromResource(R.xml.preferences_app_menu, arg1);

		menuIconPref = findPreference("menu_icon_uri");
		menuIconPref.setOnPreferenceClickListener((Preference p1) -> {
			startActivityForResult(
					new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*"),
					MENU_REQUEST_CODE);

			return false;
		});

		userIconPref = findPreference("user_icon_uri");
		userIconPref.setOnPreferenceClickListener((Preference p1) -> {
			startActivityForResult(
					new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*"),
					USER_REQUEST_CODE);

			return false;
		});

		userIconPref.setVisible(!AppUtils.isSystemApp(getActivity(), getActivity().getPackageName()));
		findPreference("user_name").setVisible(userIconPref.isVisible());

		final Preference heightPreference = findPreference("app_menu_height");
		final Preference widthPreference = findPreference("app_menu_width");
		final Preference centerPreference = findPreference("center_app_menu");
		final Preference fullscreenPreference = findPreference("app_menu_fullscreen");
		SharedPreferences sp = fullscreenPreference.getSharedPreferences();
		heightPreference.setEnabled(!sp.getBoolean(fullscreenPreference.getKey(), false));
		widthPreference.setEnabled(!sp.getBoolean(fullscreenPreference.getKey(), false));
		centerPreference.setEnabled(!sp.getBoolean(fullscreenPreference.getKey(), false));

		fullscreenPreference.setOnPreferenceChangeListener((Preference p0, Object value) -> {
			boolean checked = (boolean) value;
			heightPreference.setEnabled(!checked);
			widthPreference.setEnabled(!checked);
			centerPreference.setEnabled(!checked);
			return true;
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			Uri openUri = data.getData();
			getActivity().getContentResolver().takePersistableUriPermission(openUri,
					Intent.FLAG_GRANT_READ_URI_PERMISSION);

			if (requestCode == MENU_REQUEST_CODE) {
				menuIconPref.setFile(openUri.toString());
			} else if (requestCode == USER_REQUEST_CODE) {
				userIconPref.setFile(openUri.toString());
			}
		}

	}

}
