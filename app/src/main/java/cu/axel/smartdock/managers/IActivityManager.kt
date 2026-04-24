package cu.axel.smartdock.managers

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.os.IBinder
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.Method

@SuppressLint("PrivateApi")
class IActivityManager {

    private var getRunningTasksMethod: Method
    private var iAmInstance: Any

    init {
        val iAmClass = Class.forName("android.app.IActivityManager")
        val iAmStub = Class.forName("android.app.IActivityManager\$Stub")
        val asInterfaceMethod = iAmStub.getMethod("asInterface", IBinder::class.java)
        getRunningTasksMethod = iAmClass.getMethod("getTasks", Int::class.java)
        iAmInstance = asInterfaceMethod.invoke(
            null,
            ShizukuBinderWrapper(SystemServiceHelper.getSystemService("activity"))
        )!!
    }

    fun getRunningTasks(max: Int): List<ActivityManager.RunningTaskInfo> {
        val tasks = getRunningTasksMethod.invoke(
            iAmInstance,
            max
        ) as List<ActivityManager.RunningTaskInfo>
        return tasks
    }
}