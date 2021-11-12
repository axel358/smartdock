package cu.axel.smartdock.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.widget.PopupMenu;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import android.graphics.PorterDuff.Mode;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;

public class Utils {
    public static boolean notificationPanelVisible;
    private static final String FILES_DIR="/data/data/cu.axel.smartdock/files";
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

    public static void doAutostart() {
        File script=new File(FILES_DIR + "/autostart.sh");
        if (script.exists()) {
            try {
                if (!script.canExecute())
                    script.setExecutable(true);
                Runtime.getRuntime().exec(script.getAbsolutePath());
            } catch (IOException e) {}
        }
    }

    public static String readAutostart() {
        String content="";
        File script=new File(FILES_DIR + "/autostart.sh");
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

    public static void saveAutoStart(String content) {
        File script=new File(FILES_DIR + "/autostart.sh");
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

}
