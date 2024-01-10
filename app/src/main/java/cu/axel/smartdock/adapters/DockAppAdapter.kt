package cu.axel.smartdock.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import cu.axel.smartdock.R
import cu.axel.smartdock.models.DockApp
import cu.axel.smartdock.utils.AppUtils
import cu.axel.smartdock.utils.ColorUtils
import cu.axel.smartdock.utils.Utils

class DockAppAdapter(private val context: Context, private val apps: ArrayList<DockApp>,
                     private val listener: OnDockAppClickListener) : RecyclerView.Adapter<DockAppAdapter.ViewHolder>() {
    private var iconBackground = 0
    private val iconPadding: Int
    private val iconTheming: Boolean
    private val tintIndicators: Boolean

    interface OnDockAppClickListener {
        fun onDockAppClicked(app: DockApp, view: View)
        fun onDockAppLongClicked(app: DockApp, view: View)
    }

    init {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        iconTheming = sharedPreferences.getString("icon_pack", "") != ""
        iconPadding = Utils.dpToPx(context, sharedPreferences.getString("icon_padding", "5")!!.toInt())
        tintIndicators = sharedPreferences.getBoolean("tint_indicators", false)
        when (sharedPreferences.getString("icon_shape", "circle")) {
            "circle" -> iconBackground = R.drawable.circle
            "round_rect" -> iconBackground = R.drawable.round_square
            "default" -> iconBackground = -1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, arg1: Int): ViewHolder {
        val itemLayoutView = LayoutInflater.from(parent.context).inflate(R.layout.app_task_entry, null)
        return ViewHolder(itemLayoutView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val app = apps[position]
        val size = app.tasks.size
        if (size > 0) {
            if (tintIndicators) ColorUtils.applyColor(viewHolder.runningIndicator,
                    ColorUtils.manipulateColor(ColorUtils.getDrawableDominantColor(app.icon), 2f))
            if (app.tasks[0].id != -1) viewHolder.runningIndicator.alpha = 1f
            if (app.packageName == AppUtils.currentApp) {
                viewHolder.runningIndicator.layoutParams.width = Utils.dpToPx(context, 16)
            }
            if (size > 1) {
                viewHolder.taskCounter.text = "" + size
                viewHolder.taskCounter.alpha = 1f
            }
        }
        viewHolder.iconIv.setImageDrawable(app.icon)
        if (iconBackground != -1) {
            viewHolder.iconIv.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            viewHolder.iconIv.setBackgroundResource(iconBackground)
            ColorUtils.applyColor(viewHolder.iconIv,
                    ColorUtils.getDrawableDominantColor(viewHolder.iconIv.drawable))
        }
        viewHolder.bind(app, listener)
    }

    override fun getItemCount(): Int {
        return apps.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var iconIv: ImageView
        var taskCounter: TextView
        var runningIndicator: View

        init {
            iconIv = itemView.findViewById(R.id.icon_iv)
            runningIndicator = itemView.findViewById(R.id.running_indicator)
            taskCounter = itemView.findViewById(R.id.task_count_badge)
        }

        fun bind(app: DockApp, listener: OnDockAppClickListener) {
            itemView.setOnClickListener { v -> listener.onDockAppClicked(app, v) }
            itemView.setOnLongClickListener { v ->
                listener.onDockAppLongClicked(app, v)
                true
            }
            itemView.setOnTouchListener { v, event ->
                if (event.buttonState == MotionEvent.BUTTON_SECONDARY) {
                    listener.onDockAppLongClicked(app, v)
                    return@setOnTouchListener true
                }
                false
            }
        }
    }
}