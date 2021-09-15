package cu.axel.smartdock.utils;

import android.content.Context;
import android.widget.PopupMenu;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Utils {
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
        if (script.exists() && script.canExecute()) {
            try {
                Runtime.getRuntime().exec(script.getAbsolutePath());
            } catch (IOException e) {}
        }
    }

}
