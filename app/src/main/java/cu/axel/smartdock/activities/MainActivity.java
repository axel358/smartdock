package cu.axel.smartdock.activities;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.accessibility.AccessibilityManager;
import android.widget.ViewSwitcher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import cu.axel.smartdock.fragments.PreferencesFragment;
import java.util.List;
import android.os.Build;
import cu.axel.smartdock.services.DockService;
import cu.axel.smartdock.R;
import cu.axel.smartdock.utils.DeviceUtils;
import android.view.View;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
	private SharedPreferences sp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		sp = PreferenceManager.getDefaultSharedPreferences(this);
		getSupportFragmentManager().beginTransaction().replace(R.id.settings_container, new PreferencesFragment())
				.commit();

		if (!DeviceUtils.hasStoragePermission(this)) {
			requestStoragePermission(0);
		}

		if (!canDrawOverOtherApps() || !DeviceUtils.isAccessibilityServiceEnabled(this))
			showPermissionsDialog();

		if (sp.getInt("dock_layout", -1) == -1)
			showDockLayoutsDialog();
	}

	public void requestStoragePermission(int code) {
		ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, code);
	}

	@Override
	protected void onResume() {
		super.onResume();
		invalidateOptionsMenu();
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

		final Button grantOverlayBtn = view.findViewById(R.id.btn_grant_overlay);
		final Button grantStorageBtn = view.findViewById(R.id.btn_grant_storage);
		final Button grantAdminBtn = view.findViewById(R.id.btn_grant_admin);
		final Button grantNotificationsBtn = view.findViewById(R.id.btn_grant_notifications);
		final Button manageServiceBtn = view.findViewById(R.id.btn_manage_service);
		final Button locationBtn = view.findViewById(R.id.btn_grant_location);
		final Button usageBtn = view.findViewById(R.id.btn_manage_usage);

		manageServiceBtn.setEnabled(canDrawOverOtherApps());

		if (canDrawOverOtherApps()) {
			grantOverlayBtn.setEnabled(false);
			grantOverlayBtn.setText(R.string.granted);
		}
		if (isdeviceAdminEnabled()) {
			grantAdminBtn.setEnabled(false);
			grantAdminBtn.setText(R.string.granted);
		}

		if (DeviceUtils.hasStoragePermission(this)) {
			grantStorageBtn.setEnabled(false);
			grantStorageBtn.setText(R.string.granted);
		}

		if (DeviceUtils.hasLocationPermission(this)) {
			locationBtn.setEnabled(false);
			locationBtn.setText(R.string.granted);
		}

		builder.setView(view);
		final AlertDialog dialog = builder.create();

		grantOverlayBtn.setOnClickListener((View p1) -> {
			grantOverlayPermissions();
			dialog.dismiss();
		});
		grantStorageBtn.setOnClickListener((View p1) -> {
			requestStoragePermission(8);
			dialog.dismiss();
		});
		grantAdminBtn.setOnClickListener((View p1) -> {
			enableDeviceAdmin();
			dialog.dismiss();
		});
		grantNotificationsBtn.setOnClickListener((View p1) -> {
			startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
		});

		manageServiceBtn.setOnClickListener((View p1) -> {
			startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
			Toast.makeText(this, R.string.enable_access_help, Toast.LENGTH_LONG).show();
		});

		locationBtn.setOnClickListener((View p1) -> {
			ActivityCompat.requestPermissions(MainActivity.this,
					new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 8);
			dialog.dismiss();
		});

		usageBtn.setOnClickListener((View p1) -> {
			startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
		});

		requiredBtn.setOnClickListener((View p1) -> {
			viewSwitcher.showPrevious();
		});

		optionalBtn.setOnClickListener((View p1) -> {
			viewSwitcher.showNext();
		});

		view.findViewById(R.id.overlay_info_btn)
				.setOnClickListener((View v) -> showPermissionInfoDialog(R.string.display_over_other_apps,
						R.string.display_over_other_apps_desc));

		view.findViewById(R.id.accessibility_info_btn)
				.setOnClickListener((View v) -> showPermissionInfoDialog(R.string.accessibility_service,
						R.string.accessibility_service_desc));

		view.findViewById(R.id.stats_info_btn).setOnClickListener(
				(View v) -> showPermissionInfoDialog(R.string.usage_stats, R.string.usage_stats_desc));

		view.findViewById(R.id.notification_info_btn).setOnClickListener(
				(View v) -> showPermissionInfoDialog(R.string.notification_access, R.string.notification_access_desc));

		view.findViewById(R.id.admin_info_btn)
				.setOnClickListener((View v) -> showPermissionInfoDialog(R.string.device_administrator,
						R.string.device_administrator_desc));

		view.findViewById(R.id.storage_info_btn)
				.setOnClickListener((View v) -> showPermissionInfoDialog(R.string.storage, R.string.storage_desc));

		view.findViewById(R.id.location_info_btn)
				.setOnClickListener((View v) -> showPermissionInfoDialog(R.string.location, R.string.location_desc));

		view.findViewById(R.id.root_info_btn).setOnClickListener(
				(View v) -> showPermissionInfoDialog(R.string.root_access, R.string.root_access_desc));

		view.findViewById(R.id.btn_manage_root).setOnClickListener((View v) -> DeviceUtils.runAsRoot("ls"));

		dialog.show();
	}

	public void showDockLayoutsDialog() {
		SharedPreferences.Editor editor = sp.edit();
		MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
		dialog.setTitle(R.string.choose_dock_layout);
		int layout = sp.getInt("dock_layout", -1);
		dialog.setSingleChoiceItems(R.array.layouts, layout, (DialogInterface arg0, int wich) -> {
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
			editor.commit();
		});

		dialog.setPositiveButton(R.string.ok, null);
		dialog.show();
	}

	private void showPermissionInfoDialog(int permission, int description) {
		MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
		dialogBuilder.setTitle(permission);
		dialogBuilder.setMessage(description);
		dialogBuilder.setPositiveButton(R.string.ok, null);
		dialogBuilder.show();
	}

	public boolean canDrawOverOtherApps() {
		return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this);
	}

	public void grantOverlayPermissions() {
		startActivityForResult(
				new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 8);
	}

	public void enableDeviceAdmin() {
		Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
		intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(this, DeviceAdminReceiver.class));
		startActivityForResult(intent, 8);
	}

	public boolean isdeviceAdminEnabled() {
		DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

		List<ComponentName> deviceAdmins = dpm.getActiveAdmins();

		if (deviceAdmins != null) {
			for (ComponentName deviceAdmin : deviceAdmins) {
				if (deviceAdmin.getPackageName().equals(getPackageName())) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 8) {
			showPermissionsDialog();
		}
	}
}
