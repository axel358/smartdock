package cu.axel.smartdock.utils

import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.atan2

open class OnSwipeListener : GestureDetector.SimpleOnGestureListener() {
    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        if (e1 != null) {
            val x1 = e1.x
            val y1 = e1.y
            val x2 = e2.x
            val y2 = e2.y
            val direction = getDirection(x1, y1, x2, y2)
            return onSwipe(direction)
        }
        return false
    }


    open fun onSwipe(direction: Direction): Boolean {
        return false
    }

    private fun getDirection(x1: Float, y1: Float, x2: Float, y2: Float): Direction {
        val angle = getAngle(x1, y1, x2, y2)
        return Direction.fromAngle(angle)
    }

    private fun getAngle(x1: Float, y1: Float, x2: Float, y2: Float): Double {
        val rad = atan2((y1 - y2).toDouble(), (x2 - x1).toDouble()) + Math.PI
        return (rad * 180 / Math.PI + 180) % 360
    }

    enum class Direction {
        UP,
        DOWN,
        LEFT,
        RIGHT;

        companion object {
            fun fromAngle(angle: Double): Direction {
                return if (inRange(angle, 45f, 135f)) {
                    UP
                } else if (inRange(angle, 0f, 45f) || inRange(angle, 315f, 360f)) {
                    RIGHT
                } else if (inRange(angle, 225f, 315f)) {
                    DOWN
                } else {
                    LEFT
                }
            }

            private fun inRange(angle: Double, init: Float, end: Float): Boolean {
                return angle >= init && angle < end
            }
        }
    }
}
