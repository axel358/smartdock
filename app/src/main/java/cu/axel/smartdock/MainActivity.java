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

public class MainActivity extends PreferenceActivity 
{
	private final int ACCESSIBILITY_REQUEST_CODE = 13;
	private final int DEVICE_ADMIN_REQUEST_CODE = 358;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		isAccessibilityServiceEnabled();

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
	public boolean onOptionsItemSelected(MenuItem item)
	{

		switch (item.getItemId())
		{
			case R.id.action_enable_accessibilty:
				enableAccessibility();
				break;
			case R.id.action_enable_admin:
				enableDeviceAdmin();
		}
		return super.onOptionsItemSelected(item);
	}



	public void enableDeviceAdmin()
	{
		Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
		intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(this, DeviceAdminReceiver.class));
		startActivityForResult(intent, DEVICE_ADMIN_REQUEST_CODE);
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
			Toast.makeText(this, enabledService.getResolveInfo().serviceInfo.name, 5000).show();
			ServiceInfo serviceInfo = enabledService.getResolveInfo().serviceInfo;
			if (serviceInfo.packageName.equals(getPackageName()) && serviceInfo.name.equals(DockService.class.getName()))
			{
				return true;
			}
		}

		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == ACCESSIBILITY_REQUEST_CODE)
		{
			if (isAccessibilityServiceEnabled())
			{

			}
			else
			{
				Toast.makeText(this, "You must enable the accessibility service for the app to work", 5000).show();
			}
		}
	}


}
