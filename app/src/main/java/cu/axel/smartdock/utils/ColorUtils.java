package cu.axel.smartdock.utils;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.app.WallpaperManager;
import java.util.ArrayList;

public class ColorUtils {

    public static ArrayList<String>  getWallpaperColors(Context context) {

        ArrayList<String> wallpaperColors = new ArrayList<String>();
        /*
         Generate Wallpaper colors based on light and dark variation
         Accomplished by inspecting pixels in Wallpaper Bitmap without AndroidX palette library
         You will want to use a factor less than 1.0f to darken. try 0.8f.

         */

        if (DeviceUtils.hasStoragePermission(context)) {
            int wallpaperColor = getHomeScreenWallpaperColor(context);

            wallpaperColors.add(toHexColor(wallpaperColor));
            wallpaperColors.add(toHexColor(manipulateColor(wallpaperColor, 1.5f)));
            wallpaperColors.add(toHexColor(manipulateColor(wallpaperColor, 1.4f)));
            wallpaperColors.add(toHexColor(manipulateColor(wallpaperColor, 1.3f)));
            wallpaperColors.add(toHexColor(manipulateColor(wallpaperColor, 1.2f)));
            wallpaperColors.add(toHexColor(manipulateColor(wallpaperColor, 1.1f)));
            wallpaperColors.add(toHexColor(manipulateColor(wallpaperColor, 1f)));
            wallpaperColors.add(toHexColor(manipulateColor(wallpaperColor, .9f)));
            wallpaperColors.add(toHexColor(manipulateColor(wallpaperColor, .8f)));
            wallpaperColors.add(toHexColor(manipulateColor(wallpaperColor, .7f)));
            wallpaperColors.add(toHexColor(manipulateColor(wallpaperColor, .6f)));
            wallpaperColors.add(toHexColor(manipulateColor(wallpaperColor, .5f)));
        }

        return wallpaperColors;
    }

    public static int manipulateColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(a,
                          Math.min(r, 255),
                          Math.min(g, 255),
                          Math.min(b, 255));
    }

    public static int getHomeScreenWallpaperColor(Context context) {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        Drawable wallpaperDrawable = wallpaperManager.getDrawable();
        wallpaperDrawable.mutate();
        wallpaperDrawable.invalidateSelf();
        return generateImageColor(drawableToBitmap(wallpaperDrawable));
    }

    public static int generateImageColor(Bitmap result) {
        return getBitmapDominantColor(result);
    }

    private static int getBitmapDominantColor(Bitmap bitmap) {
        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, 1, 1, true);
        final int color = newBitmap.getPixel(0, 0);
        newBitmap.recycle();
        return color;
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static int toColor(String color) {
        try {
            return Color.parseColor(color);
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }


    public static String toHexColor(int color) {
        return "#" + Integer.toHexString(color).substring(2);
    }

}
