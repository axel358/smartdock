package cu.axel.smartdock.fragments;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import cu.axel.smartdock.R;

public class HelpAboutPreferences extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle arg0, String arg1) {
        setPreferencesFromResource(R.xml.preferences_help_about, arg1);

        findPreference("join_telegram").setOnPreferenceClickListener((Preference p1) -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("tg://resolve?domain=smartdock358")));
            } catch (ActivityNotFoundException e) {
            }
            return false;
        });

        findPreference("show_help").setOnPreferenceClickListener((Preference p1) -> {
            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(getActivity());
            dialog.setTitle(R.string.help);
            dialog.setView(R.layout.dialog_help);
            dialog.setPositiveButton(R.string.ok, null);
            dialog.setNegativeButton(R.string.more_help, (DialogInterface d1, int p2) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/axel358/smartdock"))));
            dialog.show();
            return false;
        });

    }

}
