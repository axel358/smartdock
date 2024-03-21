package cu.axel.smartdock.utils

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.Display
import android.view.WindowManager
import android.widget.Toast
import androidx.preference.PreferenceManager
import cu.axel.smartdock.R
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date

object Utils {
    var notificationPanelVisible = false
    var shouldPlayChargeComplete = false
    var startupTime: Long = 0

    //public static int dockHeight;
    fun toggleBuiltinNavigation(editor: SharedPreferences.Editor, value: Boolean) {
        editor.putBoolean("enable_nav_back", value)
        editor.putBoolean("enable_nav_home", value)
        editor.putBoolean("enable_nav_recents", value)
        editor.commit()
    }

    fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density + 0.5f).toInt()
    }

    fun getCircularBitmap(bitmap: Bitmap?): Bitmap? {
        if (bitmap == null) return null

        //Copy the bitmap to avoid software rendering issues
        val bitmapCopy = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        bitmap.recycle()
        val result =
            Bitmap.createBitmap(bitmapCopy.width, bitmapCopy.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val color = -0xbdbdbe
        val paint = Paint()
        val rect = Rect(0, 0, bitmapCopy.width, bitmapCopy.height)
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawCircle(
            (bitmapCopy.width / 2).toFloat(),
            (bitmapCopy.height / 2).toFloat(),
            (bitmap.width / 2).toFloat(),
            paint
        )
        paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_IN))
        canvas.drawBitmap(bitmapCopy, rect, rect, paint)
        bitmapCopy.recycle()
        return result
    }

    fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        var bitmap: Bitmap? = null
        val contentResolver = context.contentResolver
        try {
            bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            }
        } catch (_: Exception) {
        }
        return bitmap
    }

    fun getBatteryDrawable(level: Int, plugged: Boolean): Int {
        if (plugged) {
            when (level) {
                0 -> return R.drawable.battery_charging_empty
                in 1..29 -> return R.drawable.battery_charging_20
                in 31..49 -> return R.drawable.battery_charging_30
                in 51..59 -> return R.drawable.battery_charging_50
                in 61..79 -> return R.drawable.battery_charging_60
                in 81..89 -> return R.drawable.battery_charging_80
                in 91..99 -> return R.drawable.battery_charging_90
                100 -> return R.drawable.battery_charging_full
            }
        } else {
            when (level) {
                0 -> return R.drawable.battery_empty
                in 1..29 -> return R.drawable.battery_20
                in 31..49 -> return R.drawable.battery_30
                in 51..59 -> return R.drawable.battery_50
                in 61..79 -> return R.drawable.battery_60
                in 81..89 -> return R.drawable.battery_80
                in 91..99 -> return R.drawable.battery_90
                100 -> return R.drawable.battery_full
            }
        }
        return R.drawable.battery_empty
    }

    fun saveLog(context: Context, name: String, log: String) {
        try {
            val fw = FileWriter(
                File(context.getExternalFilesDir(null), name + "_" + currentDateString + ".log")
            )
            fw.write(log)
            fw.close()
        } catch (_: IOException) {
        }
    }

    fun makeWindowParams(
        width: Int, height: Int, context: Context,
        secondary: Boolean = false
    ): WindowManager.LayoutParams {

        val displayId =
            if (secondary) DeviceUtils.getSecondaryDisplay(context).displayId else Display.DEFAULT_DISPLAY

        val displayWidth = DeviceUtils.getDisplayMetrics(context, displayId).widthPixels
        val displayHeight = DeviceUtils.getDisplayMetrics(context, displayId).heightPixels
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.format = PixelFormat.TRANSLUCENT
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        layoutParams.type =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE
        layoutParams.width = displayWidth.coerceAtMost(width)
        layoutParams.height = displayHeight.coerceAtMost(height)
        return layoutParams
    }

    fun solve(expression: String): Double {
        if (expression.contains("+")) return expression.split("\\+".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()[0].toDouble() + expression.split("\\+".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()[1].toDouble() else if (expression.contains("-")) return expression.split(
            "\\-".toRegex()
        ).dropLastWhile { it.isEmpty() }
            .toTypedArray()[0].toDouble() - expression.split("\\-".toRegex())
            .dropLastWhile { it.isEmpty() }.toTypedArray()[1].toDouble()
        if (expression.contains("/")) return expression.split("\\/".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()[0].toDouble() / expression.split("\\/".toRegex())
            .dropLastWhile { it.isEmpty() }.toTypedArray()[1].toDouble()
        return if (expression.contains("*")) expression.split("\\*".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()[0].toDouble() * expression.split("\\*".toRegex())
            .dropLastWhile { it.isEmpty() }.toTypedArray()[1].toDouble() else 0.0
    }

    fun backupPreferences(context: Context, backupUri: Uri) {
        val allPrefs = PreferenceManager.getDefaultSharedPreferences(context).all
        val stringBuilder = StringBuilder()
        for ((key, value) in allPrefs) {
            var type = "string"
            if (value is Boolean) {
                type = "boolean"
            } else if (value is Int) {
                type = "integer"
            }
            stringBuilder.append(type).append(" ").append(key).append(" ")
                .append(value.toString()).append("\n")
        }
        val content = stringBuilder.toString().trim { it <= ' ' }
        var outputStream: OutputStream? = null
        try {
            outputStream = context.contentResolver.openOutputStream(backupUri)
            outputStream!!.write(content.toByteArray())
            outputStream.flush()
            Toast.makeText(context, R.string.preferences_saved, Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close()
                } catch (_: IOException) {
                }
            }
        }
    }

    fun restorePreferences(context: Context, restoreUri: Uri) {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(restoreUri)
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val editor = sharedPreferences.edit()

            bufferedReader.readLines().forEach { line ->
                val contents =
                    line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (contents.size > 2) {
                    val type = contents[0]
                    val key = contents[1]
                    val value = contents[2]
                    when (type) {
                        "boolean" -> editor.putBoolean(key, java.lang.Boolean.parseBoolean(value))
                        "integer" -> editor.putInt(key, value.toInt())
                        else -> editor.putString(key, value)
                    }
                }
            }

            bufferedReader.close()
            editor.apply()
            Toast.makeText(context, R.string.preferences_restored, Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    val currentDateString: String
        get() = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())
}
