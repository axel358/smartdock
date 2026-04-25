package android.app;

interface IActivityManager {

    List<android.app.ActivityManager.RunningTaskInfo> getTasks(int maxNum);

    void resizeTask(int taskId, in Rect bounds, int resizeMode);

    boolean removeTask(int taskId);
}