package cu.axel.smartdock.adapters

import android.app.Notification
import android.app.PendingIntent.CanceledException
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PorterDuff
import android.service.notification.StatusBarNotification
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import cu.axel.smartdock.R
import cu.axel.smartdock.utils.AppUtils
import cu.axel.smartdock.utils.ColorUtils
import cu.axel.smartdock.utils.Utils

class NotificationAdapter(
    private val context: Context,
    private var notifications: Array<StatusBarNotification>,
    private val listener: OnNotificationClickListener
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {
    private var sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)
    private val actionsHeight = Utils.dpToPx(context, 20)

    interface OnNotificationClickListener {
        fun onNotificationClicked(notification: StatusBarNotification, item: View)
        fun onNotificationLongClicked(notification: StatusBarNotification, item: View)
        fun onNotificationCancelClicked(notification: StatusBarNotification, item: View)
    }

    override fun onCreateViewHolder(parent: ViewGroup, arg1: Int): ViewHolder {
        val itemLayoutView = LayoutInflater.from(parent.context).inflate(
            R.layout.notification_entry, parent, false
        )
        return ViewHolder(itemLayoutView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val sbn = notifications[position]
        val notification = sbn.notification
        val actions = notification.actions
        val extras = notification.extras
        viewHolder.notifActionsLayout.removeAllViews()
        if (actions != null) {
            val actionLayoutParams = LinearLayout.LayoutParams(
                0,
                actionsHeight
            )
            actionLayoutParams.weight = 1f
            if (AppUtils.isMediaNotification(notification)) {
                for (action in actions) {
                    val actionIv = ImageView(context)
                    val resources = context.packageManager
                        .getResourcesForApplication(sbn.packageName)
                    val drawable = resources.getDrawable(
                        resources.getIdentifier(
                            action.icon.toString() + "",
                            "drawable",
                            sbn.packageName
                        )
                    )
                    drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
                    actionIv.setImageDrawable(drawable)
                    actionIv.setOnClickListener {
                        try {
                            action.actionIntent.send()
                        } catch (_: CanceledException) {
                        }
                    }
                    viewHolder.notifText.isSingleLine = true
                    viewHolder.notifActionsLayout.addView(actionIv, actionLayoutParams)
                }
            } else {
                for (action in actions) {
                    val actionTv = TextView(context)
                    actionTv.setTextColor(context.getColor(R.color.action))
                    actionTv.isSingleLine = true
                    actionTv.text = action.title
                    actionTv.setOnClickListener {
                        try {
                            action.actionIntent.send()
                        } catch (_: CanceledException) {
                        }
                    }
                    viewHolder.notifActionsLayout.addView(actionTv, actionLayoutParams)
                }
            }
        }
        var notificationTitle = extras.getString(Notification.EXTRA_TITLE)
        if (notificationTitle == null) notificationTitle =
            AppUtils.getPackageLabel(context, sbn.packageName)
        val notificationText = extras.getCharSequence(Notification.EXTRA_TEXT)
        val progress = extras.getInt(Notification.EXTRA_PROGRESS)
        val formattedProgress = if (progress != 0) " $progress%" else ""
        viewHolder.notifTitle.text = notificationTitle + formattedProgress
        viewHolder.notifText.text = notificationText
        if (sbn.isClearable) {
            viewHolder.notifCancelBtn.alpha = 1f
            viewHolder.notifCancelBtn.setOnClickListener { view ->
                listener.onNotificationCancelClicked(sbn, view)
            }
        } else viewHolder.notifCancelBtn.alpha = 0f

        if (AppUtils.isMediaNotification(notification) && notification.getLargeIcon() != null) {
            val padding = Utils.dpToPx(context, 0)
            viewHolder.notifIcon.setPadding(padding, padding, padding, padding)
            viewHolder.notifIcon.setImageIcon(notification.getLargeIcon())
        } else {
            notification.smallIcon.setTint(Color.WHITE)
            viewHolder.notifIcon.setBackgroundResource(R.drawable.circle)
            ColorUtils.applySecondaryColor(
                context, sharedPreferences,
                viewHolder.notifIcon
            )
            val padding = Utils.dpToPx(context, 14)
            viewHolder.notifIcon.setPadding(padding, padding, padding, padding)
            viewHolder.notifIcon.setImageIcon(notification.smallIcon)
        }

        viewHolder.bind(sbn, listener)
    }

    override fun getItemCount(): Int {
        return notifications.size
    }

    fun updateNotifications(newNotifications: Array<StatusBarNotification>){
        notifications = newNotifications
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var notifIcon: ImageView = itemView.findViewById(R.id.notification_icon_iv)
        var notifCancelBtn: ImageView = itemView.findViewById(R.id.notification_close_btn)
        var notifTitle: TextView = itemView.findViewById(R.id.notification_title_tv)
        var notifText: TextView = itemView.findViewById(R.id.notification_text_tv)
        var notifActionsLayout: LinearLayout = itemView.findViewById(R.id.notification_actions_layout)

        fun bind(notification: StatusBarNotification, listener: OnNotificationClickListener) {
            itemView.setOnClickListener { view ->
                listener.onNotificationClicked(notification, view)
            }
            itemView.setOnLongClickListener { view ->
                listener.onNotificationLongClicked(notification, view)
                true
            }
        }
    }
}