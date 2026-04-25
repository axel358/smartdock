package android.app;

interface IActivityManager{
    List<android.app.ActivityManager.RunningTaskInfo> getTasks(int maxNum);
}