package cu.axel.smartdock

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import com.google.android.material.color.DynamicColors
import cu.axel.smartdock.activities.DebugActivity
import kotlin.system.exitProcess
import android.os.Process

class App : Application() {
    private lateinit var uncaughtExceptionHandler: Thread.UncaughtExceptionHandler
    override fun onCreate() {
        DynamicColors.applyToActivitiesIfAvailable(this)
        uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()!!
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            val report = StringBuilder("Exception: $exception\n")
            for (element in exception.stackTrace) report.append(element.toString()).append("\n")
            val cause = exception.cause
            if (cause != null) {
                report.append("Cause: ").append(cause).append("\n")
                for (element in cause.stackTrace) report.append(element.toString()).append("\n")
            }
            val message = exception.message
            if (message != null) report.append("Message: ").append(message)
            val intent = Intent(applicationContext, DebugActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.putExtra("report", report.toString())
            //startActivity(intent);
            val pendingIntent = PendingIntent.getActivity(applicationContext, 11111, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
            val am = getSystemService(ALARM_SERVICE) as AlarmManager
            am[AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000] = pendingIntent
            Process.killProcess(Process.myPid())
            exitProcess(2)
            uncaughtExceptionHandler.uncaughtException(thread, exception)
        }
        super.onCreate()
    }
}
