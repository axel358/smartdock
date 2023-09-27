package cu.axel.smartdock.models;

import java.util.ArrayList;

import android.graphics.drawable.Drawable;

public class DockApp extends App {
    private ArrayList<AppTask> tasks;

    public DockApp(String name, String packageName, Drawable icon) {
        super(name, packageName, icon);

        tasks = new ArrayList<AppTask>();
    }

    public DockApp(AppTask task) {
        super(task.getName(), task.getPackageName(), task.getIcon());
        tasks = new ArrayList<AppTask>();
        tasks.add(task);
    }

    public void addTask(AppTask task) {
        tasks.add(task);
    }

    public ArrayList<AppTask> getTasks() {
        return tasks;
    }

    @Override
    public Drawable getIcon() {
        if (tasks.size() > 0 && tasks.size() < 2)
            return tasks.get(0).getIcon();

        return super.getIcon();
    }
}
