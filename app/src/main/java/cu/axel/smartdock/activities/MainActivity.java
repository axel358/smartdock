package cu.axel.smartdock.activities;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.accessibility.AccessibilityManager;
import java.util.List;
import android.os.Build;
import cu.axel.smartdock.services.DockService;
import cu.axel.smartdock.R;
import cu.axel.smartdock.utils.DeviceUtils;
import android.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.Toast;

public class MainActivity extends PreferenceActivity {
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!DeviceUtils.hasStoragePermission(this)) {
            requestStoragePermission(0);
        }

        if (!canDrawOverOtherApps() || !DeviceUtils.isAccessibilityServiceEnabled(this))
            showPermissionsDialog();
    }

    public void requestStoragePermission(int code) {
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, code);
    }
	@Override
	protected void onResume() {
		super.onResume();
		invalidateOptionsMenu();
	}

	@Override
	public void onBuildHeaders(List<PreferenceActivity.Header> target) {
		loadHeadersFromResource(R.xml.preference_headers, target);
	}

	@Override
	protected boolean isValidFragment(String fragmentName) {
		return true;
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
		}
		return super.onOptionsItemSelected(item);
	}

    public void showPermissionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogTheme);
        builder.setTitle(R.string.manage_permissions);
        View view = getLayoutInflater().inflate(R.layout.dialog_permissions, null);
        Button grantOverlayBtn = view.findViewById(R.id.btn_grant_overlay);
        Button grantStorageBtn = view.findViewById(R.id.btn_grant_storage);
        Button grantAdminBtn = view.findViewById(R.id.btn_grant_admin);
        Button grantNotificationsBtn = view.findViewById(R.id.btn_grant_notifications);
        Button manageServiceBtn = view.findViewById(R.id.btn_manage_service);

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

        builder.setView(view);
        final AlertDialog dialog = builder.create();

        grantOverlayBtn.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1) {
                    grantOverlayPermissions();
                    dialog.dismiss();
                }
            });
        grantStorageBtn.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1) {
                    requestStoragePermission(8);
                    dialog.dismiss();
                }
            });
        grantAdminBtn.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1) {
                    enableDeviceAdmin();
                    dialog.dismiss();
                }
            });
        grantNotificationsBtn.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1) {
                    startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                }
            });

        manageServiceBtn.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1) {
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                }
            });

        dialog.show();
    }

	public boolean canDrawOverOtherApps() {
		return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this);
	}

	public void grantOverlayPermissions() {
		startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 8);
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
