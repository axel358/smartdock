package cu.axel.smartdock.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.LinearLayout

class HoverInterceptorLayout(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    override fun onInterceptHoverEvent(event: MotionEvent): Boolean {
        return true
    }
}
