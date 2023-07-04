package cu.axel.smartdock.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
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
import cu.axel.smartdock.utils.Utils;
import cu.axel.smartdock.utils.AppUtils;
import cu.axel.smartdock.utils.ColorUtils;
import cu.axel.smartdock.models.DockApp;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class DockAppAdapter extends RecyclerView.Adapter<DockAppAdapter.ViewHolder> {

	private ArrayList<DockApp> apps;
	private OnDockAppClickListener listener;
	private final Context context;
	private int iconBackground;
	private final int iconPadding;
	private boolean iconTheming, tintIndicators;
	private IconParserUtilities iconParserUtilities;

	public interface OnDockAppClickListener {
		void onDockAppClicked(DockApp app, View item);

		void onDockAppLongClicked(DockApp app, View item);
	}

	public DockAppAdapter(Context context, ArrayList<DockApp> apps, OnDockAppClickListener listener) {
		this.apps = apps;
		this.listener = listener;
		this.context = context;
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		iconTheming = !sp.getString("icon_pack", "").equals("");
		iconPadding = Utils.dpToPx(context, Integer.parseInt(sp.getString("icon_padding", "5")));
		
		tintIndicators = sp.getBoolean("tint_indicators", false);
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
		View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_task_entry, null);

		ViewHolder viewHolder = new ViewHolder(itemLayoutView);
		return viewHolder;
	}

	@Override
	public void onBindViewHolder(ViewHolder viewHolder, int position) {
		DockApp app = apps.get(position);

		int size = app.getTasks().size();

		if (size > 0) {
			if (tintIndicators)
				ColorUtils.applyColor(viewHolder.runningIndicator,
						ColorUtils.manipulateColor(ColorUtils.getDrawableDominantColor(app.getIcon()), 2f));
			if (app.getTasks().get(0).getID() != -1)
				viewHolder.runningIndicator.setAlpha(1f);

			if (app.getPackageName().equals(AppUtils.currentApp)) {
				viewHolder.runningIndicator.getLayoutParams().width = Utils.dpToPx(context, 16);
			}

			if (size > 1) {
				viewHolder.taskCounter.setText("" + size);
				viewHolder.taskCounter.setAlpha(1f);
			}
		}

		if (iconTheming || size > 1)
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

	public static class ViewHolder extends RecyclerView.ViewHolder {

		ImageView iconIv;
		TextView taskCounter;
		View runningIndicator;

		public ViewHolder(View itemView) {
			super(itemView);
			iconIv = itemView.findViewById(R.id.icon_iv);
			runningIndicator = itemView.findViewById(R.id.running_indicator);
			taskCounter = itemView.findViewById(R.id.task_count_badge);
		}

		public void bind(DockApp app, OnDockAppClickListener listener) {
			itemView.setOnClickListener((View v) -> {
				listener.onDockAppClicked(app, v);
			});

			itemView.setOnLongClickListener((View v) -> {
				listener.onDockAppLongClicked(app, v);
				return true;
			});

			itemView.setOnTouchListener((View v, MotionEvent p2) -> {
				if (p2.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
					listener.onDockAppLongClicked(app, v);
					return true;
				}
				return false;
			});
		}

	}
}