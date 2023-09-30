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

import android.widget.Toast;
import androidx.preference.PreferenceManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.io.InputStreamReader;
import cu.axel.smartdock.R;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class Utils {
    public static boolean notificationPanelVisible, shouldPlayChargeComplete;
    public static long startupTime;
	public static String BACKUP_FILE_NAME = "cu.axel.smartdock_preferences_backup.sdp";
    //public static int dockHeight;

    public static void toggleBuiltinNavigation(SharedPreferences.Editor editor, boolean value) {
        editor.putBoolean("enable_nav_back", value);
        editor.putBoolean("enable_nav_home", value);
        editor.putBoolean("enable_nav_recents", value);
        editor.commit();
    }

    public static int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static Bitmap getCircularBitmap(Bitmap bitmap) {
        if (bitmap == null)
            return null;

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
	
	public static void backupPreferences(Context context, Uri backupUri) {
		Map<String, ?> allPrefs = PreferenceManager.getDefaultSharedPreferences(context).getAll();
		StringBuilder stringBuilder = new StringBuilder();
		
		for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
			String type = "string";
			if (entry.getValue() instanceof Boolean) {
				type = "boolean";
				} else if (entry.getValue() instanceof Integer) {
				type = "integer";
			}
			
			stringBuilder.append(type).append(" ").append(entry.getKey()).append(" ")
			.append(entry.getValue().toString()).append("\n");
		}
		String content = stringBuilder.toString().trim();
		
		OutputStream os = null;
		try {
			os = context.getContentResolver().openOutputStream(backupUri);
			os.write(content.getBytes());
			os.flush();
			Toast.makeText(context, R.string.preferences_saved, Toast.LENGTH_SHORT).show();
			
			} catch (IOException e) {
			e.printStackTrace();
			} finally {
			if (os != null) {
				try {
					os.close();
					} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void restorePreferences(Context context, Uri restoreUri) {
		InputStream is = null;
		
		try {
			is = context.getContentResolver().openInputStream(restoreUri);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
			
			SharedPreferences.Editor editor = sharedPreferences.edit();
			while ((line = br.readLine()) != null) {
				String[] contents = line.split(" ");
				if (contents.length > 2) {
					String type = contents[0];
					String key = contents[1];
					String value = contents[2];
					
					if (type.equals("boolean"))
					editor.putBoolean(key, Boolean.parseBoolean(value));
					else if (type.equals("integer"))
					editor.putInt(key, Integer.parseInt(value));
					else
					editor.putString(key, value);
				}
			}
			br.close();
			editor.commit();
			Toast.makeText(context, R.string.preferences_restored, Toast.LENGTH_SHORT).show();
			} catch (IOException e) {
			e.printStackTrace();
			} finally {
			if (is != null) {
				try {
					is.close();
					} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}

}
