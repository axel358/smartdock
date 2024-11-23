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
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.Gravity
import android.view.HapticFeedbackConstants
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

const val ACTION_HIDE_NOTIFICATION_PANEL = "hide_panel"
const val ACTION_SHOW_NOTIFICATION_PANEL = "show_panel"
const val NOTIFICATION_COUNT_CHANGED = "count_changed"
const val NOTIFICATION_SERVICE_ACTION = "notification_service_action"

class NotificationService : NotificationListenerService(), OnNotificationClickListener,
    SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var windowManager: WindowManager
    private lateinit var notificationLayout: LinearLayout
    private lateinit var notificationTitleTv: TextView
    private lateinit var notificationTextTv: TextView
    private lateinit var notificationIconIv: ImageView
    private lateinit var notificationCloseBtn: ImageView
    private lateinit var handler: Handler
    private lateinit var sharedPreferences: SharedPreferences
    private var notificationPanel: View? = null
    private var notificationsLv: RecyclerView? = null
    private var cancelAllBtn: ImageButton? = null
    private lateinit var notificationActionsLayout: LinearLayout
    private lateinit var context: Context
    private var notificationArea: LinearLayout? = null
    private var preferLastDisplay = false
    private var y = 0
    private var margins = 0
    private var dockHeight: Int = 0
    private lateinit var notificationLayoutParams: WindowManager.LayoutParams
    private var actionsHeight = 0
    override fun onCreate() {
        super.onCreate()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        preferLastDisplay = sharedPreferences.getBoolean("prefer_last_display", false)
        context = DeviceUtils.getDisplayContext(this, preferLastDisplay)
        actionsHeight = Utils.dpToPx(context, 20)
        windowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
        notificationLayoutParams = Utils.makeWindowParams(
            Utils.dpToPx(context, 300), LinearLayout.LayoutParams.WRAP_CONTENT, context,
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
        notificationLayoutParams.gravity = Gravity.BOTTOM or if (sharedPreferences.getInt(
                "dock_layout",
                -1
            ) == 0
        ) Gravity.CENTER_HORIZONTAL else Gravity.END
        notificationLayoutParams.y = y
        notificationLayout = LayoutInflater.from(this).inflate(
            R.layout.notification_entry,
            null
        ) as LinearLayout
        val padding = Utils.dpToPx(context, 10)
        notificationLayout.setPadding(padding, padding, padding, padding)
        notificationLayout.setBackgroundResource(R.drawable.round_square)
        notificationLayout.visibility = View.GONE
        notificationTitleTv = notificationLayout.findViewById(R.id.notification_title_tv)
        notificationTextTv = notificationLayout.findViewById(R.id.notification_text_tv)
        notificationIconIv = notificationLayout.findViewById(R.id.notification_icon_iv)
        notificationCloseBtn = notificationLayout.findViewById(R.id.notification_close_btn)
        notificationCloseBtn.alpha = 1f
        notificationActionsLayout =
            notificationLayout.findViewById(R.id.notification_actions_layout)
        windowManager.addView(notificationLayout, notificationLayoutParams)
        handler = Handler(Looper.getMainLooper())
        notificationLayout.alpha = 0f
        notificationLayout.setOnHoverListener { _, event ->
            if (event.action == MotionEvent.ACTION_HOVER_ENTER) {
                handler.removeCallbacksAndMessages(null)
            } else if (event.action == MotionEvent.ACTION_HOVER_EXIT) {
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
                if ((sbn.isOngoing && !sharedPreferences.getBoolean(
                        "show_ongoing",
                        false
                    )) || (sbn.packageName == AppUtils.currentApp && sharedPreferences.getBoolean(
                        "silence_current",
                        true
                    )) || notification.contentView != null || isBlackListed(sbn.packageName)
                )
                    return
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

                if (AppUtils.isMediaNotification(notification) && notification.getLargeIcon() != null) {
                    val padding = Utils.dpToPx(context, 0)
                    notificationIconIv.setPadding(padding, padding, padding, padding)
                    notificationIconIv.setImageIcon(notification.getLargeIcon())
                    notificationIconIv.background = null
                } else {
                    notification.smallIcon.setTint(Color.WHITE)
                    notificationIconIv.setBackgroundResource(R.drawable.circle)
                    ColorUtils.applySecondaryColor(
                        context, sharedPreferences,
                        notificationIconIv
                    )
                    val padding = Utils.dpToPx(context, 14)
                    notificationIconIv.setPadding(padding, padding, padding, padding)
                    notificationIconIv.setImageIcon(notification.smallIcon)
                }

                val progress = extras.getInt(Notification.EXTRA_PROGRESS)
                val p = if (progress != 0) " $progress%" else ""
                notificationTitleTv.text = notificationTitle + p
                notificationTextTv.text = notificationText
                val actions = notification.actions
                notificationActionsLayout.removeAllViews()
                if (actions != null) {
                    val actionLayoutParams = LinearLayout.LayoutParams(0, actionsHeight)
                    actionLayoutParams.weight = 1f
                    if (AppUtils.isMediaNotification(notification)) {
                        for (action in actions) {
                            val actionIv = ImageView(this@NotificationService)
                            try {
                                val resources = packageManager
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
                                notificationTextTv.isSingleLine = true
                                notificationActionsLayout.addView(actionIv, actionLayoutParams)
                            } catch (_: PackageManager.NameNotFoundException) {
                            }
                        }
                    } else {
                        for (action in actions) {
                            val actionTv = TextView(context)
                            actionTv.isSingleLine = true
                            actionTv.text = action.title
                            actionTv.setTextColor(getColor(R.color.action))
                            actionTv.setOnClickListener {
                                try {
                                    action.actionIntent.send()
                                    notificationLayout.visibility = View.GONE
                                    notificationLayout.alpha = 0f
                                } catch (_: CanceledException) {
                                }
                            }
                            notificationActionsLayout!!.addView(actionTv, actionLayoutParams)
                        }
                    }
                }
                notificationCloseBtn.setOnClickListener {
                    notificationLayout.visibility = View.GONE
                    if (sbn.isClearable)
                        cancelNotification(sbn.key)
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
                    val savedApps = sharedPreferences.getStringSet(
                        "ignored_notifications_popups",
                        setOf()
                    )!!
                    val ignoredApps = mutableSetOf<String>()
                    ignoredApps.addAll(savedApps)
                    ignoredApps.add(sbn.packageName)

                    sharedPreferences.edit()
                        .putStringSet("ignored_notifications_popups", ignoredApps).apply()
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
        }, sharedPreferences.getString("notification_display_time", "5")!!.toInt() * 1000L)
    }

    private fun isBlackListed(packageName: String): Boolean {
        val ignoredPackages =
            sharedPreferences.getStringSet("ignored_notifications_popups", setOf("android"))
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
        val ignoredApps = sharedPreferences.getStringSet("ignored_notifications_panel", setOf())!!
        val adapter = NotificationAdapter(
            context,
            activeNotifications.filterNot { ignoredApps.contains(it.packageName) }.sortedWith(
                compareByDescending { AppUtils.isMediaNotification(it.notification) && it.isOngoing })
                .toTypedArray(),
            this
        )
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

    override fun onNotificationLongClicked(notification: StatusBarNotification, item: View) {
        val savedApps = sharedPreferences.getStringSet(
            "ignored_notifications_panel",
            setOf()
        )!!
        val ignoredApps = mutableSetOf<String>()
        ignoredApps.addAll(savedApps)
        ignoredApps.add(notification.packageName)
        Toast.makeText(this, ignoredApps.toString(), Toast.LENGTH_LONG).show()
        sharedPreferences.edit().putStringSet("ignored_notifications_panel", ignoredApps).apply()
        item.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        Toast.makeText(
            this@NotificationService,
            R.string.silenced_notifications,
            Toast.LENGTH_LONG
        )
            .show()
        updateNotificationPanel()
    }

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
