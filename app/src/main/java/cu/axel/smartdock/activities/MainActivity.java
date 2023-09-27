package cu.axel.smartdock.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import cu.axel.smartdock.R;
import cu.axel.smartdock.fragments.PreferencesFragment;
import cu.axel.smartdock.services.NotificationService;
import cu.axel.smartdock.utils.ColorUtils;
import cu.axel.smartdock.utils.DeviceUtils;

public class MainActivity extends AppCompatActivity {
	private SharedPreferences sp;
	private AlertDialog permissionsDialog;
	private MaterialButton overlayBtn, storageBtn, adminBtn, notificationsBtn, accessibilityBtn, locationBtn, usageBtn,
			secureBtn;
	private boolean canDrawOverOtherApps, hasStoragePermission, isdeviceAdminEnabled, hasLocationPermission,
			hasWriteSettingsPermission;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		getSupportFragmentManager().beginTransaction().replace(R.id.settings_container, new PreferencesFragment())
				.commit();

		if (!DeviceUtils.hasStoragePermission(this)) {
			DeviceUtils.requestStoragePermissions(this);
		}

		if (!DeviceUtils.canDrawOverOtherApps(this) || !DeviceUtils.isAccessibilityServiceEnabled(this))
			showPermissionsDialog();

		if (sp.getInt("dock_layout", -1) == -1)
			showDockLayoutsDialog();
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (permissionsDialog != null && permissionsDialog.isShowing()) {
			updatePermissionsStatus();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.action_grant_permissions:
			showPermissionsDialog();
			break;
		case R.id.action_switch_layout:
			showDockLayoutsDialog();
		}
		return super.onOptionsItemSelected(item);
	}

	public void showPermissionsDialog() {
		final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
		builder.setTitle(R.string.manage_permissions);
		final View view = getLayoutInflater().inflate(R.layout.dialog_permissions, null);
		final ViewSwitcher viewSwitcher = view.findViewById(R.id.permissions_view_switcher);
		final Button requiredBtn = view.findViewById(R.id.show_required_button);
		final Button optionalBtn = view.findViewById(R.id.show_optional_button);

		overlayBtn = view.findViewById(R.id.btn_grant_overlay);
		storageBtn = view.findViewById(R.id.btn_grant_storage);
		adminBtn = view.findViewById(R.id.btn_grant_admin);
		notificationsBtn = view.findViewById(R.id.btn_grant_notifications);
		accessibilityBtn = view.findViewById(R.id.btn_manage_service);
		locationBtn = view.findViewById(R.id.btn_grant_location);
		usageBtn = view.findViewById(R.id.btn_manage_usage);
		secureBtn = view.findViewById(R.id.btn_manage_secure);

		builder.setView(view);
		permissionsDialog = builder.create();

		overlayBtn.setOnClickListener(v -> {
			showPermissionInfoDialog(R.string.display_over_other_apps, R.string.display_over_other_apps_desc,
					() -> DeviceUtils.grantOverlayPermissions(this), canDrawOverOtherApps);
		});

		storageBtn.setOnClickListener(v -> {
			showPermissionInfoDialog(R.string.storage, R.string.storage_desc,
					() -> DeviceUtils.requestStoragePermissions(this), hasStoragePermission);
		});

		adminBtn.setOnClickListener(v -> {
			showPermissionInfoDialog(R.string.device_administrator, R.string.device_administrator_desc,
					() -> DeviceUtils.requestDeviceAdminPermissions(this), isdeviceAdminEnabled);
		});

		notificationsBtn.setOnClickListener(v -> {
			showNotificationsDialog();
		});

		accessibilityBtn.setOnClickListener(v -> {
			showAccessibilityDialog();
		});

		locationBtn.setOnClickListener(v -> {
			showPermissionInfoDialog(R.string.location, R.string.location_desc,
					() -> DeviceUtils.requestLocationPermissions(this), hasLocationPermission);
		});

		usageBtn.setOnClickListener(v -> {
			showPermissionInfoDialog(R.string.usage_stats, R.string.usage_stats_desc,
					() -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)),
					DeviceUtils.hasUsageStatsPermission(this));
		});

		secureBtn.setOnClickListener(
				v -> showPermissionInfoDialog(R.string.write_secure, R.string.write_secure_desc, null, true));

		requiredBtn.setOnClickListener(v -> {
			viewSwitcher.showPrevious();
		});

		optionalBtn.setOnClickListener(v -> {
			viewSwitcher.showNext();
		});

		updatePermissionsStatus();
		permissionsDialog.show();
	}

	public void showDockLayoutsDialog() {
		SharedPreferences.Editor editor = sp.edit();
		MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
		dialog.setTitle(R.string.choose_dock_layout);
		int layout = sp.getInt("dock_layout", -1);
		dialog.setSingleChoiceItems(R.array.layouts, layout, (i, wich) -> {
			editor.putBoolean("enable_nav_back", wich != 0);
			editor.putBoolean("enable_nav_home", wich != 0);
			editor.putBoolean("enable_nav_recents", wich != 0);
			editor.putBoolean("enable_qs_wifi", wich != 0);
			editor.putBoolean("enable_qs_vol", wich != 0);
			editor.putBoolean("enable_qs_date", wich != 0);
			editor.putBoolean("enable_qs_notif", wich != 0);
			editor.putBoolean("app_menu_fullscreen", wich != 2);
			editor.putString("launch_mode", wich != 2 ? "fullscreen" : "standard");
			editor.putString("max_running_apps", wich == 0 ? "4" : "10");
			editor.putString("dock_activation_area", wich == 2 ? "5" : "25");
			editor.putInt("dock_layout", wich);
			editor.putString("activation_method", wich != 2 ? "handle" : "swipe");
			editor.putBoolean("show_notifications", wich != 0);
			editor.commit();
		});

		dialog.setPositiveButton(R.string.ok, null);
		dialog.show();
	}

	private void updatePermissionsStatus() {
		canDrawOverOtherApps = DeviceUtils.canDrawOverOtherApps(this);
		accessibilityBtn.setEnabled(canDrawOverOtherApps);

		if (canDrawOverOtherApps) {
			overlayBtn.setIconResource(R.drawable.ic_granted);
			overlayBtn.setIconTint(ColorStateList.valueOf(ColorUtils.getThemeColors(this, false)[0]));
		}

		if (DeviceUtils.isAccessibilityServiceEnabled(this)) {
			accessibilityBtn.setIconResource(R.drawable.ic_settings);
			accessibilityBtn.setIconTint(ColorStateList.valueOf(ColorUtils.getThemeColors(this, false)[0]));
		} else {
			accessibilityBtn.setIconResource(R.drawable.ic_alert);
			accessibilityBtn.setIconTint(ColorStateList.valueOf(ColorUtils.getThemeColors(this, false)[2]));
		}

		if (DeviceUtils.hasUsageStatsPermission(this)) {
			usageBtn.setIconResource(R.drawable.ic_granted);
			usageBtn.setIconTint(ColorStateList.valueOf(ColorUtils.getThemeColors(this, false)[0]));
		}

		if (DeviceUtils.isServiceRunning(this, NotificationService.class)) {
			notificationsBtn.setIconResource(R.drawable.ic_settings);
			notificationsBtn.setIconTint(ColorStateList.valueOf(ColorUtils.getThemeColors(this, false)[0]));
		}

		isdeviceAdminEnabled = DeviceUtils.isdeviceAdminEnabled(this);
		if (isdeviceAdminEnabled) {
			adminBtn.setIconResource(R.drawable.ic_granted);
			adminBtn.setIconTint(ColorStateList.valueOf(ColorUtils.getThemeColors(this, false)[0]));
		}

		hasStoragePermission = DeviceUtils.hasStoragePermission(this);
		if (hasStoragePermission) {
			storageBtn.setIconResource(R.drawable.ic_granted);
			storageBtn.setIconTint(ColorStateList.valueOf(ColorUtils.getThemeColors(this, false)[0]));
		}

		hasLocationPermission = DeviceUtils.hasLocationPermission(this);
		if (hasLocationPermission) {
			locationBtn.setIconResource(R.drawable.ic_granted);
			locationBtn.setIconTint(ColorStateList.valueOf(ColorUtils.getThemeColors(this, false)[0]));
		}

		hasWriteSettingsPermission = DeviceUtils.hasWriteSettingsPermission(this);
		if (hasWriteSettingsPermission) {
			secureBtn.setIconResource(R.drawable.ic_granted);
			secureBtn.setIconTint(ColorStateList.valueOf(ColorUtils.getThemeColors(this, false)[0]));
		}
	}

	private void showPermissionInfoDialog(int permission, int description, Method grantMethod, boolean granted) {
		MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
		dialogBuilder.setTitle(permission);
		dialogBuilder.setMessage(description);
		if (!granted)
			dialogBuilder.setPositiveButton(R.string.grant, (i, p) -> grantMethod.run());
		else
			dialogBuilder.setPositiveButton(R.string.ok, null);
		dialogBuilder.show();
	}

	private void showAccessibilityDialog() {
		MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
		dialogBuilder.setTitle(R.string.accessibility_service);
		dialogBuilder.setMessage(R.string.accessibility_service_desc);

		if (DeviceUtils.hasWriteSettingsPermission(this)) {
			dialogBuilder.setPositiveButton(R.string.enable, (i, p) -> {
				DeviceUtils.enableService(this);
				new Handler(getMainLooper()).postDelayed(() -> {
					updatePermissionsStatus();
				}, 500);

			});
			dialogBuilder.setNegativeButton(R.string.disable, (i, p) -> {
				DeviceUtils.disableService(this);
				new Handler(getMainLooper()).postDelayed(() -> {
					updatePermissionsStatus();
				}, 500);
			});
		} else {
			dialogBuilder.setPositiveButton(R.string.manage, (i, p) -> {
				startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
				Toast.makeText(this, R.string.enable_access_help, Toast.LENGTH_LONG).show();
			});
		}

		dialogBuilder.setNeutralButton(R.string.help, (i, p) -> startActivity(new Intent(Intent.ACTION_VIEW,
				Uri.parse("https://github.com/axel358/smartdock#grant-restricted-permissions"))));

		dialogBuilder.show();
	}

	private void showNotificationsDialog() {
		MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
		dialogBuilder.setTitle(R.string.notification_access);
		dialogBuilder.setMessage(R.string.notification_access_desc);

		dialogBuilder.setPositiveButton(R.string.manage, (i, p) -> {
			startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
			Toast.makeText(this, R.string.enable_access_help, Toast.LENGTH_LONG).show();
		});

		dialogBuilder.setNeutralButton(R.string.help, (i, p) -> startActivity(new Intent(Intent.ACTION_VIEW,
				Uri.parse("https://github.com/axel358/smartdock#grant-restricted-permissions"))));

		dialogBuilder.show();
	}

	interface Method {
		void run();
	}
}
