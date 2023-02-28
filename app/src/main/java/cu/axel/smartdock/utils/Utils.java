package cu.axel.smartdock.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.PorterDuff.Mode;
import android.os.Build;
import android.view.WindowManager;
import android.widget.PopupMenu;
import cu.axel.smartdock.R;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import android.view.MotionEvent;
import android.content.pm.ApplicationInfo;
import cu.axel.smartdock.services.ElevatedService;
import android.widget.Toast;

public class Utils {
    public static boolean notificationPanelVisible, shouldPlayChargeComplete;
    public static String AUTOSTART_SCRIPT="autostart.sh";
    public static long startupTime;

    public static void toggleBuiltinNavigation(SharedPreferences.Editor editor, boolean value) {
        editor.putBoolean("enable_nav_back", value);
        editor.putBoolean("enable_nav_home", value);
        editor.putBoolean("enable_nav_recents", value);
        editor.commit();
    }

    public static void setForceShowIcon(PopupMenu popupMenu) {
		try {
            Field[] declaredFields = popupMenu.getClass().getDeclaredFields();
            for (Field field : declaredFields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object obj = field.get(popupMenu);
                    Class.forName(obj.getClass().getName()).getMethod("setForceShowIcon", Boolean.TYPE).invoke(obj, new Boolean(true));
                    return;
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    public static boolean allowReflection() {
        try {
            Method forName = Class.class.getDeclaredMethod("forName", String.class);
            Method getDeclaredMethod = Class.class.getDeclaredMethod("getDeclaredMethod", String.class, Class[].class);

            Class<?> vmRuntimeClass = (Class<?>) forName.invoke(null, "dalvik.system.VMRuntime");
            Method getRuntime = (Method) getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", null);
            Method setHiddenApiExemptions = (Method) getDeclaredMethod.invoke(vmRuntimeClass, "setHiddenApiExemptions", new Class[]{String[].class});

            Object vmRuntime = getRuntime.invoke(null);
            setHiddenApiExemptions.invoke(vmRuntime, new Object[]{new String[]{"L"}});
        } catch (Throwable ignored) {
            return  false;
        }

        return true;
    }

    public static int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static void doAutostart(Context context) {
        File script=new File(context.getFilesDir(), AUTOSTART_SCRIPT);
        if (script.exists()) {
            try {
                if (!script.canExecute())
                    script.setExecutable(true);
                Runtime.getRuntime().exec(script.getAbsolutePath());
            } catch (IOException e) {}
        }
    }

    public static String readAutostart(Context context) {
        String content="";
        File script=new File(context.getFilesDir(), AUTOSTART_SCRIPT);
        try {
            BufferedReader br=new BufferedReader(new FileReader(script));
            String line="";
            while ((line = br.readLine()) != null) {
                content += line + "\n";
            }
            br.close();
        } catch (IOException e) {
        }
        return content;
    }

    public static void saveAutoStart(Context context, String content) {
        File script=new File(context.getFilesDir(), AUTOSTART_SCRIPT);
        try {
            FileWriter fw=new FileWriter(script, false);
            fw.write(content);
            fw.close();
        } catch (IOException e) {}
    }

    public static Bitmap getCircularBitmap(Bitmap bitmap) {
        Bitmap result = Bitmap.createBitmap(bitmap.getWidth(),
                                            bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                          bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return result;
    }

    public static int getBatteryDrawable(int level, boolean plugged) {
        if (plugged) {

            if (level == 0)
                return R.drawable.battery_charging_empty;
            else if (level > 0 && level < 30)
                return R.drawable.battery_charging_20;
            else if (level > 30 && level < 50)
                return R.drawable.battery_charging_30;
            else if (level > 50 && level < 60)
                return R.drawable.battery_charging_50;
            else if (level > 60 && level < 80)
                return R.drawable.battery_charging_60;
            else if (level > 80 && level < 90)
                return R.drawable.battery_charging_80;
            else if (level > 90 && level < 100)
                return R.drawable.battery_charging_90;
            else if (level == 100)
                return R.drawable.battery_charging_full;
        } else {

            if (level == 0)
                return R.drawable.battery_empty;
            else if (level > 0 && level < 30)
                return R.drawable.battery_20;
            else if (level > 30 && level < 50)
                return R.drawable.battery_30;
            else if (level > 50 && level < 60)
                return R.drawable.battery_50;
            else if (level > 60 && level < 80)
                return R.drawable.battery_60;
            else if (level > 80 && level < 90)
                return R.drawable.battery_80;
            else if (level > 90 && level < 100)
                return R.drawable.battery_90;  
            else if (level == 100)
                return R.drawable.battery_full;
        }
        return R.drawable.battery_empty;
    }

    public static void saveLog(Context context, String name, String log) {
        try {
            FileWriter fw = new FileWriter(new File(context.getExternalFilesDir(null), name + "_" + System.currentTimeMillis() + ".log"));
            fw.write(log);
            fw.close();
        } catch (IOException e) {}
    }

    public static WindowManager.LayoutParams makeWindowParams(int width, int height) {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        layoutParams.width = width;
        layoutParams.height = height;
        return layoutParams;
    }

    public static double solve(String expression) {
        if (expression.contains("+"))
            return Double.parseDouble(expression.split("\\+")[0]) + Double.parseDouble(expression.split("\\+")[1]);
        else  if (expression.contains("-"))
            return Double.parseDouble(expression.split("\\-")[0]) - Double.parseDouble(expression.split("\\-")[1]);
        if (expression.contains("/"))
            return Double.parseDouble(expression.split("\\/")[0]) / Double.parseDouble(expression.split("\\/")[1]);
        if (expression.contains("*"))
            return Double.parseDouble(expression.split("\\*")[0]) * Double.parseDouble(expression.split("\\*")[1]);
        return 0;
    }

    public static void startElevatedService(Context context) {
        final String className = ElevatedService.class.getName();
        ApplicationInfo appInfo = context.getApplicationInfo();

        File file = new File(context.getFilesDir(), "start_elevated_service.sh");
        try {

            FileWriter linesToWrite = new FileWriter(file);
            linesToWrite.append("#!/system/bin/sh\n");
            linesToWrite.append("exec env ");
            linesToWrite.append("LD_LIBRARY_PATH=\"").append(appInfo.nativeLibraryDir)  //path containing lib*.so
                .append("\" CLASSPATH=\"").append(appInfo.publicSourceDir) // Absolute path to apk in /data/app
                .append("\" /system/bin/app_process /system/bin ")
                .append(className + " Thailand").append("\n");

            linesToWrite.flush();
            linesToWrite.close();

            Toast.makeText(context, DeviceUtils.runAsRoot("sh " + file.getAbsolutePath()), 5000).show();

        } catch (IOException e) {
            Toast.makeText(context, e.getMessage(), 5000).show();
        }
    }


}
