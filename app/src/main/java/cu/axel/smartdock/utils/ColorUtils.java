package cu.axel.smartdock.utils;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColorUtils {

    public static ArrayList<String>  getWallpaperColors(Context context) {

        ArrayList<String> wallpaperColors = new ArrayList<String>();
        /*
         Generate Wallpaper colors based on light and dark variation
         Accomplished by inspecting pixels in Wallpaper Bitmap without AndroidX palette library
         You will want to use a factor less than 1.0f to darken. try 0.8f.

         */

        if (DeviceUtils.hasStoragePermission(context)) {
            
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
            Drawable wallpaperDrawable = wallpaperManager.getDrawable();
            wallpaperDrawable.mutate();
            wallpaperDrawable.invalidateSelf();
            Bitmap wallpaperBitmap = drawableToBitmap(wallpaperDrawable);
            
            int color1 = wallpaperBitmap.getPixel(wallpaperBitmap.getWidth()/4, wallpaperBitmap.getHeight()/4);
            int color2 = wallpaperBitmap.getPixel(wallpaperBitmap.getWidth() - wallpaperBitmap.getWidth()/4, wallpaperBitmap.getHeight()/4);
            int color3 = wallpaperBitmap.getPixel(wallpaperBitmap.getWidth()/2, wallpaperBitmap.getHeight() - wallpaperBitmap.getHeight()/4);

            
            
            wallpaperColors.add(toHexColor(manipulateColor(color1, 1.5f)));
            wallpaperColors.add(toHexColor(manipulateColor(color1, 1.2f)));
            wallpaperColors.add(toHexColor(color1));
            wallpaperColors.add(toHexColor(manipulateColor(color1, .8f)));
            wallpaperColors.add(toHexColor(manipulateColor(color1, .5f)));
            
            wallpaperColors.add(toHexColor(manipulateColor(color2, 1.5f)));
            wallpaperColors.add(toHexColor(manipulateColor(color2, 1.2f)));
            wallpaperColors.add(toHexColor(color2));
            wallpaperColors.add(toHexColor(manipulateColor(color2, .8f)));
            wallpaperColors.add(toHexColor(manipulateColor(color2, .5f)));
            
            wallpaperColors.add(toHexColor(manipulateColor(color3, 1.5f)));
            wallpaperColors.add(toHexColor(manipulateColor(color3, 1.2f)));
            wallpaperColors.add(toHexColor(color3));
            wallpaperColors.add(toHexColor(manipulateColor(color3, .8f)));
            wallpaperColors.add(toHexColor(manipulateColor(color3, .5f)));
            
            
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

    
    public static int getBitmapDominantColor(Bitmap bitmap) {
        int color = bitmap.getPixel(bitmap.getWidth()/2, bitmap.getHeight()/9);
        if(color==Color.TRANSPARENT)
            color=bitmap.getPixel(bitmap.getWidth()/4, bitmap.getHeight()/2);
        if(color==Color.TRANSPARENT)
            color=bitmap.getPixel(bitmap.getWidth()/2, bitmap.getHeight()/2);
        return color;
    }
    /*
    public static int getBitmapDominantColorAccurate(Bitmap bitmap) {
      return  Palette.from(bitmap).generate().getDominantColor(Color.WHITE);
        
    }*/
    
    public static int getDrawableDominantColor(Drawable drawable){
        return getBitmapDominantColor(drawableToBitmap(drawable));
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
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
    
    public static void applyColor(View view,int color){
        view.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    }
    
    public static void applyMainColor(SharedPreferences sp, View view) {
        String color = "";
        int alpha = 255;
        switch (sp.getString("theme", "dark")) {
            case "dark":
                color = "#212121";
                break;
            case "black":
                color = "#000000";
                break;
            case "transparent":
                color = "#000000";
                alpha = 225;
                break;
            case "nanashi":
                color = "#202020";
                alpha = 235;
                break;
            case "custom":
                color = sp.getString("theme_main_color", "#212121");
                alpha = sp.getInt("theme_main_alpha", 255);
        }
        applyColor(view, Color.parseColor(color));
        view.getBackground().setAlpha(alpha);
    }

    public static void applySecondaryColor(SharedPreferences sp,  View view) {
        String color = "";
        int alpha = 255;
        switch (sp.getString("theme", "dark")) {
            case "dark":
                color = "#292929";
                break;
            case "black":
                color = "#0B0B0B";
                break;
            case "transparent":
                color = "#000000";
                alpha = 80;
                break;
            case "nanashi":
                color = "#cccccc";
                alpha = 40;
                break;
            case "custom":
                color = sp.getString("theme_secondary_color", "#292929");
                alpha = sp.getInt("theme_secondary_alpha", 255);
        }
        applyColor(view, Color.parseColor(color));
        view.getBackground().setAlpha(alpha);
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
