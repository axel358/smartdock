package cu.axel.smartdock.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.preference.Preference;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import androidx.preference.PreferenceFragmentCompat;
import cu.axel.smartdock.R;
import cu.axel.smartdock.utils.DeviceUtils;
import cu.axel.smartdock.utils.Utils;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManager;
import android.widget.Toast;
import androidx.preference.CheckBoxPreference;
import cu.axel.smartdock.utils.AppUtils;

public class AdvancedPreferences extends PreferenceFragmentCompat {

	@Override
	public void onCreatePreferences(Bundle arg0, String arg1) {
		setPreferencesFromResource(R.xml.preferences_advanced, arg1);
		Preference editAutostart = findPreference("edit_autostart");
		editAutostart.setOnPreferenceClickListener((Preference p1) -> {
			showEditAutostartDialog(getActivity());
			return false;
		});
		Preference displayS = findPreference("custom_display_size");
		displayS.setOnPreferenceChangeListener((Preference p1, Object p2) -> {
			String n = p2.toString();
			if (n.isEmpty())
				DeviceUtils.setDisplaySize(0);
			else
				DeviceUtils.setDisplaySize(Integer.parseInt(n));

			showRebootDialog(getActivity(), true);
			return true;
		});
		Preference softReboot = findPreference("soft_reboot");
		softReboot.setOnPreferenceClickListener((Preference p1) -> {
			DeviceUtils.sotfReboot();
			return false;
		});

		Preference moveToSystem = findPreference("move_to_system");
		moveToSystem.setEnabled(!AppUtils.isSystemApp(getActivity(), getActivity().getPackageName()));
		moveToSystem.setOnPreferenceClickListener((Preference p1) -> {
			try {
				ApplicationInfo appInfo = getActivity().getPackageManager()
						.getApplicationInfo(getActivity().getPackageName(), 0);
				String appDir = appInfo.sourceDir.substring(0, appInfo.sourceDir.lastIndexOf("/"));
				DeviceUtils.runAsRoot("mv " + appDir + " /system/priv-app/");
				DeviceUtils.reboot();
			} catch (PackageManager.NameNotFoundException e) {
			}
			return false;
		});

		CheckBoxPreference hideNav = (CheckBoxPreference) findPreference("hide_nav_buttons");
		hideNav.setChecked(DeviceUtils.runAsRoot("cat /system/build.prop").contains("qemu.hw.mainkeys=1"));
		hideNav.setOnPreferenceChangeListener((Preference p1, Object p2) -> {
			if ((boolean) p2) {
				String status = DeviceUtils.runAsRoot("echo qemu.hw.mainkeys=1 >> /system/build.prop");
				if (!status.equals("error")) {
					showRebootDialog(getActivity(), false);
					return true;
				}
			} else {
				String status = DeviceUtils.runAsRoot("sed -i /qemu.hw.mainkeys=1/d /system/build.prop");
				if (!status.equals("error")) {
					showRebootDialog(getActivity(), false);
					return true;
				}
			}
			return false;
		});
		CheckBoxPreference hideStatus = (CheckBoxPreference) findPreference("hide_status_bar");
		hideStatus.setChecked(
				DeviceUtils.runAsRoot("settings get global policy_control").contains("immersive.status=apps"));
		hideStatus.setOnPreferenceChangeListener((Preference p1, Object p2) -> {
			if ((boolean) p2) {
				String status = DeviceUtils.runAsRoot("settings put global policy_control immersive.status=apps");
				if (!status.equals("error")) {
					showRebootDialog(getActivity(), true);
					return true;
				}
			} else {
				String status = DeviceUtils.runAsRoot("settings delete global policy_control");
				if (!status.equals("error")) {
					showRebootDialog(getActivity(), true);
					return true;
				}
			}
			return false;
		});
		findPreference("status_icon_blacklist").setOnPreferenceClickListener((Preference p1) -> {
			showIBDialog(getActivity());
			return false;
		});
	}

	public void showEditAutostartDialog(final Context context) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle(getString(R.string.edit_autostart));
		View view = LayoutInflater.from(context).inflate(R.layout.dialog_edit_autostart, null);
		final EditText contentEt = view.findViewById(R.id.edit_autostart_et);
		contentEt.setText(Utils.readAutostart(context));
		dialog.setPositiveButton(getString(R.string.save), (DialogInterface p1, int p2) -> {
			String content = contentEt.getText().toString();
			if (!content.isEmpty()) {
				Utils.saveAutoStart(context, content);
			}
		});
		dialog.setNegativeButton(getString(R.string.cancel), null);
		dialog.setView(view);
		dialog.show();
	}

	public void showIBDialog(final Context context) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle("Blacklisted icons");
		View view = LayoutInflater.from(context).inflate(R.layout.dialog_icon_blacklist, null);
		final EditText contentEt = view.findViewById(R.id.icon_blacklist_et);
		contentEt.setText(DeviceUtils.runAsRoot("settings get secure icon_blacklist").replace("\n", ""));
		dialog.setPositiveButton(getString(R.string.save), (DialogInterface p1, int p2) -> {
			DeviceUtils.runAsRoot("settings put secure icon_blacklist " + contentEt.getText());
		});
		dialog.setNegativeButton(getString(R.string.cancel), null);
		dialog.setView(view);
		dialog.show();
	}

	public void showRebootDialog(Context context, final boolean softReboot) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle(getString(R.string.reboot_required_title));
		dialog.setMessage(getString(R.string.reboot_required_text));
		dialog.setPositiveButton(getString(R.string.ok), (DialogInterface p1, int p2) -> {
			if (softReboot)
				DeviceUtils.sotfReboot();
			else
				DeviceUtils.reboot();
		});
		dialog.setNegativeButton(getString(R.string.cancel), null);
		dialog.show();
	}
}
