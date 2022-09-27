package cu.axel.smartdock.utils;

import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import cu.axel.smartdock.models.App;
import cu.axel.smartdock.models.AppTask;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.os.SystemClock;
import android.content.pm.PackageManager.NameNotFoundException;
import android.widget.Toast;

public class AppUtils {
    public static final String PINNED_LIST="pinned.lst";
    public static final String DOCK_PINNED_LIST="dock_pinned.lst";
    public static final String DESKTOP_LIST="desktop.lst";
    public static String currentApp = "";

    public static ArrayList<App> getInstalledApps(PackageManager pm) {
        ArrayList<App> apps = new ArrayList<App>();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> appsInfo = pm.queryIntentActivities(intent, 0);

        //TODO: Filter Google App
        for (ResolveInfo appInfo : appsInfo) {
            String label = appInfo.activityInfo.loadLabel(pm).toString();
            Drawable icon = appInfo.activityInfo.loadIcon(pm);
            String packageName = appInfo.activityInfo.packageName;

            apps.add(new App(label, packageName, icon));
        }

        Collections.sort(apps, new Comparator<App>(){

                @Override
                public int compare(App p1, App p2) {
                    return p1.getName().compareToIgnoreCase(p2.getName());
                }


            });

        return apps;
	}

    public static ArrayList<App> getPinnedApps(Context context, PackageManager pm, String type) {
        ArrayList<App> apps = new ArrayList<App>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(context.getFilesDir(), type)));
            String applist="";
            try {
                if ((applist = br.readLine()) != null) {
                    String[] applist2 = applist.split(" ");
                    for (String app:applist2) {
                        try {
                            ApplicationInfo appInfo=pm.getApplicationInfo(app, 0);
                            apps.add(new App(pm.getApplicationLabel(appInfo).toString(), app, pm.getApplicationIcon(app)));
                        } catch (PackageManager.NameNotFoundException e) {
                            //app is no longer available, lets unpin it
                            unpinApp(context, app, type);
                        }   
                    }
                }

            } catch (IOException e) {} 
        } catch (FileNotFoundException e) {}

        return apps;

    }

    public static void pinApp(Context context , String app, String type) {
        try {
            File file=new File(context.getFilesDir(), type);
            FileWriter fw = new FileWriter(file, true);
            fw.write(app + " ");
            fw.close();
        } catch (IOException e) {}

    }
    public static void unpinApp(Context context, String app, String type) {
        try {
            File file=new File(context.getFilesDir(), type);
            BufferedReader br = new BufferedReader(new FileReader(file));
            String applist="";

            if ((applist = br.readLine()) != null) {
                applist = applist.replace(app + " ", "");
                FileWriter fw = new FileWriter(file, false);
                fw.write(applist);
                fw.close();
            }

        } catch (IOException e) {}   
    }
    
    public static void moveApp(Context context, String app, String type, int direction) {
        try {
            File file=new File(context.getFilesDir(), type);
            BufferedReader br = new BufferedReader(new FileReader(file));
            String applist="", what="", with="";

            if ((applist = br.readLine()) != null) {
                String apps[] = applist.split(" ");
                int pos = findInArray(app, apps);
                if(direction == 0 && pos > 0) {
                    what = apps[pos -1] + " " + app;
                    with = app + " " + apps[pos -1];
                }else if(direction == 1 && pos < apps.length - 1) {
                    what = app + " " + apps[pos+1];
                    with = apps[pos+1] + " " + app;
                }
                applist = applist.replace(what, with);
                FileWriter fw = new FileWriter(file, false);
                fw.write(applist);
                fw.close();
            }

        } catch (IOException e) {} 
    }
    
    public static int findInArray(String key, String[] array){
        for(int i=0; i < array.length; i++){
            if(array[i].contains(key))
                return i;
        }
        return -1;
    }

    public static boolean isPinned(Context context, String app, String type) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(context.getFilesDir(), type)));
            String applist="";

            if ((applist = br.readLine()) != null) {
                return applist.contains(app);
            }

        } catch (IOException e) {}
        return    false; 
    }

    public static boolean isGame(PackageManager pm, String packageName) {
        try {
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return info.category == ApplicationInfo.CATEGORY_GAME;
            } else {
                return (info.flags & ApplicationInfo.FLAG_IS_GAME) == ApplicationInfo.FLAG_IS_GAME;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static String getCurrentLauncher(PackageManager pm) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo.activityInfo.packageName;
    }

    public static void setWindowMode(ActivityManager am, int taskId, int mode) {
        try {
            Method setWindowMode = am.getClass().getMethod("setTaskWindowingMode", int.class, int.class, boolean.class);
            setWindowMode.invoke(am, taskId, mode, false);
        } catch (Exception e) {
        }
    }

    public static ArrayList<AppTask> getRunningTasks(ActivityManager am, PackageManager pm, int max) {
        List<ActivityManager.RunningTaskInfo> tasksInfo = am.getRunningTasks(max);
        currentApp = tasksInfo.get(0).baseActivity.getPackageName();

        ArrayList<AppTask> appTasks = new ArrayList<AppTask>();
        for (ActivityManager.RunningTaskInfo taskInfo : tasksInfo) {
            try {
                //Exclude systemui, launcher and other system apps from the tasklist
                if (taskInfo.baseActivity.getPackageName().contains("com.android.systemui") 
                    || taskInfo.baseActivity.getPackageName().contains("com.google.android.packageinstaller"))
                    continue;

                //Hack to save Dock settings activity ftom being excluded
                if (!(taskInfo.topActivity.getClassName().equals("cu.axel.smartdock.activities.MainActivity") || 
                    taskInfo.topActivity.getClassName().equals("cu.axel.smartdock.activities.DebugActivity"))
                    && taskInfo.topActivity.getPackageName().equals(AppUtils.getCurrentLauncher(pm)))
                    continue;

                if (Build.VERSION.SDK_INT > 29) {
                    try {
                        Field isRunning = taskInfo.getClass().getField("isRunning");
                        boolean running= isRunning.getBoolean(taskInfo);
                        if (!running)
                            continue;
                    } catch (Exception e) {}
                }

                appTasks.add(new AppTask(taskInfo.id, pm.getActivityInfo(taskInfo.topActivity, 0).loadLabel(pm).toString(), taskInfo.topActivity.getPackageName(), pm.getActivityIcon(taskInfo.topActivity)));
            } catch (PackageManager.NameNotFoundException e) {}
        }

        return appTasks;
    }
    
    public static ArrayList<AppTask> getRecentTasks(Context context, int max) {
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        long start = System.currentTimeMillis() - SystemClock.elapsedRealtime();
        List<UsageStats> usageStats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, System.currentTimeMillis());
        ArrayList<AppTask> appTasks = new ArrayList<>();
        
        Collections.sort(usageStats, new Comparator<UsageStats>(){

                @Override
                public int compare(UsageStats p1, UsageStats p2) {
                    return  Long.compare(p2.getLastTimeUsed(), p1.getLastTimeUsed());
                }
            });

        for(UsageStats stat : usageStats) {
            String app = stat.getPackageName();
            try {
                if(isLaunchable(context, app) && !app.equals(AppUtils.getCurrentLauncher(context.getPackageManager())))
                    appTasks.add(new AppTask(-1, getPackageLabel(context, app), app, context.getPackageManager().getApplicationIcon(app)));
            } catch (PackageManager.NameNotFoundException e) {}
            
            if(appTasks.size() > max)
                break;
        }

        return appTasks;
    }
    
    public static boolean isSystemApp(Context context, String app) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(app, 0);
            return (appInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    
    private static boolean isLaunchable(Context context, String app) {
        List<ResolveInfo> resolveInfo = context.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(app), 0);
        return resolveInfo != null && resolveInfo.size() > 0;
    }
    
    public static void removeTask(ActivityManager am, int id){
        try {
            Method removeTask = am.getClass().getMethod("removeTask", int.class);
            removeTask.invoke(am, id);
        } catch (Exception e) {
            Log.e("Dock", e.toString()+e.getCause().toString());
        }
    }
    
    public static String getPackageLabel(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo appInfo=pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(appInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
        }
        return "";
    }
}
