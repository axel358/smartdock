package cu.axel.smartdock.utils

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import com.google.android.material.color.DynamicColors
import cu.axel.smartdock.R
import kotlin.math.roundToInt

object ColorUtils {
    @SuppressLint("MissingPermission")
    private fun getWallpaperColors(context: Context): ArrayList<String> {
        val wallpaperColors = ArrayList<String>()
        /*
		 Generate Wallpaper colors based on light and dark variation
		 Accomplished by inspecting pixels in Wallpaper Bitmap without AndroidX palette library
		 You will want to use a factor less than 1.0f to darken. try 0.8f.
		
		 */if (DeviceUtils.hasStoragePermission(context)) {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val wallpaperDrawable = wallpaperManager.drawable
            wallpaperDrawable.mutate()
            wallpaperDrawable.invalidateSelf()
            val wallpaperBitmap = drawableToBitmap(wallpaperDrawable)
            val color = wallpaperBitmap.getPixel(wallpaperBitmap.width / 4, wallpaperBitmap.height / 4)
            wallpaperColors.add(toHexColor(color))
            wallpaperColors.add(toHexColor(manipulateColor(color, .8f)))
            wallpaperColors.add(toHexColor(manipulateColor(color, .5f)))
        }
        return wallpaperColors
    }

    @SuppressLint("ResourceType")
    fun getThemeColors(context: Context, forceDark: Boolean): IntArray {
        val colors = IntArray(3)
        val variant = if (forceDark) R.style.ThemeOverlay_Material3_DynamicColors_Dark else R.style.ThemeOverlay_Material3_DynamicColors_DayNight
        val styledContext = DynamicColors.wrapContextIfAvailable(context, variant)
        val attrsToResolve = intArrayOf(R.attr.colorPrimary, R.attr.colorSurface, R.attr.colorError)
        val attrs = styledContext.obtainStyledAttributes(attrsToResolve)
        colors[0] = attrs.getColor(0, 0)
        colors[1] = attrs.getColor(1, 0)
        colors[2] = attrs.getColor(2, 0)
        attrs.recycle()
        return colors
    }

    //TODO: Use builtin
    fun manipulateColor(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).roundToInt()
        val g = (Color.green(color) * factor).roundToInt()
        val b = (Color.blue(color) * factor).roundToInt()
        return Color.argb(a, r.coerceAtMost(255), g.coerceAtMost(255), b.coerceAtMost(255))
    }

    private fun getBitmapDominantColor(bitmap: Bitmap): Int {
        var color = bitmap.getPixel(bitmap.width / 2, bitmap.height / 9)
        if (color == Color.TRANSPARENT) color = bitmap.getPixel(bitmap.width / 4, bitmap.height / 2)
        if (color == Color.TRANSPARENT) color = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
        return color
    }

    fun getDrawableDominantColor(drawable: Drawable): Int {
        return getBitmapDominantColor(drawableToBitmap(drawable))
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            if (drawable.bitmap != null) {
                return drawable.bitmap
            }
        }
        val bitmap: Bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Single color bitmap will be created of 1x1 pixel
        } else {
            Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888)
        }
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun applyColor(view: View, color: Int) {
        view.background.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
    }

    fun getMainColors(sp: SharedPreferences, context: Context): IntArray {
        val theme = sp.getString("theme", "dark")
        var mainColor = 0
        var secondaryColor = 0
        var alpha = 255
        val colors = IntArray(5)
        when (theme) {
            "dark" -> {
                mainColor = Color.parseColor("#212121")
                secondaryColor = manipulateColor(mainColor, 1.35f)
            }

            "black" -> {
                mainColor = Color.parseColor("#060606")
                secondaryColor = manipulateColor(mainColor, 2.2f)
            }

            "transparent" -> {
                mainColor = Color.parseColor("#050505")
                secondaryColor = manipulateColor(mainColor, 2f)
                alpha = 225
            }

            "material_u" -> if (DynamicColors.isDynamicColorAvailable()) {
                val surfaceColor = getThemeColors(context, true)[1]
                mainColor = manipulateColor(surfaceColor, 0.9f)
                secondaryColor = manipulateColor(surfaceColor, 1.2f)
            } else {
                mainColor = Color.parseColor(getWallpaperColors(context)[2])
                secondaryColor = Color.parseColor(getWallpaperColors(context)[1])
            }

            "custom" -> {
                mainColor = Color.parseColor(sp.getString("theme_main_color", "#212121"))
                secondaryColor = manipulateColor(mainColor, 1.2f)
                alpha = sp.getInt("theme_main_alpha", 255)
            }
        }
        colors[0] = mainColor
        colors[1] = alpha
        colors[2] = secondaryColor
        if (alpha < 255) alpha = (alpha - alpha * 0.60).toInt()
        //secondary color alpha
        colors[3] = alpha
        //separator color
        colors[4] = if (theme == "black") colors[2] else manipulateColor(colors[0], 0.8f)
        return colors
    }

    fun applyMainColor(context: Context, sp: SharedPreferences, view: View) {
        val colors = getMainColors(sp, context)
        applyColor(view, colors[0])
        view.background.alpha = colors[1]
    }

    fun applySecondaryColor(context: Context, sp: SharedPreferences, view: View) {
        val colors = getMainColors(sp, context)
        applyColor(view, colors[2])
        val alpha = colors[3]
        view.background.alpha = alpha
    }

    fun toColor(color: String): Int {
        return try {
            Color.parseColor(color)
        } catch (e: IllegalArgumentException) {
            -1
        }
    }

    fun toHexColor(color: Int): String {
        return "#" + Integer.toHexString(color).substring(2)
    }
}
