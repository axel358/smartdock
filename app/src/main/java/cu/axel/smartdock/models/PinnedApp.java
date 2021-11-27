package cu.axel.smartdock.models;
import java.util.ArrayList;

public class PinnedApp {
    private String packageName;
    private ArrayList<Integer> runningTaskIDs;

    public PinnedApp(String packageName) {
        this.packageName = packageName;
        runningTaskIDs = new ArrayList<Integer>();
    }

    public String getPackageName() {
        return packageName;
    }

    public ArrayList<Integer> getRunningTaskIDs() {
        return runningTaskIDs;
    }

    public void addTaskID(int id) {
        runningTaskIDs.add(id);
    }
}
