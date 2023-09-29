package cu.axel.smartdock.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import cu.axel.smartdock.R;
import cu.axel.smartdock.icons.IconParserUtilities;
import cu.axel.smartdock.models.App;
import cu.axel.smartdock.utils.ColorUtils;
import cu.axel.smartdock.utils.Utils;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    private ArrayList<App> apps;
    private final ArrayList<App> allApps;
    private final OnAppClickListener listener;
    private final Context context;
    private int iconBackground;
    private final int iconPadding;
    private final boolean iconTheming;
    private final IconParserUtilities iconParserUtilities;
    private String query;
    private final boolean large;

    public interface OnAppClickListener {
        void onAppClicked(App app, View item);

        void onAppLongClicked(App app, View item);
    }

    public AppAdapter(Context context, IconParserUtilities iconParserUtilities, ArrayList<App> apps,
                      OnAppClickListener listener, boolean large) {
        this.context = context;
        this.listener = listener;
        this.apps = apps;
        this.large = large;
        this.allApps = new ArrayList<>(apps);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        this.iconParserUtilities = iconParserUtilities;
        iconPadding = Utils.dpToPx(context, Integer.parseInt(sp.getString("icon_padding", "5")));
        iconTheming = !sp.getString("icon_pack", "").equals("");

        switch (sp.getString("icon_shape", "circle")) {
            case "circle":
                iconBackground = R.drawable.circle;
                break;
            case "round_rect":
                iconBackground = R.drawable.round_square;
                break;
            case "default":
                iconBackground = -1;
                break;
        }

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int arg1) {
        View itemLayoutView = LayoutInflater.from(context)
                .inflate(large ? R.layout.app_entry_large : R.layout.app_entry, null);

        ViewHolder viewHolder = new ViewHolder(itemLayoutView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        App app = apps.get(position);

        String name = app.getName();

        if (query != null) {
            int spanStart = name.toLowerCase().indexOf(query.toLowerCase());
            int spanEnd = spanStart + query.length();
            if (spanStart != -1) {
                SpannableString spannable = new SpannableString(name);
                spannable.setSpan(new StyleSpan(Typeface.BOLD), spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                viewHolder.nameTv.setText(spannable);
            } else {
                viewHolder.nameTv.setText(name);
            }
        } else {
            viewHolder.nameTv.setText(name);
        }

        if (iconTheming)
            viewHolder.iconIv.setImageDrawable(iconParserUtilities.getPackageThemedIcon(app.getPackageName()));
        else
            viewHolder.iconIv.setImageDrawable(app.getIcon());

        if (iconBackground != -1) {
            viewHolder.iconIv.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
            viewHolder.iconIv.setBackgroundResource(iconBackground);
            ColorUtils.applyColor(viewHolder.iconIv,
                    ColorUtils.getDrawableDominantColor(viewHolder.iconIv.getDrawable()));
        }

        viewHolder.bind(app, listener);
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    public void filter(String query) {
        ArrayList<App> results = new ArrayList<>();

        if (query.length() > 1) {
            if (query.matches("^[0-9]+(\\.[0-9]+)?[-+/*][0-9]+(\\.[0-9]+)?")) {
                results.add(new App(Utils.solve(query) + "", context.getPackageName() + ".calc",
                        ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_calculator, context.getTheme())));
            } else {
                for (App app : allApps) {
                    if (app.getName().toLowerCase().contains(query.toLowerCase()))
                        results.add(app);
                }
            }
            apps = results;
            this.query = query;
        } else {
            apps = allApps;
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView iconIv;
        TextView nameTv;

        public ViewHolder(View itemView) {
            super(itemView);
            iconIv = itemView.findViewById(R.id.app_icon_iv);
            nameTv = itemView.findViewById(R.id.app_name_tv);
        }

        public void bind(App app, OnAppClickListener listener) {
            itemView.setOnClickListener((View v) -> listener.onAppClicked(app, v));

            itemView.setOnLongClickListener((View v) -> {
                listener.onAppLongClicked(app, v);
                return true;
            });

            itemView.setOnTouchListener((View v, MotionEvent p2) -> {
                if (p2.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                    listener.onAppLongClicked(app, v);
                    return true;
                }
                return false;
            });
        }

    }
}