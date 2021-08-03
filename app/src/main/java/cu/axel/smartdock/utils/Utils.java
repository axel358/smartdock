package cu.axel.smartdock.utils;

import android.content.Context;
import android.widget.PopupMenu;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

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
