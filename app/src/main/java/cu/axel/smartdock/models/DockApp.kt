package cu.axel.smartdock.models

import android.graphics.drawable.Drawable

class DockApp : App {
    val tasks: ArrayList<AppTask>

    constructor(name: String, packageName: String, icon: Drawable) : super(name, packageName, icon) {
        tasks = ArrayList()
    }

    constructor(task: AppTask) : super(task.name, task.packageName, task.icon) {
        tasks = ArrayList()
        tasks.add(task)
    }

    fun addTask(task: AppTask) {
        tasks.add(task)
    }

    override val icon: Drawable
        get() = if (tasks.size == 1) tasks[0].icon else super.icon
}
