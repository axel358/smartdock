package cu.axel.smartdock.models

import android.graphics.drawable.Drawable

open class App(val name: String, val packageName: String, open val icon: Drawable) {
    override fun toString(): String {
        return name
    }
}
