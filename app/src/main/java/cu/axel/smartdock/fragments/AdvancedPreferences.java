package cu.axel.smartdock.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import androidx.preference.PreferenceFragmentCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
	private boolean rootAvailable;

	@Override
	public void onCreatePreferences(Bundle arg0, String arg1) {
		setPreferencesFromResource(R.xml.preferences_advanced, arg1);

		Preference preferLastDisplay = findPreference("prefer_last_display");
		preferLastDisplay.setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);

		preferLastDisplay.setOnPreferenceClickListener((Preference p1) -> {
			if (DeviceUtils.hasWriteSettingsPermission(getActivity()))
				DeviceUtils.restartService(getActivity());
			else
				showAccessibilityDialog(getActivity());
			return true;
		});

		findPreference("custom_display_size").setOnPreferenceClickListener(p -> {
			showDisplaySizeDialog(getActivity());
			return true;
		});

		findPreference("soft_reboot").setOnPreferenceClickListener((Preference p1) -> {
			DeviceUtils.sotfReboot();
			return false;
		});

		Preference moveToSystem = findPreference("move_to_system");
		moveToSystem.setVisible(!AppUtils.isSystemApp(getActivity(), getActivity().getPackageName()));
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

		String result = DeviceUtils.runAsRoot("cat /system/build.prop");

		hideNav.setChecked(result.contains("qemu.hw.mainkeys=1"));

		rootAvailable = !result.equals("error");
		findPreference("root_category").setEnabled(rootAvailable);

		if (rootAvailable)
			DeviceUtils.grantPermission(Manifest.permission.WRITE_SECURE_SETTINGS);

		findPreference("secure_category").setEnabled(DeviceUtils.hasWriteSettingsPermission(getActivity()));

		hideNav.setOnPreferenceChangeListener((Preference p1, Object p2) -> {
			if ((boolean) p2) {
				String status = DeviceUtils.runAsRoot("echo qemu.hw.mainkeys=1 >> /system/build.prop");
				if (!status.equals("error")) {
					hideNav.getSharedPreferences().edit().putBoolean("navbar_fix", false).commit();
					showRebootDialog(getActivity(), false);
					return true;
				}
			} else {
				String status = DeviceUtils.runAsRoot("sed -i /qemu.hw.mainkeys=1/d /system/build.prop");
				if (!status.equals("error")) {
					hideNav.getSharedPreferences().edit().putBoolean("navbar_fix", true).commit();
					showRebootDialog(getActivity(), false);
					return true;
				}
			}
			return false;
		});
		CheckBoxPreference hideStatus = (CheckBoxPreference) findPreference("hide_status_bar");
		hideStatus.setChecked(DeviceUtils.getGlobalSettingString(getActivity(), DeviceUtils.POLICY_CONTROL)
				.equals(DeviceUtils.IMMERSIVE_APPS));

		hideStatus.setOnPreferenceChangeListener((Preference p1, Object p2) -> {
			if ((boolean) p2) {
				if (DeviceUtils.putGlobalSetting(getActivity(), DeviceUtils.POLICY_CONTROL,
						DeviceUtils.IMMERSIVE_APPS)) {
					if (rootAvailable)
						showRebootDialog(getActivity(), true);
					return true;
				}
			} else {
				if (DeviceUtils.putGlobalSetting(getActivity(), DeviceUtils.POLICY_CONTROL, null)) {
					if (rootAvailable)
						showRebootDialog(getActivity(), true);
					return true;
				}
			}
			return false;
		});

		hideStatus.setVisible(Build.VERSION.SDK_INT < 31);

		findPreference("status_icon_blacklist").setOnPreferenceClickListener((Preference p1) -> {
			showIBDialog(getActivity());
			return false;
		});

		findPreference("navbar_fix").setVisible(Build.VERSION.SDK_INT > 31);
	}

	public void showDisplaySizeDialog(final Context context) {
		MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
		dialog.setTitle(R.string.custom_display_size_title);
		View view = LayoutInflater.from(context).inflate(R.layout.dialog_display_size, null);
		final EditText contentEt = view.findViewById(R.id.display_size_et);
		contentEt.setText(DeviceUtils.getSecureSettingString(context, DeviceUtils.DISPLAY_SIZE) + "");
		dialog.setPositiveButton(R.string.ok, (DialogInterface p1, int p2) -> {
			String value = contentEt.getText().toString();
			int size = value.isEmpty() ? 0 : Integer.parseInt(value);

			if (DeviceUtils.putSecureSetting(getActivity(), DeviceUtils.DISPLAY_SIZE, size) && rootAvailable)
				showRebootDialog(getActivity(), true);
		});
		dialog.setNegativeButton(getString(R.string.cancel), null);
		dialog.setView(view);
		dialog.show();
	}

	public void showIBDialog(final Context context) {
		MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
		dialog.setTitle(R.string.icon_blacklist);
		View view = LayoutInflater.from(context).inflate(R.layout.dialog_icon_blacklist, null);
		final EditText contentEt = view.findViewById(R.id.icon_blacklist_et);
		contentEt.setText(DeviceUtils.getSecureSettingString(context, DeviceUtils.ICON_BLACKLIST));
		dialog.setPositiveButton(R.string.ok, (DialogInterface p1, int p2) -> {
			DeviceUtils.putSecureSetting(context, DeviceUtils.ICON_BLACKLIST, contentEt.getText().toString());
		});
		dialog.setNegativeButton(getString(R.string.cancel), null);
		dialog.setView(view);
		dialog.show();
	}

	public void showRebootDialog(Context context, final boolean softReboot) {
		MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
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

	public void showAccessibilityDialog(final Context context) {
		MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(context);
		dialog.setTitle(R.string.restart);
		dialog.setMessage(R.string.restart_accessibility);
		dialog.setNegativeButton(getString(R.string.cancel), null);
		dialog.setPositiveButton(getString(R.string.open_accessibility), (DialogInterface p1, int p2) -> {
			startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
		});
		dialog.show();
	}
}
