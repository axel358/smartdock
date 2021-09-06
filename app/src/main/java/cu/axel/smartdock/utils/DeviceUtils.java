package cu.axel.smartdock.utils;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import cu.axel.smartdock.services.DockService;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class DeviceUtils {
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
        try {
            java.lang.Process proccess = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(proccess.getOutputStream());
            os.writeBytes("input keyevent " + keycode + "\n");
            os.flush();
            os.close();
        } catch (IOException e) {
        }
	}

    public static void toggleVolume(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);

	}

    public static boolean isAccessibilityServiceEnabled(Context context) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        for (AccessibilityServiceInfo enabledService : enabledServices) {
            ServiceInfo serviceInfo = enabledService.getResolveInfo().serviceInfo;
            if (serviceInfo.packageName.equals(context.getPackageName()) && serviceInfo.name.equals(DockService.class.getName())) {
                return true;
            }
        }

        return false;
    }

    public static void enableAccessibility(Context context) {
        context.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

}
