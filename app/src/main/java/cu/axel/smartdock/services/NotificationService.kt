package cu.axel.smartdock.services

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Notification
import android.app.PendingIntent.CanceledException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.divider.MaterialDividerItemDecoration
import cu.axel.smartdock.R
import cu.axel.smartdock.activities.LAUNCHER_ACTION
import cu.axel.smartdock.adapters.NotificationAdapter
import cu.axel.smartdock.adapters.NotificationAdapter.OnNotificationClickListener
import cu.axel.smartdock.utils.AppUtils
import cu.axel.smartdock.utils.ColorUtils
import cu.axel.smartdock.utils.DeviceUtils
import cu.axel.smartdock.utils.Utils
import cu.axel.smartdock.widgets.HoverInterceptorLayout

const val ACTION_HIDE_NOTIFICATION_PANEL = "hide_panel"
const val ACTION_SHOW_NOTIFICATION_PANEL = "show_panel"
const val NOTIFICATION_COUNT_CHANGED = "count_changed"
const val NOTIFICATION_SERVICE_ACTION = "notification_service_action"

class NotificationService : NotificationListenerService(), OnNotificationClickListener,
    SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var windowManager: WindowManager
    private lateinit var notificationLayout: HoverInterceptorLayout
    private lateinit var notifTitle: TextView
    private lateinit var notifText: TextView
    private lateinit var notifIcon: ImageView
    private lateinit var notifCancelBtn: ImageView
    private lateinit var handler: Handler
    private lateinit var sharedPreferences: SharedPreferences
    private var notificationPanel: View? = null
    private var notificationsLv: RecyclerView? = null
    private var cancelAllBtn: ImageButton? = null
    private var notifActionsLayout: LinearLayout? = null
    private lateinit var context: Context
    private var notificationArea: LinearLayout? = null
    private var preferLastDisplay = false
    private var y = 0
    private var margins = 0
    private var dockHeight: Int = 0
    private lateinit var notificationLayoutParams: WindowManager.LayoutParams
    override fun onCreate() {
        super.onCreate()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        preferLastDisplay = sharedPreferences.getBoolean("prefer_last_display", false)
        context = DeviceUtils.getDisplayContext(this, preferLastDisplay)
        windowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
        notificationLayoutParams = Utils.makeWindowParams(
            Utils.dpToPx(context, 300), -2, context,
            preferLastDisplay
        )
        margins = Utils.dpToPx(context, 2)
        dockHeight =
            Utils.dpToPx(context, sharedPreferences.getString("dock_height", "56")!!.toInt())
        y = (if (DeviceUtils.shouldApplyNavbarFix())
            dockHeight - DeviceUtils.getNavBarHeight(context)
        else
            dockHeight) + margins
        notificationLayoutParams.x = margins
        notificationLayoutParams.gravity = Gravity.BOTTOM or Gravity.END
        notificationLayoutParams.y = y
        notificationLayout = LayoutInflater.from(this).inflate(
            R.layout.notification_popup,
            null
        ) as HoverInterceptorLayout
        notificationLayout.visibility = View.GONE
        notifTitle = notificationLayout.findViewById(R.id.notif_title_tv)
        notifText = notificationLayout.findViewById(R.id.notif_text_tv)
        notifIcon = notificationLayout.findViewById(R.id.notif_icon_iv)
        notifCancelBtn = notificationLayout.findViewById(R.id.notif_close_btn)
        notifActionsLayout = notificationLayout.findViewById(R.id.notif_actions_container2)
        windowManager.addView(notificationLayout, notificationLayoutParams)
        handler = Handler(Looper.getMainLooper())
        notificationLayout.alpha = 0f
        notificationLayout.setOnHoverListener { _, event ->
            if (event.action == MotionEvent.ACTION_HOVER_ENTER) {
                notifCancelBtn.visibility = View.VISIBLE
                handler.removeCallbacksAndMessages(null)
            } else if (event.action == MotionEvent.ACTION_HOVER_EXIT) {
                Handler(Looper.getMainLooper()).postDelayed({
                    notifCancelBtn.visibility = View.INVISIBLE
                }, 200)
                hideNotification()
            }
            false
        }
        val dockReceiver = DockServiceReceiver()
        ContextCompat.registerReceiver(
            this,
            dockReceiver,
            IntentFilter(DOCK_SERVICE_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        updateNotificationCount()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        updateNotificationCount()
        if (Utils.notificationPanelVisible)
            updateNotificationPanel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        updateNotificationCount()
        if (Utils.notificationPanelVisible) {
            updateNotificationPanel()
        } else {
            if (sharedPreferences.getBoolean("show_notifications", true)) {
                val notification = sbn.notification
                if (sbn.isOngoing
                    && !PreferenceManager.getDefaultSharedPreferences(this)
                        .getBoolean("show_ongoing", false)
                ) {
                } else if (notification.contentView == null && !isBlackListed(sbn.packageName)
                    && !(sbn.packageName == AppUtils.currentApp && sharedPreferences.getBoolean(
                        "show_current",
                        true
                    ))
                ) {
                    val extras = notification.extras
                    var notificationTitle = extras.getString(Notification.EXTRA_TITLE)
                    if (notificationTitle == null) notificationTitle =
                        AppUtils.getPackageLabel(context, sbn.packageName)
                    val notificationText = extras.getCharSequence(Notification.EXTRA_TEXT)
                    ColorUtils.applyMainColor(
                        this@NotificationService,
                        sharedPreferences,
                        notificationLayout
                    )
                    ColorUtils.applySecondaryColor(
                        this@NotificationService,
                        sharedPreferences,
                        notifCancelBtn
                    )
                    val notificationIcon = AppUtils.getAppIcon(context, sbn.packageName)
                    notifIcon.setImageDrawable(notificationIcon)
                    val iconPadding = Utils.dpToPx(
                        context,
                        sharedPreferences.getString("icon_padding", "5")!!.toInt()
                    )
                    var iconBackground = -1
                    when (sharedPreferences.getString("icon_shape", "circle")) {
                        "circle" -> iconBackground = R.drawable.circle
                        "round_rect" -> iconBackground = R.drawable.round_square
                    }
                    notifIcon.setImageDrawable(notificationIcon)
                    if (iconBackground != -1) {
                        notifIcon.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
                        notifIcon.setBackgroundResource(iconBackground)
                        ColorUtils.applyColor(
                            notifIcon,
                            ColorUtils.getDrawableDominantColor(notificationIcon)
                        )
                    }
                    val progress = extras.getInt(Notification.EXTRA_PROGRESS)
                    val p = if (progress != 0) " $progress%" else ""
                    notifTitle.text = notificationTitle + p
                    notifText.text = notificationText
                    val actions = notification.actions
                    notifActionsLayout!!.removeAllViews()
                    if (actions != null) {
                        val layoutParams = LinearLayout.LayoutParams(-2, -2)
                        layoutParams.weight = 1f
                        if (extras[Notification.EXTRA_MEDIA_SESSION] != null) {
                            for (action in actions) {
                                val actionTv = ImageView(this@NotificationService)
                                try {
                                    val res = packageManager
                                        .getResourcesForApplication(sbn.packageName)
                                    val drawable = res.getDrawable(
                                        res.getIdentifier(
                                            action.icon.toString() + "",
                                            "drawable",
                                            sbn.packageName
                                        )
                                    )
                                    drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
                                    actionTv.setImageDrawable(drawable)
                                    //actionTv.setImageIcon(action.getIcon());
                                    actionTv.setOnClickListener {
                                        try {
                                            action.actionIntent.send()
                                        } catch (_: CanceledException) {
                                        }
                                    }
                                    notifText.isSingleLine = true
                                    notifActionsLayout!!.addView(actionTv, layoutParams)
                                } catch (_: PackageManager.NameNotFoundException) {
                                }
                            }
                        } else {
                            for (action in actions) {
                                val actionTv = TextView(this@NotificationService)
                                actionTv.isSingleLine = true
                                actionTv.text = action.title
                                actionTv.setTextColor(Color.WHITE)
                                actionTv.setOnClickListener {
                                    try {
                                        action.actionIntent.send()
                                        notificationLayout.visibility = View.GONE
                                        notificationLayout.alpha = 0f
                                    } catch (_: CanceledException) {
                                    }
                                }
                                notifActionsLayout!!.addView(actionTv, layoutParams)
                            }
                        }
                    }
                    notifCancelBtn.setOnClickListener {
                        notificationLayout.visibility = View.GONE
                        if (sbn.isClearable) cancelNotification(sbn.key)
                    }
                    notificationLayout.setOnClickListener {
                        notificationLayout.visibility = View.GONE
                        notificationLayout.alpha = 0f
                        val intent = notification.contentIntent
                        if (intent != null) {
                            try {
                                intent.send()
                                if (sbn.isClearable) cancelNotification(sbn.key)
                            } catch (_: CanceledException) {
                            }
                        }
                    }
                    notificationLayout.setOnLongClickListener {
                        sharedPreferences.edit()
                            .putString("blocked_notifications",
                                sharedPreferences.getString("blocked_notifications", "")!!
                                    .trim { it <= ' ' } + " " + sbn.packageName)
                            .apply()
                        notificationLayout.visibility = View.GONE
                        notificationLayout.alpha = 0f
                        Toast.makeText(
                            this@NotificationService,
                            R.string.silenced_notifications,
                            Toast.LENGTH_LONG
                        )
                            .show()
                        if (sbn.isClearable) cancelNotification(sbn.key)
                        true
                    }
                    notificationLayout.animate().alpha(1f).setDuration(300)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationStart(animation: Animator) {
                                notificationLayout.visibility = View.VISIBLE
                            }
                        })
                    if (sharedPreferences.getBoolean(
                            "enable_notification_sound",
                            false
                        )
                    ) DeviceUtils.playEventSound(this, "notification_sound")
                    hideNotification()
                }
            }
        }
    }

    private fun hideNotification() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            notificationLayout.animate().alpha(0f).setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        notificationLayout.visibility = View.GONE
                    }
                })
        }, sharedPreferences.getString("notification_timeout", "5000")!!.toInt().toLong())
    }

    private fun isBlackListed(packageName: String): Boolean {
        val ignoredPackages = sharedPreferences.getString("blocked_notifications", "android")
        return ignoredPackages!!.contains(packageName)
    }

    private fun updateNotificationCount() {
        var count = 0
        var cancelableCount = 0
        val notifications = activeNotifications
        for (notification in notifications) {
            if (notification != null && notification.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0) {
                count++
                if (notification.isClearable) cancelableCount++
            }
            if (Utils.notificationPanelVisible) cancelAllBtn!!.visibility =
                if (cancelableCount > 0) View.VISIBLE else View.INVISIBLE
        }
        sendBroadcast(
            Intent(NOTIFICATION_SERVICE_ACTION)
                .setPackage(packageName)
                .putExtra("action", NOTIFICATION_COUNT_CHANGED)
                .putExtra("count", count)
        )
    }

    fun showNotificationPanel() {
        val layoutParams = Utils.makeWindowParams(
            Utils.dpToPx(context, 400), -2, context,
            preferLastDisplay
        )
        layoutParams.gravity = Gravity.BOTTOM or Gravity.END
        layoutParams.y = y
        layoutParams.x = margins
        layoutParams.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        notificationPanel = LayoutInflater.from(context).inflate(R.layout.notification_panel, null)
        cancelAllBtn = notificationPanel!!.findViewById(R.id.cancel_all_n_btn)
        notificationsLv = notificationPanel!!.findViewById(R.id.notification_lv)
        notificationsLv!!.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        notificationArea = notificationPanel!!.findViewById(R.id.notification_area)
        val qsArea = notificationPanel!!.findViewById<LinearLayout>(R.id.qs_area)
        val notificationsBtn = notificationPanel!!.findViewById<ImageView>(R.id.notifications_btn)
        val orientationBtn = notificationPanel!!.findViewById<ImageView>(R.id.btn_orientation)
        val touchModeBtn = notificationPanel!!.findViewById<ImageView>(R.id.btn_touch_mode)
        val screenshotBtn = notificationPanel!!.findViewById<ImageView>(R.id.btn_screenshot)
        val screencapBtn = notificationPanel!!.findViewById<ImageView>(R.id.btn_screencast)
        val settingsBtn = notificationPanel!!.findViewById<ImageView>(R.id.btn_settings)
        ColorUtils.applySecondaryColor(context, sharedPreferences, notificationsBtn)
        ColorUtils.applySecondaryColor(context, sharedPreferences, orientationBtn)
        ColorUtils.applySecondaryColor(context, sharedPreferences, touchModeBtn)
        ColorUtils.applySecondaryColor(context, sharedPreferences, screencapBtn)
        ColorUtils.applySecondaryColor(context, sharedPreferences, screenshotBtn)
        ColorUtils.applySecondaryColor(context, sharedPreferences, settingsBtn)
        touchModeBtn.setOnClickListener {
            hideNotificationPanel()
            if (sharedPreferences.getBoolean("tablet_mode", false)) {
                Utils.toggleBuiltinNavigation(sharedPreferences.edit(), false)
                sharedPreferences.edit().putBoolean("app_menu_fullscreen", false).apply()
                sharedPreferences.edit().putBoolean("tablet_mode", false).apply()
                Toast.makeText(context, R.string.tablet_mode_off, Toast.LENGTH_SHORT).show()
            } else {
                Utils.toggleBuiltinNavigation(sharedPreferences.edit(), true)
                sharedPreferences.edit().putBoolean("app_menu_fullscreen", true).apply()
                sharedPreferences.edit().putBoolean("tablet_mode", true).apply()
                Toast.makeText(context, R.string.tablet_mode_on, Toast.LENGTH_SHORT).show()
            }
        }
        orientationBtn.setImageResource(
            if (sharedPreferences.getBoolean(
                    "lock_landscape",
                    true
                )
            ) R.drawable.ic_screen_rotation_off else R.drawable.ic_screen_rotation_on
        )
        orientationBtn.setOnClickListener {
            sharedPreferences.edit()
                .putBoolean("lock_landscape", !sharedPreferences.getBoolean("lock_landscape", true))
                .apply()
            orientationBtn
                .setImageResource(
                    if (sharedPreferences.getBoolean(
                            "lock_landscape",
                            true
                        )
                    ) R.drawable.ic_screen_rotation_off else R.drawable.ic_screen_rotation_on
                )
        }
        screenshotBtn.setOnClickListener {
            hideNotificationPanel()
            sendBroadcast(
                Intent(NOTIFICATION_SERVICE_ACTION)
                    .setPackage(packageName)
                    .putExtra("action", ACTION_TAKE_SCREENSHOT)
            )
        }
        screencapBtn.setOnClickListener {
            hideNotificationPanel()
            launchApp("standard", sharedPreferences.getString("app_rec", "")!!)
        }
        settingsBtn.setOnClickListener {
            hideNotificationPanel()
            launchApp("standard", packageName)
        }
        cancelAllBtn!!.setOnClickListener { cancelAllNotifications() }
        notificationsBtn.setImageResource(
            if (sharedPreferences.getBoolean(
                    "show_notifications",
                    true
                )
            ) R.drawable.ic_notifications else R.drawable.ic_notifications_off
        )
        notificationsBtn.setOnClickListener {
            val showNotifications = sharedPreferences.getBoolean("show_notifications", true)
            sharedPreferences.edit().putBoolean("show_notifications", !showNotifications).apply()
            notificationsBtn.setImageResource(
                if (!showNotifications) R.drawable.ic_notifications else R.drawable.ic_notifications_off
            )
            if (showNotifications) Toast.makeText(
                context,
                R.string.popups_disabled,
                Toast.LENGTH_LONG
            ).show()
        }
        ColorUtils.applyMainColor(this@NotificationService, sharedPreferences, notificationArea!!)
        ColorUtils.applyMainColor(this@NotificationService, sharedPreferences, qsArea)
        windowManager.addView(notificationPanel, layoutParams)
        val separator = MaterialDividerItemDecoration(
            ContextThemeWrapper(context, R.style.AppTheme_Dock), LinearLayoutManager.VERTICAL
        )
        separator.dividerColor = ColorUtils.getMainColors(sharedPreferences, context)[4]
        separator.isLastItemDecorated = false
        notificationsLv!!.addItemDecoration(separator)
        updateNotificationPanel()
        notificationPanel!!.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE
                && (event.y < notificationPanel!!.measuredHeight || event.x < notificationPanel!!.x)
            ) {
                hideNotificationPanel()
            }
            false
        }
        Utils.notificationPanelVisible = true
        updateNotificationCount()
    }

    private fun launchApp(mode: String, app: String) {
        sendBroadcast(
            Intent(LAUNCHER_ACTION)
                .setPackage(packageName)
                .putExtra("action", ACTION_LAUNCH_APP)
                .putExtra("mode", mode)
                .putExtra("app", app)
        )
    }

    fun hideNotificationPanel() {
        windowManager.removeView(notificationPanel)
        Utils.notificationPanelVisible = false
        notificationsLv = null
        notificationPanel = null
        cancelAllBtn = null
    }

    private fun updateNotificationPanel() {
        val adapter = NotificationAdapter(context, activeNotifications, this)
        notificationsLv!!.adapter = adapter
        val layoutParams = notificationsLv!!.layoutParams
        val count = adapter.itemCount
        if (count > 3) {
            layoutParams.height = Utils.dpToPx(context, 232)
        } else layoutParams.height = -2
        notificationArea!!.visibility = if (count == 0) View.GONE else View.VISIBLE
        notificationsLv!!.layoutParams = layoutParams
    }

    internal inner class DockServiceReceiver : BroadcastReceiver() {
        override fun onReceive(p1: Context, intent: Intent) {
            when (intent.getStringExtra("action")) {
                ACTION_SHOW_NOTIFICATION_PANEL -> showNotificationPanel()
                ACTION_HIDE_NOTIFICATION_PANEL -> hideNotificationPanel()
            }
        }
    }

    override fun onNotificationClicked(sbn: StatusBarNotification, item: View) {
        val notification = sbn.notification
        if (notification.contentIntent != null) {
            hideNotificationPanel()
            try {
                notification.contentIntent.send()
                if (sbn.isClearable) cancelNotification(sbn.key)
            } catch (_: CanceledException) {
            }
        }
    }

    override fun onNotificationLongClicked(notification: StatusBarNotification, item: View) {}
    override fun onNotificationCancelClicked(notification: StatusBarNotification, item: View) {
        cancelNotification(notification.key)
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, preference: String?) {
        if (preference == "dock_height")
            updateLayoutParams()
    }

    private fun updateLayoutParams() {
        dockHeight =
            Utils.dpToPx(context, sharedPreferences.getString("dock_height", "56")!!.toInt())
        y = (if (DeviceUtils.shouldApplyNavbarFix())
            dockHeight - DeviceUtils.getNavBarHeight(context)
        else
            dockHeight) + margins

        notificationLayoutParams.y = y
        windowManager.updateViewLayout(notificationLayout, notificationLayoutParams)
    }
}
