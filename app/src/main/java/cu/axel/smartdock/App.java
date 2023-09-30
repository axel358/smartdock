package cu.axel.smartdock;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.google.android.material.color.DynamicColors;

import cu.axel.smartdock.activities.DebugActivity;

public class App extends Application {
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    @Override
    public void onCreate() {
        DynamicColors.applyToActivitiesIfAvailable(this);
        uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {

            StringBuilder report = new StringBuilder("Exception: " + exception + "\n");

            for (StackTraceElement element : exception.getStackTrace())
                report.append(element.toString()).append("\n");

            Throwable cause = exception.getCause();

            if (cause != null) {
                report.append("Cause: ").append(cause).append("\n");
                for (StackTraceElement element : cause.getStackTrace())
                    report.append(element.toString()).append("\n");
            }

            String message = exception.getMessage();
            if (message != null)
                report.append("Message: ").append(message);

            Intent intent = new Intent(getApplicationContext(), DebugActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("report", report.toString());
            //startActivity(intent);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 11111, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000, pendingIntent);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(2);
            uncaughtExceptionHandler.uncaughtException(thread, exception);
        });
        super.onCreate();
    }

}
