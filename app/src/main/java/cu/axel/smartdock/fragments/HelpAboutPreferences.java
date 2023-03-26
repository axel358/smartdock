package cu.axel.smartdock.fragments;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import cu.axel.smartdock.R;
import androidx.preference.Preference;
import android.content.Intent;
import android.net.Uri;
import android.content.ActivityNotFoundException;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class HelpAboutPreferences extends PreferenceFragmentCompat {

	@Override
	public void onCreatePreferences(Bundle arg0, String arg1) {
		setPreferencesFromResource(R.xml.preferences_help_about, arg1);

		findPreference("join_telegram").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference p1) {
				try {
					startActivity(
							new Intent(Intent.ACTION_VIEW).setData(Uri.parse("tg://resolve?domain=smartdock358")));
				} catch (ActivityNotFoundException e) {
				}
				return false;
			}
		});

		findPreference("show_help").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference p1) {
				AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
				dialog.setTitle(R.string.help);
				dialog.setView(R.layout.dialog_help);
				dialog.setPositiveButton(R.string.ok, null);
				dialog.setNegativeButton(R.string.more_help, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface p1, int p2) {
						startActivity(
								new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/axel358/smartdock")));
					}
				});
				dialog.show();
				return false;
			}
		});

	}

}
