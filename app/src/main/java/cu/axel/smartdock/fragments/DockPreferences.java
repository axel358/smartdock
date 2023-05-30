package cu.axel.smartdock.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import cu.axel.smartdock.R;

public class DockPreferences extends PreferenceFragmentCompat {
	@Override
	public void onCreatePreferences(Bundle arg0, String arg1) {
		setPreferencesFromResource(R.xml.preferences_dock, arg1);
		findPreference("auto_pin").setOnPreferenceClickListener((Preference preference) -> {
			showAutopinDialog();
			return false;
		});
	}

	public void showAutopinDialog() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
		SharedPreferences.Editor editor = sp.edit();
		MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(getActivity());
		dialogBuilder.setTitle(R.string.auto_pin_summary);
		View view = getLayoutInflater().inflate(R.layout.dialog_auto_pin, null);
		CheckBox startupChkbx = view.findViewById(R.id.pin_startup_chkbx);
		CheckBox windowedChkbx = view.findViewById(R.id.pin_window_chkbx);
		CheckBox fullscreenChkbx = view.findViewById(R.id.unpin_fullscreen_chkbx);

		startupChkbx.setChecked(sp.getBoolean("pin_dock", true));
		windowedChkbx.setChecked(sp.getBoolean("auto_pin", true));
		fullscreenChkbx.setChecked(sp.getBoolean("auto_unpin", true));

		startupChkbx.setOnCheckedChangeListener((CompoundButton arg0, boolean checked) -> {
			editor.putBoolean("pin_dock", checked).commit();
		});

		windowedChkbx.setOnCheckedChangeListener((CompoundButton arg0, boolean checked) -> {
			editor.putBoolean("auto_pin", checked).commit();
		});

		fullscreenChkbx.setOnCheckedChangeListener((CompoundButton arg0, boolean checked) -> {
			editor.putBoolean("auto_unpin", checked).commit();
		});

		dialogBuilder.setView(view);
		dialogBuilder.show();
	}
}
