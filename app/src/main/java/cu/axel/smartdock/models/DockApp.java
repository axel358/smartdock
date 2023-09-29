package cu.axel.smartdock.models;

import java.util.ArrayList;

import android.graphics.drawable.Drawable;

public class DockApp extends App {
    private final ArrayList<AppTask> tasks;

    public DockApp(String name, String packageName, Drawable icon) {
        super(name, packageName, icon);

        tasks = new ArrayList<>();
    }

    public DockApp(AppTask task) {
        super(task.getName(), task.getPackageName(), task.getIcon());
        tasks = new ArrayList<>();
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
        return tasks.size() == 1 ? tasks.get(0).getIcon() : super.getIcon();
    }
}
