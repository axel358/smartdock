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

public class MainActivity extends PreferenceActivity 
{
	private final int ACCESSIBILITY_REQUEST_CODE = 13;
	private final int DEVICE_ADMIN_REQUEST_CODE = 358;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT > 22)
        {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 156);
            }
        }
    }
    

	@Override
	protected void onResume()
	{
		super.onResume();
		invalidateOptionsMenu();
	}

	@Override
	public void onBuildHeaders(List<PreferenceActivity.Header> target)
	{
		loadHeadersFromResource(R.xml.preference_headers, target);


	}

	@Override
	protected boolean isValidFragment(String fragmentName)
	{
		return true;
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		if (canDrawOverOtherApps())
		{
			menu.getItem(0).setEnabled(false);
		}
		else
		{
			menu.getItem(0).setEnabled(true);
			menu.getItem(1).setEnabled(false);
		}
		if (isAccessibilityServiceEnabled())
		{
			menu.getItem(1).setEnabled(false);
		}
		if (isdeviceAdminEnabled())
		{
			menu.getItem(2).setEnabled(false);
		}
		return super.onPrepareOptionsMenu(menu);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{

		switch (item.getItemId())
		{
			case R.id.action_enable_accessibilty:
				enableAccessibility();
				break;
			case R.id.action_enable_admin:
				enableDeviceAdmin();
				break;
			case R.id.action_grant_permissions:
				grantOverlayPermissions();
		}
		return super.onOptionsItemSelected(item);
	}

	public boolean canDrawOverOtherApps()
	{
		return Settings.canDrawOverlays(this);
	}

	public void grantOverlayPermissions()
	{
		startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
	}


	public void enableDeviceAdmin()
	{
		Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
		intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(this, DeviceAdminReceiver.class));
		startActivityForResult(intent, DEVICE_ADMIN_REQUEST_CODE);
	}

	public boolean isdeviceAdminEnabled()
	{
		DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

		List<ComponentName> deviceAdmins = dpm.getActiveAdmins();

		if (deviceAdmins != null)
		{
			for (ComponentName deviceAdmin : deviceAdmins)
			{
				if (deviceAdmin.getPackageName().equals(getPackageName()))
				{
					return true;
				}
			}
		}
		return false;
	}

	public void enableAccessibility()
	{
		startActivityForResult(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), ACCESSIBILITY_REQUEST_CODE);
	}


	public boolean isAccessibilityServiceEnabled()
	{
		AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
		List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

		for (AccessibilityServiceInfo enabledService : enabledServices)
		{
			ServiceInfo serviceInfo = enabledService.getResolveInfo().serviceInfo;
			if (serviceInfo.packageName.equals(getPackageName()) && serviceInfo.name.equals(DockService.class.getName()))
			{
				return true;
			}
		}

		return false;
	}




}
