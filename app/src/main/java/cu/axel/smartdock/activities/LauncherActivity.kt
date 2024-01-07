package cu.axel.smartdock.activities

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ShortcutInfo
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import cu.axel.smartdock.R
import cu.axel.smartdock.adapters.AppActionsAdapter
import cu.axel.smartdock.adapters.AppAdapter
import cu.axel.smartdock.adapters.AppAdapter.OnAppClickListener
import cu.axel.smartdock.adapters.AppShortcutAdapter
import cu.axel.smartdock.icons.IconParserUtilities
import cu.axel.smartdock.models.Action
import cu.axel.smartdock.models.App
import cu.axel.smartdock.utils.AppUtils
import cu.axel.smartdock.utils.ColorUtils
import cu.axel.smartdock.utils.DeepShortcutManager
import cu.axel.smartdock.utils.DeviceUtils
import cu.axel.smartdock.utils.Utils
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException


open class LauncherActivity : AppCompatActivity(), OnAppClickListener {
    private lateinit var serviceBtn: MaterialButton
    private lateinit var appsGv: RecyclerView
    private lateinit var notesEt: EditText
    private lateinit var sp: SharedPreferences
    private var x = 0f
    private var y = 0f
    private lateinit var iconParserUtilities: IconParserUtilities
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        val backgroundLayout = findViewById<LinearLayout>(R.id.ll_background)
        serviceBtn = findViewById(R.id.service_btn)
        appsGv = findViewById(R.id.desktop_apps_gv)
        appsGv.layoutManager = GridLayoutManager(this, 2)
        notesEt = findViewById(R.id.notes_et)
        sp = PreferenceManager.getDefaultSharedPreferences(this)
        iconParserUtilities = IconParserUtilities(this)
        serviceBtn
                .setOnClickListener { startActivity(Intent(this@LauncherActivity, MainActivity::class.java)) }
        backgroundLayout.setOnLongClickListener {
            val view = LayoutInflater.from(this@LauncherActivity).inflate(R.layout.task_list, null)
            val lp = Utils.makeWindowParams(-2, -2, this@LauncherActivity, false)
            ColorUtils.applyMainColor(this@LauncherActivity, sp, view)
            lp.gravity = Gravity.TOP or Gravity.START
            lp.flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
            lp.x = x.toInt()
            lp.y = y.toInt()
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            view.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    wm.removeView(view)
                }
                false
            }
            val actionsLv = view.findViewById<ListView>(R.id.tasks_lv)
            val actions = ArrayList<Action>()
            actions.add(Action(R.drawable.ic_wallpaper, getString(R.string.change_wallpaper)))
            actions.add(Action(R.drawable.ic_fullscreen, getString(R.string.display_settings)))
            actionsLv.adapter = AppActionsAdapter(this, actions)
            actionsLv.onItemClickListener = OnItemClickListener { a1: AdapterView<*>, _: View?, p3: Int, _: Long ->
                val action = a1.getItemAtPosition(p3) as Action
                if (action.text == getString(R.string.change_wallpaper)) startActivityForResult(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER),
                        getString(R.string.change_wallpaper)), 18) else if (action.text == getString(R.string.display_settings)) startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
                wm.removeView(view)
            }
            wm.addView(view, lp)
            true
        }
        backgroundLayout.setOnTouchListener { _, event ->
            x = event.x
            y = event.y
            false
        }
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(p1: Context, p2: Intent) {
                val action = p2.getStringExtra("action")
                if (action == "CONNECTED") {
                    serviceBtn.visibility = View.GONE
                } else if (action == "PINNED") {
                    loadDesktopApps()
                }
            }
        }, object : IntentFilter("$packageName.SERVICE") {})
    }

    fun loadDesktopApps() {
        appsGv.adapter = AppAdapter(this, iconParserUtilities,
                AppUtils.getPinnedApps(this, packageManager, AppUtils.DESKTOP_LIST), this, true)
    }

    override fun onResume() {
        super.onResume()
        sendBroadcastToService("resume")
        if (DeviceUtils.isAccessibilityServiceEnabled(this)) serviceBtn.visibility = View.GONE else serviceBtn.visibility = View.VISIBLE
        loadDesktopApps()
        if (sp.getBoolean("show_notes", false)) {
            notesEt.visibility = View.VISIBLE
            loadNotes()
        } else {
            notesEt.visibility = View.GONE
        }
        appsGv.requestFocus()
    }

    override fun onPause() {
        super.onPause()
        if (sp.getBoolean("show_notes", false)) saveNotes()
    }

    override fun onBackPressed() {}
    private fun loadNotes() {
        val notes = File(getExternalFilesDir(null), "notes.txt")
        try {
            val br = BufferedReader(FileReader(notes))
            var line: String?
            val noteContent = StringBuilder()
            while (br.readLine().also { line = it } != null) {
                noteContent.append(line).append("\n")
            }
            br.close()
            notesEt.setText(noteContent.toString())
        } catch (_: IOException) {
        }
    }

    private fun saveNotes() {
        val noteContent = notesEt.text.toString()
        if (noteContent.isNotEmpty()) {
            val notes = File(getExternalFilesDir(null), "notes.txt")
            try {
                val fr = FileWriter(notes)
                fr.write(noteContent)
                fr.close()
            } catch (_: IOException) {
            }
        }
    }

    private fun sendBroadcastToService(action: String) {
        sendBroadcast(Intent("$packageName.HOME").putExtra("action", action))
    }

    private fun launchApp(mode: String?, app: String) {
        sendBroadcast(Intent("$packageName.HOME").putExtra("action", "launch").putExtra("mode", mode)
                .putExtra("app", app))
    }

    private fun getAppActions(app: String): ArrayList<Action> {
        val actions = ArrayList<Action>()
        if (DeepShortcutManager.hasHostPermission(this)) {
            if (DeepShortcutManager.getShortcuts(app, this)!!.isNotEmpty()) actions.add(Action(R.drawable.ic_shortcuts, getString(R.string.shortcuts)))
        }
        actions.add(Action(R.drawable.ic_manage, getString(R.string.manage)))
        actions.add(Action(R.drawable.ic_launch_mode, getString(R.string.open_in)))
        actions.add(Action(R.drawable.ic_remove_from_desktop, getString(R.string.remove)))
        return actions
    }

    @SuppressLint("NewApi")
    private fun showAppContextMenu(app: String, anchor: View) {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val view = LayoutInflater.from(this).inflate(R.layout.task_list, null)
        val lp = Utils.makeWindowParams(-2, -2, this, false)
        ColorUtils.applyMainColor(this@LauncherActivity, sp, view)
        lp.gravity = Gravity.TOP or Gravity.START
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        lp.x = location[0]
        lp.y = location[1] + Utils.dpToPx(this, anchor.measuredHeight / 2)
        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                wm.removeView(view)
            }
            false
        }
        val actionsLv = view.findViewById<ListView>(R.id.tasks_lv)
        actionsLv.adapter = AppActionsAdapter(this, getAppActions(app))
        actionsLv.onItemClickListener = OnItemClickListener { p1: AdapterView<*>, _: View?, p3: Int, _: Long ->
            if (p1.getItemAtPosition(p3) is Action) {
                val action = p1.getItemAtPosition(p3) as Action
                when (action.text) {
                    getString(R.string.manage) -> {
                        val actions = ArrayList<Action>()
                        actions.add(Action(R.drawable.ic_arrow_back, ""))
                        actions.add(Action(R.drawable.ic_info, getString(R.string.app_info)))
                        if (!AppUtils.isSystemApp(this@LauncherActivity, app)
                                || sp.getBoolean("allow_sysapp_uninstall", false)) actions.add(Action(R.drawable.ic_uninstall, getString(R.string.uninstall)))
                        if (sp.getBoolean("allow_app_freeze", false)) actions.add(Action(R.drawable.ic_freeze, getString(R.string.freeze)))
                        actionsLv.adapter = AppActionsAdapter(this@LauncherActivity, actions)
                    }

                    getString(R.string.shortcuts) -> {
                        actionsLv.adapter = AppShortcutAdapter(this,
                                DeepShortcutManager.getShortcuts(app, this)!!)
                    }

                    "" -> {
                        actionsLv.adapter = AppActionsAdapter(this@LauncherActivity, getAppActions(app))
                    }

                    getString(R.string.open_in) -> {
                        val actions = ArrayList<Action>()
                        actions.add(Action(R.drawable.ic_arrow_back, ""))
                        actions.add(Action(R.drawable.ic_standard, getString(R.string.standard)))
                        actions.add(Action(R.drawable.ic_maximized, getString(R.string.maximized)))
                        actions.add(Action(R.drawable.ic_portrait, getString(R.string.portrait)))
                        actions.add(Action(R.drawable.ic_fullscreen, getString(R.string.fullscreen)))
                        actionsLv.adapter = AppActionsAdapter(this@LauncherActivity, actions)
                    }

                    getString(R.string.app_info) -> {
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.parse("package:$app")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        wm.removeView(view)
                    }

                    getString(R.string.uninstall) -> {
                        @Suppress("DEPRECATION")
                        if (AppUtils.isSystemApp(this@LauncherActivity, app)) DeviceUtils.runAsRoot("pm uninstall --user 0 $app") else startActivity(Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:$app"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        wm.removeView(view)
                    }

                    getString(R.string.freeze) -> {
                        val status = DeviceUtils.runAsRoot("pm disable $app")
                        if (status != "error") Toast.makeText(this@LauncherActivity, R.string.app_frozen, Toast.LENGTH_SHORT).show() else Toast.makeText(this@LauncherActivity, R.string.something_wrong, Toast.LENGTH_SHORT).show()
                        wm.removeView(view)
                        loadDesktopApps()
                    }

                    getString(R.string.remove) -> {
                        AppUtils.unpinApp(this@LauncherActivity, app, AppUtils.DESKTOP_LIST)
                        wm.removeView(view)
                        loadDesktopApps()
                    }

                    getString(R.string.standard) -> {
                        wm.removeView(view)
                        launchApp("standard", app)
                    }

                    getString(R.string.maximized) -> {
                        wm.removeView(view)
                        launchApp("maximized", app)
                    }

                    getString(R.string.portrait) -> {
                        wm.removeView(view)
                        launchApp("portrait", app)
                    }

                    getString(R.string.fullscreen) -> {
                        wm.removeView(view)
                        launchApp("fullscreen", app)
                    }
                }
            } else if (p1.getItemAtPosition(p3) is ShortcutInfo) {
                val shortcut = p1.getItemAtPosition(p3) as ShortcutInfo
                wm.removeView(view)
                DeepShortcutManager.startShortcut(shortcut, this@LauncherActivity)
            }
        }
        wm.addView(view, lp)
    }

    override fun onAppClicked(app: App, item: View) {
        launchApp(null, app.packageName)
    }

    override fun onAppLongClicked(app: App, item: View) {
        showAppContextMenu(app.packageName, item)
    }
}
