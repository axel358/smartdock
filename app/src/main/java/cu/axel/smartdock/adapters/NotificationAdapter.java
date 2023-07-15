package cu.axel.smartdock.adapters;

import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import android.app.Notification;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

	private StatusBarNotification[] notifications;
	private OnNotificationClickListener listener;
	private final Context context;
	private int iconBackground;
	private final int iconPadding;
	private boolean iconTheming;
	private IconParserUtilities iconParserUtilities;

	public interface OnNotificationClickListener {
		void onNotificationClicked(StatusBarNotification notification, View item);

		void onNotificationLongClicked(StatusBarNotification notification, View item);

		void onNotificationCancelClicked(StatusBarNotification notification, View item);
	}

	public NotificationAdapter(Context context, IconParserUtilities iconParserUtilities,
			StatusBarNotification[] notifications, OnNotificationClickListener listener) {
		this.notifications = notifications;
		this.listener = listener;
		this.context = context;
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		iconTheming = !sp.getString("icon_pack", "").equals("");
		iconPadding = Utils.dpToPx(context, Integer.parseInt(sp.getString("icon_padding", "5")));
		this.iconParserUtilities = iconParserUtilities;

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
		View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_entry, null);

		ViewHolder viewHolder = new ViewHolder(itemLayoutView);
		return viewHolder;
	}

	@Override
	public void onBindViewHolder(ViewHolder viewHolder, int position) {
		final StatusBarNotification sbn = notifications[position];
		final Notification notification = sbn.getNotification();

		Notification.Action[] actions = notification.actions;
		Bundle extras = notification.extras;

		viewHolder.notifActionsLayout.removeAllViews();

		if (actions != null) {
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
			lp.weight = 1f;

			if (extras.get(Notification.EXTRA_MEDIA_SESSION) != null) {
				for (final Notification.Action action : actions) {
					ImageView actionTv = new ImageView(context);
					try {
						Resources res = context.getPackageManager().getResourcesForApplication(sbn.getPackageName());

						Drawable drawable = res
								.getDrawable(res.getIdentifier(action.icon + "", "drawable", sbn.getPackageName()));
						drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
						actionTv.setImageDrawable(drawable);
						actionTv.setOnClickListener((View p1) -> {
							try {
								action.actionIntent.send();
							} catch (PendingIntent.CanceledException e) {
							}
						});

						viewHolder.notifActionsLayout.addView(actionTv, lp);

					} catch (PackageManager.NameNotFoundException e) {

					}
				}
			} else {
				for (final Notification.Action action : actions) {
					TextView actionTv = new TextView(context);
					actionTv.setTextColor(Color.WHITE);
					actionTv.setSingleLine(true);
					actionTv.setText(action.title);
					actionTv.setOnClickListener((View p1) -> {
						try {
							action.actionIntent.send();
						} catch (PendingIntent.CanceledException e) {
						}
					});
					viewHolder.notifActionsLayout.addView(actionTv, lp);
				}
			}
		}

		String notificationTitle = extras.getString(Notification.EXTRA_TITLE);
		if (notificationTitle == null)
			notificationTitle = AppUtils.getPackageLabel(context, sbn.getPackageName());
		CharSequence notificationText = extras.getCharSequence(Notification.EXTRA_TEXT);
		int progress = extras.getInt(Notification.EXTRA_PROGRESS);

		String p = progress != 0 ? " " + progress + "%" : "";

		viewHolder.notifTitle.setText(notificationTitle + p);
		viewHolder.notifText.setText(notificationText);

		if (sbn.isClearable()) {

			viewHolder.notifCancelBtn.setAlpha(1f);
			viewHolder.notifCancelBtn.setOnClickListener((View p1) -> {

				if (sbn.isClearable())
					listener.onNotificationCancelClicked(sbn, p1);

			});
		} else
			viewHolder.notifCancelBtn.setAlpha(0f);

		Drawable notificationIcon = AppUtils.getAppIcon(context, sbn.getPackageName());

		if (iconTheming)
			viewHolder.notifIcon.setImageDrawable(iconParserUtilities.getPackageThemedIcon(sbn.getPackageName()));
		else
			viewHolder.notifIcon.setImageDrawable(notificationIcon);

		if (iconBackground != -1) {
			viewHolder.notifIcon.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
			viewHolder.notifIcon.setBackgroundResource(iconBackground);
			ColorUtils.applyColor(viewHolder.notifIcon, ColorUtils.getDrawableDominantColor(notificationIcon));
		}

		viewHolder.bind(sbn, listener);
	}

	@Override
	public int getItemCount() {
		return notifications.length;
	}

	public static class ViewHolder extends RecyclerView.ViewHolder {

		ImageView notifIcon, notifCancelBtn;
		TextView notifTitle, notifText;
		LinearLayout notifActionsLayout;

		public ViewHolder(View itemView) {
			super(itemView);
			notifTitle = itemView.findViewById(R.id.notif_w_title_tv);
			notifText = itemView.findViewById(R.id.notif_w_text_tv);
			notifIcon = itemView.findViewById(R.id.notif_w_icon_iv);
			notifCancelBtn = itemView.findViewById(R.id.notif_w_close_btn);
			notifActionsLayout = itemView.findViewById(R.id.notif_actions_container);

		}

		public void bind(StatusBarNotification notification, OnNotificationClickListener listener) {
			itemView.setOnClickListener((View v) -> {
				listener.onNotificationClicked(notification, v);
			});

			itemView.setOnLongClickListener((View v) -> {
				listener.onNotificationLongClicked(notification, v);
				return true;
			});

		}

	}
}