package cu.axel.smartdock.utils;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;
import cu.axel.smartdock.services.DockService;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import android.widget.Toast;

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
        runAsRoot("input keyevent " + keycode);
	}

    public static void runAsRoot(String command) {
        try {
            java.lang.Process proccess = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(proccess.getOutputStream());
            os.writeBytes(command + "\n");
            os.flush();
            os.close();
        } catch (IOException e) {}
    }

    public static void sotfReboot() {
        runAsRoot("setprop ctl.restart zygote");
    }

    public static void setDisplaySize(int size) {
        if (size > 0)
            runAsRoot("settings put secure display_density_forced " + size);
        else
            runAsRoot("settings delete secure display_density_forced");
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

    public static int getStatusBarHeight(Context context) { 
        int result = 0;
        int resourceId =context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        } 
        return result;
    }

    public static String getUserName(Context context) {
        UserManager um=(UserManager) context.getSystemService(Context.USER_SERVICE);
        try {
            return um.getUserName();
        } catch (Exception e) {

        }
        return null;
    }

    public static Bitmap getUserIcon(Context context) {
        UserManager um=(UserManager) context.getSystemService(Context.USER_SERVICE);
        Bitmap userIcon=null;
        try {
            Method getUserIcon=um.getClass().getMethod("getUserIcon", int.class);
            Method myUserId=UserHandle.class.getMethod("myUserId");
            int id=myUserId.invoke(UserHandle.class);
            userIcon = (Bitmap) getUserIcon.invoke(um, id);
            if (userIcon != null)
                userIcon = Utils.getCircularBitmap(userIcon);
        } catch (Exception e) {
        }
        return userIcon;
    }
    public static void toggleQuickSettings(Context context, String panel) {
        try {
            Object statusBarService = context.getSystemService("statusbar");
            Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
            Method expandSettingsPanel=statusbarManager.getMethod("expandSettingsPanel", String.class);
            expandSettingsPanel.setAccessible(true);
            expandSettingsPanel.invoke(statusBarService, panel);
        } catch (Exception e) {
            Toast.makeText(context, e.toString() + "\n" + e.getCause(), 5000).show();
        }
    }

}
