package cu.axel.smartdock.activities

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.ShortcutInfo
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import cu.axel.smartdock.R
import cu.axel.smartdock.adapters.AppActionsAdapter
import cu.axel.smartdock.adapters.AppAdapter
import cu.axel.smartdock.adapters.AppAdapter.OnAppClickListener
import cu.axel.smartdock.adapters.AppShortcutAdapter
import cu.axel.smartdock.models.Action
import cu.axel.smartdock.models.App
import cu.axel.smartdock.services.ACTION_LAUNCH_APP
import cu.axel.smartdock.services.DESKTOP_APP_PINNED
import cu.axel.smartdock.services.DOCK_SERVICE_ACTION
import cu.axel.smartdock.services.DOCK_SERVICE_CONNECTED
import cu.axel.smartdock.utils.AppUtils
import cu.axel.smartdock.utils.ColorUtils
import cu.axel.smartdock.utils.DeepShortcutManager
import cu.axel.smartdock.utils.DeviceUtils
import cu.axel.smartdock.utils.IconPackUtils
import cu.axel.smartdock.utils.Utils
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

const val LAUNCHER_ACTION = "launcher_action"
const val LAUNCHER_RESUMED = "launcher_resumed"

open class LauncherActivity : AppCompatActivity(), OnAppClickListener,
    OnSharedPreferenceChangeListener {
    private var iconPackUtils: IconPackUtils? = null
    private lateinit var serviceBtn: MaterialButton
    private lateinit var appsGv: RecyclerView
    private lateinit var notesEt: EditText
    private lateinit var sharedPreferences: SharedPreferences
    private var x = 0f
    private var y = 0f
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        val backgroundLayout = findViewById<LinearLayout>(R.id.ll_background)
        serviceBtn = findViewById(R.id.service_btn)
        appsGv = findViewById(R.id.desktop_apps_gv)
        appsGv.layoutManager = GridLayoutManager(this, 2)
        notesEt = findViewById(R.id.notes_et)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        serviceBtn
            .setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
        backgroundLayout.setOnLongClickListener {
            val view = LayoutInflater.from(this).inflate(R.layout.task_list, null)
            val layoutParams = Utils.makeWindowParams(-2, -2, this)
            ColorUtils.applyMainColor(this, sharedPreferences, view)
            layoutParams.gravity = Gravity.TOP or Gravity.START
            layoutParams.flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
            layoutParams.x = x.toInt()
            layoutParams.y = y.toInt()
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            view.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    windowManager.removeView(view)
                }
                false
            }
            val actionsLv = view.findViewById<ListView>(R.id.tasks_lv)
            val actions = ArrayList<Action>()
            actions.add(Action(R.drawable.ic_wallpaper, getString(R.string.change_wallpaper)))
            actions.add(Action(R.drawable.ic_fullscreen, getString(R.string.display_settings)))
            actionsLv.adapter = AppActionsAdapter(this, actions)
            actionsLv.setOnItemClickListener { adapterView, _, position, _ ->
                val action = adapterView.getItemAtPosition(position) as Action
                if (action.text == getString(R.string.change_wallpaper)) startActivityForResult(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SET_WALLPAPER),
                        getString(R.string.change_wallpaper)
                    ), 18
                ) else if (action.text == getString(R.string.display_settings)) startActivity(
                    Intent(
                        Settings.ACTION_DISPLAY_SETTINGS
                    )
                )
                windowManager.removeView(view)
            }
            windowManager.addView(view, layoutParams)
            true
        }
        backgroundLayout.setOnTouchListener { _, event ->
            x = event.x
            y = event.y
            false
        }
        ContextCompat.registerReceiver(
            this, object : BroadcastReceiver() {
                override fun onReceive(p1: Context, intent: Intent) {
                    when (intent.getStringExtra("action")) {
                        DOCK_SERVICE_CONNECTED -> serviceBtn.visibility = View.GONE
                        DESKTOP_APP_PINNED -> loadDesktopApps()
                    }
                }
            }, IntentFilter(DOCK_SERVICE_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        if (sharedPreferences.getString("icon_pack", "")!!.isNotEmpty()) {
            iconPackUtils = IconPackUtils(this)
        }
    }

    fun loadDesktopApps() {
        appsGv.adapter = AppAdapter(
            this,
            AppUtils.getPinnedApps(this, AppUtils.DESKTOP_LIST), this, true, iconPackUtils
        )
    }

    override fun onResume() {
        super.onResume()
        sendBroadcast(
            Intent(LAUNCHER_ACTION)
                .setPackage(packageName)
                .putExtra("action", LAUNCHER_RESUMED)
        )

        serviceBtn.visibility =
            if (DeviceUtils.isAccessibilityServiceEnabled(this)) View.GONE else View.VISIBLE

        loadDesktopApps()

        if (sharedPreferences.getBoolean("show_notes", false)) {
            notesEt.visibility = View.VISIBLE
            loadNotes()
        } else
            notesEt.visibility = View.GONE

        appsGv.requestFocus()
    }

    override fun onPause() {
        super.onPause()
        if (sharedPreferences.getBoolean("show_notes", false))
            saveNotes()
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {}
    private fun loadNotes() {
        val notes = File(getExternalFilesDir(null), "notes.txt")
        try {
            val bufferedReader = BufferedReader(FileReader(notes))
            var line: String?
            val noteContent = StringBuilder()
            while (bufferedReader.readLine().also { line = it } != null) {
                noteContent.append(line).append("\n")
            }
            bufferedReader.close()
            notesEt.setText(noteContent.toString())
        } catch (_: IOException) {
        }
    }

    private fun saveNotes() {
        val noteContent = notesEt.text.toString()
        if (noteContent.isNotEmpty()) {
            val notes = File(getExternalFilesDir(null), "notes.txt")
            try {
                val fileWriter = FileWriter(notes)
                fileWriter.write(noteContent)
                fileWriter.close()
            } catch (_: IOException) {
            }
        }
    }

    private fun launchApp(mode: String?, app: String) {
        sendBroadcast(
            Intent(LAUNCHER_ACTION)
                .setPackage(packageName)
                .putExtra("action", ACTION_LAUNCH_APP)
                .putExtra("mode", mode)
                .putExtra("app", app)
        )
    }

    private fun getAppActions(app: String): ArrayList<Action> {
        val actions = ArrayList<Action>()
        if (DeepShortcutManager.hasHostPermission(this)) {
            if (DeepShortcutManager.getShortcuts(app, this)!!
                    .isNotEmpty()
            ) actions.add(Action(R.drawable.ic_shortcuts, getString(R.string.shortcuts)))
        }
        actions.add(Action(R.drawable.ic_manage, getString(R.string.manage)))
        actions.add(Action(R.drawable.ic_launch_mode, getString(R.string.open_as)))
        actions.add(Action(R.drawable.ic_remove_from_desktop, getString(R.string.remove)))
        return actions
    }

    @SuppressLint("NewApi")
    private fun showAppContextMenu(app: App, anchor: View) {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val view = LayoutInflater.from(this).inflate(R.layout.task_list, null)
        val layoutParams = Utils.makeWindowParams(-2, -2, this)
        ColorUtils.applyMainColor(this, sharedPreferences, view)
        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        layoutParams.x = location[0]
        layoutParams.y = location[1] + Utils.dpToPx(this, anchor.measuredHeight / 2)
        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                windowManager.removeView(view)
            }
            false
        }
        val actionsLv = view.findViewById<ListView>(R.id.tasks_lv)
        actionsLv.adapter = AppActionsAdapter(this, getAppActions(app.packageName))
        actionsLv.setOnItemClickListener { adapterView, _, position, _ ->
            if (adapterView.getItemAtPosition(position) is Action) {
                val action = adapterView.getItemAtPosition(position) as Action
                when (action.text) {
                    getString(R.string.manage) -> {
                        val actions = ArrayList<Action>()
                        actions.add(Action(R.drawable.ic_arrow_back, ""))
                        actions.add(Action(R.drawable.ic_info, getString(R.string.app_info)))
                        if (!AppUtils.isSystemApp(this, app.packageName)
                            || sharedPreferences.getBoolean("allow_sysapp_uninstall", false)
                        ) actions.add(
                            Action(
                                R.drawable.ic_uninstall,
                                getString(R.string.uninstall)
                            )
                        )
                        if (sharedPreferences.getBoolean("allow_app_freeze", false)) actions.add(
                            Action(R.drawable.ic_freeze, getString(R.string.freeze))
                        )
                        actionsLv.adapter = AppActionsAdapter(this, actions)
                    }

                    getString(R.string.shortcuts) -> {
                        actionsLv.adapter = AppShortcutAdapter(
                            this,
                            DeepShortcutManager.getShortcuts(app.packageName, this)!!
                        )
                    }

                    "" -> {
                        actionsLv.adapter = AppActionsAdapter(this, getAppActions(app.packageName))
                    }

                    getString(R.string.open_as) -> {
                        val actions = ArrayList<Action>()
                        actions.add(Action(R.drawable.ic_arrow_back, ""))
                        actions.add(Action(R.drawable.ic_standard, getString(R.string.standard)))
                        actions.add(Action(R.drawable.ic_maximized, getString(R.string.maximized)))
                        actions.add(Action(R.drawable.ic_portrait, getString(R.string.portrait)))
                        actions.add(
                            Action(
                                R.drawable.ic_fullscreen,
                                getString(R.string.fullscreen)
                            )
                        )
                        actionsLv.adapter = AppActionsAdapter(this, actions)
                    }

                    getString(R.string.app_info) -> {
                        startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.parse("package:$app"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        windowManager.removeView(view)
                    }

                    getString(R.string.uninstall) -> {
                        @Suppress("DEPRECATION")
                        if (AppUtils.isSystemApp(
                                this,
                                app.packageName
                            )
                        ) DeviceUtils.runAsRoot("pm uninstall --user 0 $app") else startActivity(
                            Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:$app"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        windowManager.removeView(view)
                    }

                    getString(R.string.freeze) -> {
                        val status = DeviceUtils.runAsRoot("pm disable $app")
                        if (status != "error")
                            Toast.makeText(this, R.string.app_frozen, Toast.LENGTH_SHORT).show()
                        else
                            Toast.makeText(this, R.string.something_wrong, Toast.LENGTH_SHORT)
                                .show()
                        windowManager.removeView(view)
                        loadDesktopApps()
                    }

                    getString(R.string.remove) -> {
                        AppUtils.unpinApp(this, app.packageName, AppUtils.DESKTOP_LIST)
                        windowManager.removeView(view)
                        loadDesktopApps()
                    }

                    getString(R.string.standard) -> {
                        windowManager.removeView(view)
                        launchApp("standard", app.packageName)
                    }

                    getString(R.string.maximized) -> {
                        windowManager.removeView(view)
                        launchApp("maximized", app.packageName)
                    }

                    getString(R.string.portrait) -> {
                        windowManager.removeView(view)
                        launchApp("portrait", app.packageName)
                    }

                    getString(R.string.fullscreen) -> {
                        windowManager.removeView(view)
                        launchApp("fullscreen", app.packageName)
                    }
                }
            } else if (adapterView.getItemAtPosition(position) is ShortcutInfo) {
                val shortcut = adapterView.getItemAtPosition(position) as ShortcutInfo
                windowManager.removeView(view)
                DeepShortcutManager.startShortcut(shortcut, this)
            }
        }
        windowManager.addView(view, layoutParams)
    }

    override fun onAppClicked(app: App, item: View) {
        launchApp(null, app.packageName)
    }

    override fun onAppLongClicked(app: App, item: View) {
        showAppContextMenu(app, item)
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        preference: String?
    ) {
        if (preference == null)
            return
        if (preference == "icon_pack") {
            val iconPack = sharedPreferences.getString("icon_pack", "")!!
            iconPackUtils = if (iconPack.isNotEmpty()) {
                IconPackUtils(this)
            } else
                null
        }
    }
}
