package cu.axel.smartdock.utils;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.admin.DeviceAdminReceiver;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.hardware.display.DisplayManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.media.MediaPlayer;
import android.net.Uri;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;
import android.os.UserHandle;
import androidx.core.content.ContextCompat;
import cu.axel.smartdock.services.NotificationService;
import java.lang.reflect.Method;
import android.graphics.Bitmap;
import android.os.UserManager;
import java.util.List;
import cu.axel.smartdock.services.DockService;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityManager;
import android.provider.Settings;
import android.os.Build;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import android.app.admin.DevicePolicyManager;
import android.content.Context;

public class DeviceUtils {
	public static final String DISPLAY_SIZE = "display_density_forced";
	public static final String ICON_BLACKLIST = "icon_blacklist";
	public static final String POLICY_CONTROL = "policy_control";
	public static final String IMMERSIVE_APPS = "immersive.status=apps";
	public static final String HEADS_UP_ENABLED = "heads_up_notifications_enabled";
	public static final String SERVICE_NAME = "cu.axel.smartdock/cu.axel.smartdock.services.DockService";
	public static final String ENABLED_ACCESSIBILITY_SERVICES = "enabled_accessibility_services";

	//Root access
	public static Process getRootAccess() throws IOException {
		String[] paths = { "/sbin/su", "/system/sbin/su", "/system/bin/su", "/system/xbin/su", "/su/bin/su",
				"/magisk/.core/bin/su" };
		for (String path : paths) {
			if (new java.io.File(path).exists())
				return Runtime.getRuntime().exec(path);
		}
		return Runtime.getRuntime().exec("/system/bin/su");
	}

	public static String runAsRoot(String command) {
		String output = "";
		try {
			java.lang.Process proccess = getRootAccess();
			DataOutputStream os = new DataOutputStream(proccess.getOutputStream());
			os.writeBytes(command + "\n");
			os.flush();
			os.close();
			BufferedReader br = new BufferedReader(new InputStreamReader(proccess.getInputStream()));
			String line;
			while ((line = br.readLine()) != null) {
				output += line + "\n";
			}
			br.close();
		} catch (IOException e) {
			return "error";
		}
		return output;
	}

	//Device control
	public static boolean lockScreen(Context context) {

		DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
		try {
			dpm.lockNow();
		} catch (SecurityException e) {
			return false;
		}
		return true;
	}

	public static void sendKeyEvent(int keycode) {
		runAsRoot("input keyevent " + keycode);
	}

	public static void sotfReboot() {
		runAsRoot("setprop ctl.restart zygote");
	}

	public static void reboot() {
		runAsRoot("am start -a android.intent.action.REBOOT");
	}

	public static void shutdown() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
			runAsRoot("am start -a android.intent.action.ACTION_REQUEST_SHUTDOWN");
		else
			runAsRoot("am start -a com.android.internal.intent.action.REQUEST_SHUTDOWN");
	}

	public static void toggleVolume(Context context) {
		AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
	}

	public static void playEventSound(Context context, String event) {
		String soundUri = PreferenceManager.getDefaultSharedPreferences(context).getString(event, "default");
		if (!soundUri.equals("default")) {
			try {
				Uri sound = Uri.parse(soundUri);
				if (sound != null) {
					final MediaPlayer mp = MediaPlayer.create(context, sound);
					mp.start();
					mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

						@Override
						public void onCompletion(MediaPlayer p1) {
							mp.release();
						}
					});
				}
			} catch (Exception e) {
			}
		}
	}

	//Device Settings
	public static boolean putSecureSetting(Context context, String setting, int value) {
		try {
			Settings.Secure.putInt(context.getContentResolver(), setting, value);
			return true;
		} catch (SecurityException e) {
			return false;
		}
	}

	public static int getSecureSetting(Context context, String setting, int defaultValue) {
		try {
			return Settings.Secure.getInt(context.getContentResolver(), setting);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static String getSecureSetting(Context context, String setting, String defaultValue) {
		try {
			String value = Settings.Secure.getString(context.getContentResolver(), setting);
			return value == null ? defaultValue : value;
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static boolean putSecureSetting(Context context, String setting, String value) {
		try {
			Settings.Secure.putString(context.getContentResolver(), setting, value);
			return true;
		} catch (SecurityException e) {
			return false;
		}
	}

	public static boolean putGlobalSetting(Context context, String setting, String value) {
		try {
			Settings.Global.putString(context.getContentResolver(), setting, value);
			return true;
		} catch (SecurityException e) {
			return false;
		}
	}

	public static String getGlobalSetting(Context context, String setting, String defaultValue) {
		try {
			String value = Settings.Global.getString(context.getContentResolver(), setting);
			return value == null ? defaultValue : value;
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static boolean putGlobalSetting(Context context, String setting, int value) {
		try {
			Settings.Global.putInt(context.getContentResolver(), setting, value);
			return true;
		} catch (SecurityException e) {
			return false;
		}
	}

	public static int getGlobalSetting(Context context, String setting, int defaultValue) {
		try {
			return Settings.Global.getInt(context.getContentResolver(), setting);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	//Device info
	public static int getStatusBarHeight(Context context) {
		int result = 0;
		int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = context.getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	public static int getNavBarHeight(Context context) {
		int result = 0;
		int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = context.getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	public static String getUserName(Context context) {
		UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
		try {
			return um.getUserName();
		} catch (Exception e) {

		}
		return null;
	}

	public static Bitmap getUserIcon(Context context) {
		UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
		Bitmap userIcon = null;
		try {
			Method getUserIcon = um.getClass().getMethod("getUserIcon", int.class);
			Method myUserId = UserHandle.class.getMethod("myUserId");
			int id = (int) myUserId.invoke(UserHandle.class);
			userIcon = (Bitmap) getUserIcon.invoke(um, id);
			if (userIcon != null)
				userIcon = Utils.getCircularBitmap(userIcon);
		} catch (Exception e) {
		}
		return userIcon;
	}

	public static Display getSecondaryDisplay(Context context) {
		DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
		Display[] displays = dm.getDisplays();
		return dm.getDisplays()[displays.length - 1];
	}

	public static DisplayMetrics getDisplayMetrics(Context context, boolean secondary) {
		DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
		Display display = secondary ? getSecondaryDisplay(context) : dm.getDisplay(Display.DEFAULT_DISPLAY);
		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);
		return metrics;
	}

	public static Context getDisplayContext(Context context, boolean secondary) {
		return secondary ? context.createDisplayContext(getSecondaryDisplay(context)) : context;
	}

	//Permissions
	public static boolean isAccessibilityServiceEnabled(Context context) {
		AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
		List<AccessibilityServiceInfo> enabledServices = am
				.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

		for (AccessibilityServiceInfo enabledService : enabledServices) {
			ServiceInfo serviceInfo = enabledService.getResolveInfo().serviceInfo;
			if (serviceInfo.packageName.equals(context.getPackageName())
					&& serviceInfo.name.equals(DockService.class.getName())) {
				return true;
			}
		}

		return false;
	}

	public static boolean hasStoragePermission(Context context) {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || ContextCompat.checkSelfPermission(context,
				Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
	}

	public static void requestStoragePermissions(Activity context) {
		ActivityCompat.requestPermissions(context, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, 8);
	}

	public static boolean hasLocationPermission(Context context) {
		return ContextCompat.checkSelfPermission(context,
				Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
	}

	public static void requestLocationPermissions(Activity context) {
		ActivityCompat.requestPermissions(context, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 8);
	}

	public static boolean hasWriteSettingsPermission(Context context) {
		return ContextCompat.checkSelfPermission(context,
				Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
	}

	public static boolean grantPermission(String permission) {
		String result = runAsRoot("pm grant cu.axel.smartdock " + permission);
		return result.isEmpty();
	}

	public static void grantOverlayPermissions(Activity context) {
		context.startActivityForResult(
				new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName())),
				8);
	}

	public static void requestDeviceAdminPermissions(Activity context) {
		Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
		intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(context, DeviceAdminReceiver.class));
		context.startActivityForResult(intent, 8);
	}

	public static boolean isdeviceAdminEnabled(Context context) {
		DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

		List<ComponentName> deviceAdmins = dpm.getActiveAdmins();

		if (deviceAdmins != null) {
			for (ComponentName deviceAdmin : deviceAdmins) {
				if (deviceAdmin.getPackageName().equals(context.getPackageName())) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean hasUsageStatsPermission(Context context) {
		return checkAppOpsPermission(context, AppOpsManager.OPSTR_GET_USAGE_STATS);
	}

	private static boolean checkAppOpsPermission(Context context, String permission) {
		PackageManager packageManager = context.getPackageManager();
		ApplicationInfo applicationInfo;
		try {
			applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}

		AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
		int mode = appOpsManager.checkOpNoThrow(permission, applicationInfo.uid, applicationInfo.packageName);
		return (mode == AppOpsManager.MODE_ALLOWED);
	}

	//Service control
	public static void enableService(Context context) {
		String services = getSecureSetting(context, ENABLED_ACCESSIBILITY_SERVICES, "");
		if (services.contains(SERVICE_NAME))
			return;

		String new_services;

		if (services.isEmpty())
			new_services = SERVICE_NAME;
		else
			new_services = services + ":" + SERVICE_NAME;

		putSecureSetting(context, ENABLED_ACCESSIBILITY_SERVICES, new_services);
	}

	public static void disableService(Context context) {
		String services = getSecureSetting(context, ENABLED_ACCESSIBILITY_SERVICES, "");

		if (!services.contains(SERVICE_NAME))
			return;

		String new_services = "";

		if (services.contains(SERVICE_NAME + ":"))
			new_services = services.replace(SERVICE_NAME + ":", "");
		else if (services.contains(":" + SERVICE_NAME))
			new_services = services.replace(":" + SERVICE_NAME, "");
		else if (services.contains(SERVICE_NAME))
			new_services = services.replace(SERVICE_NAME, "");

		putSecureSetting(context, ENABLED_ACCESSIBILITY_SERVICES, new_services);
	}

	public static void restartService(Context context) {
		disableService(context);
		enableService(context);
	}

	public static boolean canDrawOverOtherApps(Context context) {
		return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(context);
	}

	public static boolean isServiceRunning(Context context, Class<?> serviceName) {
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : am.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceName.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

}
