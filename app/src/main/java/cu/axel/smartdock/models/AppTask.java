package cu.axel.smartdock.models;
import android.graphics.drawable.Drawable;
import java.util.ArrayList;

public class AppTask {
	private String packageName;
	private Drawable icon;
	private ArrayList<Integer> taskIDs;

	public AppTask(int id, String packageName, Drawable icon) {
        taskIDs = new ArrayList<Integer>();
		this.packageName = packageName;
		this.icon = icon;

        if (id != -1)
            taskIDs.add(id);
	}

	public ArrayList<Integer> getIds() {
		return  taskIDs;
	}

    public void addTask(int id) {
        taskIDs.add(id);
    }

	public String getPackageName() {
		return packageName;
	}

	public Drawable getIcon() {
		return icon;
	}

}
