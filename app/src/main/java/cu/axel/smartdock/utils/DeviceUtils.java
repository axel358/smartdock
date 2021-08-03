package cu.axel.smartdock.utils;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.media.AudioManager;
import java.io.DataOutputStream;
import java.io.IOException;

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
}
