package cu.axel.smartdock.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.view.WindowManager;
import android.widget.PopupMenu;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;

import cu.axel.smartdock.R;

public class Utils {
    public static boolean notificationPanelVisible, shouldPlayChargeComplete;
    public static String AUTOSTART_SCRIPT = "autostart.sh";
    public static long startupTime;
    //public static int dockHeight;

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
                    Class.forName(obj.getClass().getName()).getMethod("setForceShowIcon", Boolean.TYPE).invoke(obj,
                            new Boolean(true));
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

    public static Bitmap getCircularBitmap(Bitmap bitmap) {
        if (bitmap == null)
            return bitmap;

        //Copy the bitmap to avoid software rendering issues
        Bitmap bitmapCopy = bitmap.copy(Bitmap.Config.ARGB_8888, false);
        bitmap.recycle();

        Bitmap result = Bitmap.createBitmap(bitmapCopy.getWidth(), bitmapCopy.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmapCopy.getWidth(), bitmapCopy.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(bitmapCopy.getWidth() / 2, bitmapCopy.getHeight() / 2, bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmapCopy, rect, rect, paint);
        bitmapCopy.recycle();
        return result;
    }

    public static Bitmap getBitmapFromUri(Context context, Uri uri) {
        Bitmap bitmap = null;
        ContentResolver contentResolver = context.getContentResolver();
        try {
            if (Build.VERSION.SDK_INT < 28) {
                bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri);
            } else {
                ImageDecoder.Source source = ImageDecoder.createSource(contentResolver, uri);
                bitmap = ImageDecoder.decodeBitmap(source);
            }
        } catch (Exception e) {
        }
        return bitmap;
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
            FileWriter fw = new FileWriter(
                    new File(context.getExternalFilesDir(null), name + "_" + System.currentTimeMillis() + ".log"));
            fw.write(log);
            fw.close();
        } catch (IOException e) {
        }
    }

    public static WindowManager.LayoutParams makeWindowParams(int width, int height, Context context,
                                                              boolean preferLastDisplay) {
        int displayWidth = DeviceUtils.getDisplayMetrics(context, preferLastDisplay).widthPixels;
        int displayHeight = DeviceUtils.getDisplayMetrics(context, preferLastDisplay).heightPixels;
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        layoutParams.width = Math.min(displayWidth, width);
        layoutParams.height = Math.min(displayHeight, height);
        return layoutParams;
    }

    public static double solve(String expression) {
        if (expression.contains("+"))
            return Double.parseDouble(expression.split("\\+")[0]) + Double.parseDouble(expression.split("\\+")[1]);
        else if (expression.contains("-"))
            return Double.parseDouble(expression.split("\\-")[0]) - Double.parseDouble(expression.split("\\-")[1]);
        if (expression.contains("/"))
            return Double.parseDouble(expression.split("\\/")[0]) / Double.parseDouble(expression.split("\\/")[1]);
        if (expression.contains("*"))
            return Double.parseDouble(expression.split("\\*")[0]) * Double.parseDouble(expression.split("\\*")[1]);
        return 0;
    }

}
