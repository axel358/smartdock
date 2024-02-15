package cu.axel.smartdock.models

import android.content.ComponentName
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle

open class App(val name: String, val packageName: String, open val icon: Drawable,
               val componentName: ComponentName? = null, val userHandle: UserHandle = Process.myUserHandle()) {
    override fun toString(): String {
        return name
    }
}
