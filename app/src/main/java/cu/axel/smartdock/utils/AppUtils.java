package cu.axel.smartdock.utils;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import cu.axel.smartdock.models.App;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.content.Context;

public class AppUtils {
    private static final String FILES_DIR = "/data/data/cu.axel.smartdock/files";
    private static final String PINNED_LIST=FILES_DIR + "/pinned.lst";

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

    public static ArrayList<App> getFavoriteApps(PackageManager pm) {
        ArrayList<App> apps = new ArrayList<App>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(PINNED_LIST));
            String applist="";
            try {
                if ((applist = br.readLine()) != null) {
                    String[] applist2 = applist.split(" ");
                    for (String app:applist2) {
                        try {
                            ApplicationInfo appInfo=pm.getApplicationInfo(app, 0);
                            apps.add(new App(pm.getApplicationLabel(appInfo).toString(), app, pm.getApplicationIcon(app)));
                        } catch (PackageManager.NameNotFoundException e) {
                        }   
                    }
                }

            } catch (IOException e) {} 
        } catch (FileNotFoundException e) {}

        return apps;

    }

    public static void pinApp(String app) {
        try {
            File dir=new File(FILES_DIR);
            if (!dir.exists())
                dir.mkdir();
            BufferedWriter bw = new BufferedWriter(new FileWriter(PINNED_LIST, true));
            bw.write(app + " ");
            bw.close();
        } catch (IOException e) {}

    }
    public static void unpinApp(String app) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(PINNED_LIST));
            String applist="";

            if ((applist = br.readLine()) != null) {
                applist = applist.replace(app + " ", "");
                BufferedWriter bw = new BufferedWriter(new FileWriter(PINNED_LIST, false));
                bw.write(applist);
                bw.close();
            }

        } catch (IOException e) {}   
    }

    public static   boolean isPinned(String app) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(PINNED_LIST));
            String applist="";

            if ((applist = br.readLine()) != null) {
                return applist.contains(app);
            }

        } catch (IOException e) {}
        return    false; 
    }

    public static boolean isGame(PackageManager pm, String packageName) {
        try {
            ApplicationInfo info = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            return (info.flags & ApplicationInfo.FLAG_IS_GAME) != 0 || (info.metaData != null && info.metaData.getBoolean("isGame", false));
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
