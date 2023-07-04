package cu.axel.smartdock.services;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnHoverListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cu.axel.smartdock.R;
import cu.axel.smartdock.adapters.NotificationAdapter;
import cu.axel.smartdock.icons.IconParserUtilities;
import cu.axel.smartdock.services.NotificationService;
import cu.axel.smartdock.utils.ColorUtils;
import cu.axel.smartdock.utils.DeviceUtils;
import cu.axel.smartdock.utils.Utils;
import cu.axel.smartdock.widgets.HoverInterceptorLayout;
import android.util.Log;
import android.app.PendingIntent.CanceledException;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.widget.ImageButton;
import cu.axel.smartdock.utils.AppUtils;
import android.view.KeyEvent;

public class NotificationService extends NotificationListenerService
		implements NotificationAdapter.OnNotificationClickListener {
	private WindowManager wm;
	private HoverInterceptorLayout notificationLayout;
	private TextView notifTitle, notifText;
	private ImageView notifIcon, notifCancelBtn;
	private Handler handler;
	private SharedPreferences sp;
	private DockServiceReceiver dockReceiver;
	private View notificationPanel;
	private RecyclerView notificationsLv;
	private NotificationManager nm;
	private ImageButton cancelAllBtn;
	private LinearLayout notifActionsLayout;
	private Context context;
	private LinearLayout notificationArea;
	private boolean preferLastDisplay;
	private IconParserUtilities iconParserUtilities;

	@Override
	public void onCreate() {
		super.onCreate();

		sp = PreferenceManager.getDefaultSharedPreferences(this);
		preferLastDisplay = sp.getBoolean("prefer_last_display", false);
		context = DeviceUtils.getDisplayContext(this, preferLastDisplay);
		wm = (WindowManager) context.getSystemService(WINDOW_SERVICE);
		nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		iconParserUtilities = new IconParserUtilities(context);

		WindowManager.LayoutParams lp = Utils.makeWindowParams(Utils.dpToPx(context, 300), -2, context,
				preferLastDisplay);
		lp.x = 5;
		if (sp.getBoolean("notification_bottom", true)) {
			lp.gravity = Gravity.BOTTOM | Gravity.RIGHT;
			lp.y = Utils.dpToPx(this, 60);
		} else {
			lp.gravity = Gravity.TOP | Gravity.RIGHT;
			lp.y = 5;
		}

		notificationLayout = (HoverInterceptorLayout) LayoutInflater.from(this).inflate(R.layout.notification, null);
		notificationLayout.setVisibility(View.GONE);

		notifTitle = notificationLayout.findViewById(R.id.notif_title_tv);
		notifText = notificationLayout.findViewById(R.id.notif_text_tv);
		notifIcon = notificationLayout.findViewById(R.id.notif_icon_iv);
		notifCancelBtn = notificationLayout.findViewById(R.id.notif_close_btn);

		notifActionsLayout = notificationLayout.findViewById(R.id.notif_actions_container2);

		wm.addView(notificationLayout, lp);

		handler = new Handler(Looper.getMainLooper());
		notificationLayout.setAlpha(0);

		notificationLayout.setOnHoverListener((View p1, MotionEvent p2) -> {
			if (p2.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
				notifCancelBtn.setVisibility(View.VISIBLE);
				handler.removeCallbacksAndMessages(null);
			} else if (p2.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
				new Handler(Looper.getMainLooper()).postDelayed(() -> {
					notifCancelBtn.setVisibility(View.INVISIBLE);
				}, 200);
				hideNotification();
			}
			return false;
		});

		dockReceiver = new DockServiceReceiver();
		registerReceiver(dockReceiver, new IntentFilter(getPackageName() + ".DOCK"));
	}

	@Override
	public void onListenerConnected() {
		super.onListenerConnected();
		updateNotificationCount();
	}

	@Override
	public void onNotificationRemoved(StatusBarNotification sbn) {
		super.onNotificationRemoved(sbn);
		updateNotificationCount();
		if (Utils.notificationPanelVisible)
			updateNotificationPanel();
	}

	@Override
	public void onNotificationPosted(final StatusBarNotification sbn) {
		super.onNotificationPosted(sbn);

		updateNotificationCount();

		if (Utils.notificationPanelVisible) {
			updateNotificationPanel();
		} else {
			if (!sp.getBoolean("do_not_disturb", false)) {
				final Notification notification = sbn.getNotification();

				if (sbn.isOngoing()
						&& !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("show_ongoing", false)) {
				} else if (notification.contentView == null && !isBlackListed(sbn.getPackageName())
						&& !(sbn.getPackageName().equals(AppUtils.currentApp) && sp.getBoolean("show_current", true))) {
					Bundle extras = notification.extras;

					String notificationTitle = extras.getString(Notification.EXTRA_TITLE);
					if (notificationTitle == null)
						notificationTitle = AppUtils.getPackageLabel(context, sbn.getPackageName());

					CharSequence notificationText = extras.getCharSequence(Notification.EXTRA_TEXT);

					ColorUtils.applyMainColor(NotificationService.this, sp, notificationLayout);
					ColorUtils.applySecondaryColor(NotificationService.this, sp, notifCancelBtn);

					Drawable notificationIcon = AppUtils.getAppIcon(context, sbn.getPackageName());
					notifIcon.setImageDrawable(notificationIcon);


					boolean iconTheming = !sp.getString("icon_pack", "").equals("");
					int iconPadding = Utils.dpToPx(context, Integer.parseInt(sp.getString("icon_padding", "5")));
					int iconBackground = -1;
					
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

					if (iconTheming)
						notifIcon.setImageDrawable(iconParserUtilities.getPackageThemedIcon(sbn.getPackageName()));
					else
						notifIcon.setImageDrawable(notificationIcon);

					if (iconBackground != -1) {
						notifIcon.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
						notifIcon.setBackgroundResource(iconBackground);
						ColorUtils.applyColor(notifIcon, ColorUtils.getDrawableDominantColor(notificationIcon));
					}

					int progress = extras.getInt(Notification.EXTRA_PROGRESS);

					String p = progress != 0 ? " " + progress + "%" : "";

					notifTitle.setText(notificationTitle + p);
					notifText.setText(notificationText);

					Notification.Action[] actions = notification.actions;

					notifActionsLayout.removeAllViews();

					if (actions != null) {
						LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
						lp.weight = 1f;

						if (extras.get(Notification.EXTRA_MEDIA_SESSION) != null) {
							//lp.height = Utils.dpToPx(NotificationService.this, 30);
							for (final Notification.Action action : actions) {
								ImageView actionTv = new ImageView(NotificationService.this);
								try {
									Resources res = getPackageManager()
											.getResourcesForApplication(sbn.getPackageName());

									Drawable drawable = res.getDrawable(
											res.getIdentifier(action.icon + "", "drawable", sbn.getPackageName()));
									drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
									actionTv.setImageDrawable(drawable);
									//actionTv.setImageIcon(action.getIcon());
									actionTv.setOnClickListener(new OnClickListener() {

										@Override
										public void onClick(View p1) {
											try {
												action.actionIntent.send();
											} catch (PendingIntent.CanceledException e) {
											}
										}
									});

									notifActionsLayout.addView(actionTv, lp);

								} catch (PackageManager.NameNotFoundException e) {

								}
							}
						} else {
							for (final Notification.Action action : actions) {
								TextView actionTv = new TextView(NotificationService.this);
								actionTv.setSingleLine(true);
								actionTv.setText(action.title);
								actionTv.setTextColor(Color.WHITE);
								actionTv.setOnClickListener((View p1) -> {
									try {
										action.actionIntent.send();
										notificationLayout.setVisibility(View.GONE);
										notificationLayout.setAlpha(0);
									} catch (PendingIntent.CanceledException e) {
									}
								});

								notifActionsLayout.addView(actionTv, lp);
							}
						}
					}

					notifCancelBtn.setOnClickListener((View p1) -> {
						notificationLayout.setVisibility(View.GONE);

						if (sbn.isClearable())
							cancelNotification(sbn.getKey());
					});

					notificationLayout.setOnClickListener((View p1) -> {
						notificationLayout.setVisibility(View.GONE);
						notificationLayout.setAlpha(0);

						PendingIntent intent = notification.contentIntent;
						if (intent != null) {
							try {
								intent.send();
								if (sbn.isClearable())
									cancelNotification(sbn.getKey());
							} catch (PendingIntent.CanceledException e) {
							}
						}
					});

					notificationLayout.setOnLongClickListener((View p1) -> {
						sp.edit()
								.putString("blocked_notifications",
										sp.getString("blocked_notifications", "").trim() + " " + sbn.getPackageName())
								.commit();
						notificationLayout.setVisibility(View.GONE);
						notificationLayout.setAlpha(0);
						Toast.makeText(NotificationService.this, R.string.silenced_notifications, Toast.LENGTH_LONG)
								.show();

						if (sbn.isClearable())
							cancelNotification(sbn.getKey());
						return true;
					});

					notificationLayout.animate().alpha(1).setDuration(300)
							.setInterpolator(new AccelerateDecelerateInterpolator())
							.setListener(new AnimatorListenerAdapter() {

								@Override
								public void onAnimationStart(Animator animation) {
									notificationLayout.setVisibility(View.VISIBLE);
								}

							});

					if (sp.getBoolean("enable_notification_sound", false))
						DeviceUtils.playEventSound(this, "notification_sound");

					hideNotification();
				}
				
			}
		}
	}

	public void hideNotification() {
		handler.removeCallbacksAndMessages(null);
		handler.postDelayed(() -> {
			notificationLayout.animate().alpha(0).setDuration(300)
					.setInterpolator(new AccelerateDecelerateInterpolator()).setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							notificationLayout.setVisibility(View.GONE);
						}
					});
		}, Integer.parseInt(sp.getString("notification_timeout", "5000")));

	}

	public boolean isBlackListed(String packageName) {
		String ignoredPackages = sp.getString("blocked_notifications", "android");
		return ignoredPackages.contains(packageName);
	}

	private void updateNotificationCount() {
		int count = 0;
		int cancelableCount = 0;

		StatusBarNotification[] notifications = getActiveNotifications();

		for (StatusBarNotification notification : notifications) {
			if (notification != null && (notification.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) == 0) {
				count++;

				if (notification.isClearable())
					cancelableCount++;
			}
			if (Utils.notificationPanelVisible)
				cancelAllBtn.setVisibility(cancelableCount > 0 ? View.VISIBLE : View.INVISIBLE);

		}
		sendBroadcast(new Intent(getPackageName() + ".NOTIFICATION_PANEL").putExtra("action", "COUNT_CHANGED")
				.putExtra("count", count));

	}

	public void showNotificationPanel() {
		WindowManager.LayoutParams lp = Utils.makeWindowParams(Utils.dpToPx(context, 400), -2, context,
				preferLastDisplay);
		lp.gravity = Gravity.BOTTOM | Gravity.RIGHT;
		lp.y = Utils.dpToPx(context, 60);
		lp.x = Utils.dpToPx(context, 2);
		lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

		notificationPanel = LayoutInflater.from(context).inflate(R.layout.notification_panel, null);
		cancelAllBtn = notificationPanel.findViewById(R.id.cancel_all_n_btn);
		notificationsLv = notificationPanel.findViewById(R.id.notification_lv);
		notificationsLv.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
		notificationArea = notificationPanel.findViewById(R.id.notification_area);
		LinearLayout qsArea = notificationPanel.findViewById(R.id.qs_area);
		ImageView dontDisturbBtn = notificationPanel.findViewById(R.id.dont_disturb_btn);
		final ImageView orientationBtn = notificationPanel.findViewById(R.id.btn_orientation);
		ImageView touchModeBtn = notificationPanel.findViewById(R.id.btn_touch_mode);
		ImageView screenshotBtn = notificationPanel.findViewById(R.id.btn_screenshot);
		ImageView screencapBtn = notificationPanel.findViewById(R.id.btn_screencast);
		ImageView settingsBtn = notificationPanel.findViewById(R.id.btn_settings);

		ColorUtils.applySecondaryColor(context, sp, dontDisturbBtn);
		ColorUtils.applySecondaryColor(context, sp, orientationBtn);
		ColorUtils.applySecondaryColor(context, sp, touchModeBtn);
		ColorUtils.applySecondaryColor(context, sp, screencapBtn);
		ColorUtils.applySecondaryColor(context, sp, screenshotBtn);
		ColorUtils.applySecondaryColor(context, sp, settingsBtn);

		touchModeBtn.setOnClickListener((View p1) -> {
			hideNotificationPanel();
			if (sp.getBoolean("tablet_mode", false)) {
				Utils.toggleBuiltinNavigation(sp.edit(), false);
				sp.edit().putBoolean("app_menu_fullscreen", false).commit();
				sp.edit().putBoolean("tablet_mode", false).commit();
				Toast.makeText(context, R.string.tablet_mode_off, Toast.LENGTH_SHORT).show();
			} else {
				Utils.toggleBuiltinNavigation(sp.edit(), true);
				sp.edit().putBoolean("app_menu_fullscreen", true).commit();
				sp.edit().putBoolean("tablet_mode", true).commit();
				Toast.makeText(context, R.string.tablet_mode_on, Toast.LENGTH_SHORT).show();
			}
		});

		orientationBtn.setImageResource(sp.getBoolean("lock_landscape", true) ? R.drawable.ic_screen_rotation_off
				: R.drawable.ic_screen_rotation_on);

		orientationBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View p1) {
				sp.edit().putBoolean("lock_landscape", !sp.getBoolean("lock_landscape", true)).commit();
				orientationBtn
						.setImageResource(sp.getBoolean("lock_landscape", true) ? R.drawable.ic_screen_rotation_off
								: R.drawable.ic_screen_rotation_on);
			}
		});

		screenshotBtn.setOnClickListener((View p1) -> {
			hideNotificationPanel();
			if (Build.VERSION.SDK_INT >= 28) {
				sendBroadcast(
						new Intent(getPackageName() + ".NOTIFICATION_PANEL").putExtra("action", "TAKE_SCREENSHOT"));
			} else
				DeviceUtils.sendKeyEvent(KeyEvent.KEYCODE_SYSRQ);
		});

		screencapBtn.setOnClickListener((View p1) -> {
			hideNotificationPanel();
			launchApp("standard", sp.getString("app_rec", ""));
		});

		settingsBtn.setOnClickListener((View p1) -> {
			hideNotificationPanel();
			launchApp("standard", getPackageName());
		});

		cancelAllBtn.setOnClickListener((View p1) -> cancelAllNotifications());

		dontDisturbBtn.setImageResource(sp.getBoolean("do_not_disturb", false) ? R.drawable.ic_do_not_disturb
				: R.drawable.ic_do_not_disturb_off);
		dontDisturbBtn.setOnClickListener((View p1) -> {
			boolean dontDisturb = sp.getBoolean("do_not_disturb", false);
			sp.edit().putBoolean("do_not_disturb", !dontDisturb).commit();
			dontDisturbBtn
					.setImageResource(!dontDisturb ? R.drawable.ic_do_not_disturb : R.drawable.ic_do_not_disturb_off);
		});

		ColorUtils.applyMainColor(NotificationService.this, sp, notificationArea);
		ColorUtils.applyMainColor(NotificationService.this, sp, qsArea);
		wm.addView(notificationPanel, lp);
		//ColorUtils.applyColor(notificationsLv.getDivider(), ColorUtils.getMainColors(sp, this)[4]);
		notificationsLv.addItemDecoration(new DividerItemDecoration(context, LinearLayoutManager.VERTICAL));

		updateNotificationPanel();

		notificationPanel.setOnTouchListener((View p1, MotionEvent p2) -> {
			if (p2.getAction() == MotionEvent.ACTION_OUTSIDE
					&& (p2.getY() < notificationPanel.getMeasuredHeight() || p2.getX() < notificationPanel.getX())) {
				hideNotificationPanel();
			}
			return false;
		});

		Utils.notificationPanelVisible = true;
		updateNotificationCount();
	}

	public void launchApp(String mode, String app) {
		sendBroadcast(new Intent(getPackageName() + ".HOME").putExtra("action", "launch").putExtra("mode", mode)
				.putExtra("app", app));
	}

	public void hideNotificationPanel() {
		wm.removeView(notificationPanel);
		Utils.notificationPanelVisible = false;
		notificationsLv = null;
		notificationPanel = null;
		cancelAllBtn = null;
	}

	public void updateNotificationPanel() {
		NotificationAdapter adapter = new NotificationAdapter(context, getActiveNotifications(), this);
		notificationsLv.setAdapter(adapter);
		ViewGroup.LayoutParams lp = notificationsLv.getLayoutParams();

		int count = adapter.getItemCount();

		if (count > 3) {
			//View item = notificationsLv.findViewHolderForAdapterPosition(0);
			//item.measure(0, 0);
			//lp.height = 3 * item.getMeasuredHeight();
			lp.height = Utils.dpToPx(context, 350);
		} else
			lp.height = -2;

		notificationArea.setVisibility(count == 0 ? View.GONE : View.VISIBLE);

		notificationsLv.setLayoutParams(lp);

	}

	class DockServiceReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context p1, Intent p2) {
			String action = p2.getStringExtra("action");
			if (action.equals("SHOW_NOTIF_PANEL"))
				showNotificationPanel();
			else
				hideNotificationPanel();
		}

	}

	@Override
	public void onNotificationClicked(StatusBarNotification sbn, View item) {
		final Notification notification = sbn.getNotification();

		if (notification.contentIntent != null) {
			hideNotificationPanel();
			try {
				notification.contentIntent.send();

				if (sbn.isClearable())
					cancelNotification(sbn.getKey());
			} catch (PendingIntent.CanceledException e) {
			}
		}
	}

	@Override
	public void onNotificationLongClicked(StatusBarNotification notification, View item) {
	}

	@Override
	public void onNotificationCancelClicked(StatusBarNotification notification, View item) {
		cancelNotification(notification.getKey());
	}
}
