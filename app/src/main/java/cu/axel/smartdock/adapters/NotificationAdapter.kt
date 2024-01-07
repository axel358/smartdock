package cu.axel.smartdock.adapters

import android.app.Notification
import android.app.PendingIntent.CanceledException
import android.content.Context
import android.content.pm.PackageManager
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
import cu.axel.smartdock.icons.IconParserUtilities
import cu.axel.smartdock.utils.AppUtils
import cu.axel.smartdock.utils.ColorUtils
import cu.axel.smartdock.utils.Utils

class NotificationAdapter(private val context: Context, private val iconParserUtilities: IconParserUtilities,
                          private val notifications: Array<StatusBarNotification>, private val listener: OnNotificationClickListener) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {
    private var iconBackground = 0
    private val iconPadding: Int
    private val iconTheming: Boolean

    interface OnNotificationClickListener {
        fun onNotificationClicked(notification: StatusBarNotification, item: View)
        fun onNotificationLongClicked(notification: StatusBarNotification, item: View)
        fun onNotificationCancelClicked(notification: StatusBarNotification, item: View)
    }

    init {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        iconTheming = sp.getString("icon_pack", "") != ""
        iconPadding = Utils.dpToPx(context, sp.getString("icon_padding", "5")!!.toInt())
        when (sp.getString("icon_shape", "circle")) {
            "circle" -> iconBackground = R.drawable.circle
            "round_rect" -> iconBackground = R.drawable.round_square
            "default" -> iconBackground = -1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, arg1: Int): ViewHolder {
        val itemLayoutView = LayoutInflater.from(parent.context).inflate(R.layout.notification_entry, parent,
                false)
        return ViewHolder(itemLayoutView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val sbn = notifications[position]
        val notification = sbn.notification
        val actions = notification.actions
        val extras = notification.extras
        viewHolder.notifActionsLayout.removeAllViews()
        if (actions != null) {
            val lp = LinearLayout.LayoutParams(-2, -2)
            lp.weight = 1f
            if (extras[Notification.EXTRA_MEDIA_SESSION] != null) {
                for (action in actions) {
                    val actionTv = ImageView(context)
                    try {
                        val res = context.packageManager.getResourcesForApplication(sbn.packageName)
                        val drawable = res
                                .getDrawable(res.getIdentifier(action.icon.toString() + "", "drawable", sbn.packageName))
                        drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
                        actionTv.setImageDrawable(drawable)
                        actionTv.setOnClickListener {
                            try {
                                action.actionIntent.send()
                            } catch (_: CanceledException) {
                            }
                        }
                        viewHolder.notifText.isSingleLine = true
                        viewHolder.notifActionsLayout.addView(actionTv, lp)
                    } catch (_: PackageManager.NameNotFoundException) {
                    }
                }
            } else {
                for (action in actions) {
                    val actionTv = TextView(context)
                    actionTv.setTextColor(Color.WHITE)
                    actionTv.isSingleLine = true
                    actionTv.text = action.title
                    actionTv.setOnClickListener {
                        try {
                            action.actionIntent.send()
                        } catch (_: CanceledException) {
                        }
                    }
                    viewHolder.notifActionsLayout.addView(actionTv, lp)
                }
            }
        }
        var notificationTitle = extras.getString(Notification.EXTRA_TITLE)
        if (notificationTitle == null) notificationTitle = AppUtils.getPackageLabel(context, sbn.packageName)
        val notificationText = extras.getCharSequence(Notification.EXTRA_TEXT)
        val progress = extras.getInt(Notification.EXTRA_PROGRESS)
        val p = if (progress != 0) " $progress%" else ""
        viewHolder.notifTitle.text = notificationTitle + p
        viewHolder.notifText.text = notificationText
        if (sbn.isClearable) {
            viewHolder.notifCancelBtn.alpha = 1f
            viewHolder.notifCancelBtn.setOnClickListener { view -> if (sbn.isClearable) listener.onNotificationCancelClicked(sbn, view) }
        } else viewHolder.notifCancelBtn.alpha = 0f
        val notificationIcon = AppUtils.getAppIcon(context, sbn.packageName)
        if (iconTheming) viewHolder.notifIcon.setImageDrawable(iconParserUtilities.getPackageThemedIcon(sbn.packageName)) else viewHolder.notifIcon.setImageDrawable(notificationIcon)
        if (iconBackground != -1) {
            viewHolder.notifIcon.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            viewHolder.notifIcon.setBackgroundResource(iconBackground)
            ColorUtils.applyColor(viewHolder.notifIcon, ColorUtils.getDrawableDominantColor(notificationIcon))
        }
        viewHolder.bind(sbn, listener)
    }

    override fun getItemCount(): Int {
        return notifications.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var notifIcon: ImageView
        var notifCancelBtn: ImageView
        var notifTitle: TextView
        var notifText: TextView
        var notifActionsLayout: LinearLayout

        init {
            notifTitle = itemView.findViewById(R.id.notif_w_title_tv)
            notifText = itemView.findViewById(R.id.notif_w_text_tv)
            notifIcon = itemView.findViewById(R.id.notif_w_icon_iv)
            notifCancelBtn = itemView.findViewById(R.id.notif_w_close_btn)
            notifActionsLayout = itemView.findViewById(R.id.notif_actions_container)
        }

        fun bind(notification: StatusBarNotification, listener: OnNotificationClickListener) {
            itemView.setOnClickListener { view -> listener.onNotificationClicked(notification, view) }
            itemView.setOnLongClickListener { view ->
                listener.onNotificationLongClicked(notification, view)
                true
            }
        }
    }
}