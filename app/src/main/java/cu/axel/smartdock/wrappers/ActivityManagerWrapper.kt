package cu.axel.smartdock.wrappers

import android.app.ActivityManager
import android.app.IActivityManager
import android.graphics.Rect
import android.os.IBinder
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

class ActivityManagerWrapper {

    private var binder: ShizukuBinderWrapper? = null
    private var activityManager: IActivityManager? = null

    private val deathRecipient = IBinder.DeathRecipient {
        synchronized(this) {
            activityManager = null
            binder = null
        }
    }

    init {
        init()
    }

    fun init() {
        synchronized(this) {
            if (activityManager != null)
                return
            binder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("activity"))
            binder?.linkToDeath(deathRecipient, 0)
            activityManager = IActivityManager.Stub.asInterface(binder)
        }
    }

    fun isAlive() = binder?.isBinderAlive == true && activityManager != null

    fun getRunningTasks(max: Int): List<ActivityManager.RunningTaskInfo> {
        val tasks = activityManager?.getTasks(max)
        return tasks ?: emptyList()
    }

    fun resizeTask(taskId: Int, bounds: Rect){
        activityManager?.resizeTask(taskId, bounds, 1)
    }
}