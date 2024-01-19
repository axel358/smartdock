package cu.axel.smartdock.adapters

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import cu.axel.smartdock.models.App
import cu.axel.smartdock.R
import cu.axel.smartdock.utils.ColorUtils
import cu.axel.smartdock.utils.Utils
import java.util.Locale

class AppAdapter(private val context: Context, private var apps: ArrayList<App>,
                 private val listener: OnAppClickListener, private val large: Boolean) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {
    private val allApps: ArrayList<App> = ArrayList(apps)
    private var iconBackground = 0
    private val iconPadding: Int
    private val iconTheming: Boolean
    private lateinit var query: String

    interface OnAppClickListener {
        fun onAppClicked(app: App, item: View)
        fun onAppLongClicked(app: App, item: View)
    }

    init {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        iconPadding = Utils.dpToPx(context, sharedPreferences.getString("icon_padding", "5")!!.toInt())
        iconTheming = sharedPreferences.getString("icon_pack", "") != ""
        when (sharedPreferences.getString("icon_shape", "circle")) {
            "circle" -> iconBackground = R.drawable.circle
            "round_rect" -> iconBackground = R.drawable.round_square
            "default" -> iconBackground = -1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, arg1: Int): ViewHolder {
        val itemLayoutView = LayoutInflater.from(context)
                .inflate(if (large) R.layout.app_entry_large else R.layout.app_entry, null)
        return ViewHolder(itemLayoutView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val app = apps[position]
        val name = app.name
        if (::query.isInitialized) {
            val spanStart = name.lowercase(Locale.getDefault()).indexOf(query.lowercase(Locale.getDefault()))
            val spanEnd = spanStart + query.length
            if (spanStart != -1) {
                val spannable = SpannableString(name)
                spannable.setSpan(StyleSpan(Typeface.BOLD), spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                viewHolder.nameTv.text = spannable
            } else {
                viewHolder.nameTv.text = name
            }
        } else {
            viewHolder.nameTv.text = name
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

    fun filter(query: String) {
        val results = ArrayList<App>()
        if (query.length > 1) {
            if (query.matches("^[0-9]+(\\.[0-9]+)?[-+/*][0-9]+(\\.[0-9]+)?".toRegex())) {
                results.add(App(Utils.solve(query).toString() + "", context.packageName + ".calc",
                        ResourcesCompat.getDrawable(context.resources, R.drawable.ic_calculator, context.theme)!!))
            } else {
                for (app in allApps) {
                    if (app.name.lowercase(Locale.getDefault()).contains(query.lowercase(Locale.getDefault()))) results.add(app)
                }
            }
            apps = results
            this.query = query
        } else {
            apps = allApps
        }
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var iconIv: ImageView
        var nameTv: TextView

        init {
            iconIv = itemView.findViewById(R.id.app_icon_iv)
            nameTv = itemView.findViewById(R.id.app_name_tv)
        }

        fun bind(app: App, listener: OnAppClickListener) {
            itemView.setOnClickListener { view -> listener.onAppClicked(app, view) }
            itemView.setOnLongClickListener { view ->
                listener.onAppLongClicked(app, view)
                true
            }
            itemView.setOnTouchListener { view, event ->
                if (event.buttonState == MotionEvent.BUTTON_SECONDARY) {
                    listener.onAppLongClicked(app, view)
                    return@setOnTouchListener true
                }
                false
            }
        }
    }
}