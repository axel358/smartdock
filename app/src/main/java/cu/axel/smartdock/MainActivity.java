package cu.axel.smartdock;

import android.accessibilityservice.*;
import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.os.*;
import android.provider.*;
import android.view.*;
import android.view.accessibility.*;
import java.util.*;
import android.content.pm.*;
import android.widget.*;
import android.util.*;
import android.preference.*;
import android.preference.PreferenceActivity.*;
import android.net.*;

public class MainActivity extends PreferenceActivity 
{
	private final int ACCESSIBILITY_REQUEST_CODE = 13;
	private final int DEVICE_ADMIN_REQUEST_CODE = 358;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

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
