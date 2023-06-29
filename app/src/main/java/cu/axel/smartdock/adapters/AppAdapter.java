package cu.axel.smartdock.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Spannable;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.preference.PreferenceManager;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import cu.axel.smartdock.R;
import cu.axel.smartdock.icons.IconParserUtilities;
import cu.axel.smartdock.models.App;
import cu.axel.smartdock.utils.Utils;
import cu.axel.smartdock.utils.AppUtils;
import cu.axel.smartdock.utils.ColorUtils;
import java.util.ArrayList;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

	private ArrayList<App> apps, allApps;
	private OnAppClickListener listener;
	private final Context context;
	private int iconBackground;
	private final int iconPadding;
	private boolean iconTheming, menuFullscreen, phoneLayout;
	private IconParserUtilities iconParserUtilities;
	private String query;

	public interface OnAppClickListener {
		void onAppClicked(App app, View item);

		void onAppLongClicked(App app, View item);
	}

	public AppAdapter(Context context, ArrayList<App> apps, OnAppClickListener listener) {
		this.context = context;
		this.listener = listener;
		this.apps = apps;
		this.allApps = new ArrayList<App>(apps);
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		iconParserUtilities = new IconParserUtilities(context);
		menuFullscreen = sp.getBoolean("app_menu_fullscreen", false);
		phoneLayout = sp.getInt("dock_layout", -1) == 0;
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
				.inflate(menuFullscreen && !phoneLayout ? R.layout.app_entry_desktop : R.layout.app_entry, null);

		ViewHolder viewHolder = new ViewHolder(itemLayoutView);
		return viewHolder;
	}

	@Override
	public void onBindViewHolder(ViewHolder viewHolder, int position) {
		App app = apps.get(position);
		//viewHolder.nameTv.setText(app.getName());

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

		if (query.length() > 2) {
			if (query.matches("^[0-9]+(\\.[0-9]+)?[-+/*][0-9]+(\\.[0-9]+)?")) {
				results.add(new App(Utils.solve(query) + "", context.getPackageName() + ".calc",
						context.getResources().getDrawable(R.drawable.ic_calculator, context.getTheme())));
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
			itemView.setOnClickListener((View v) -> {
				listener.onAppClicked(app, v);
			});

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