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
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import cu.axel.smartdock.R;
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

public class NotificationService extends NotificationListenerService {
	private WindowManager wm;
	private HoverInterceptorLayout notificationLayout;
	private TextView notifTitle, notifText;
	private ImageView notifIcon, notifCancelBtn;
	private Handler handler;
	private SharedPreferences sp;
	private DockServiceReceiver dockReceiver;
	private View notificationPanel;
	private ListView notificationsLv;
	private NotificationManager nm;
	private ImageButton cancelAllBtn;
    private LinearLayout notifActionsLayout;

	@Override
	public void onCreate() {
		super.onCreate();

		sp = PreferenceManager.getDefaultSharedPreferences(this);
		wm = (WindowManager) getSystemService(WINDOW_SERVICE);
		nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		WindowManager.LayoutParams lp = Utils.makeWindowParams(Utils.dpToPx(this, 300), -2);
        lp.x = 5;
        if (sp.getBoolean("notification_bottom", false)) {
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

		handler = new Handler();
		notificationLayout.setAlpha(0);

		notificationLayout.setOnHoverListener(new OnHoverListener() {

                @Override
                public boolean onHover(View p1, MotionEvent p2) {
                    if (p2.getAction() == MotionEvent.ACTION_HOVER_ENTER) {
                        notifCancelBtn.setVisibility(View.VISIBLE);
                        handler.removeCallbacksAndMessages(null);
                    } else if (p2.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
                        new Handler().postDelayed(new Runnable() {

                                @Override
                                public void run() {
                                    notifCancelBtn.setVisibility(View.INVISIBLE);
                                }
                            }, 200);
                        hideNotification();
                    }
                    return false;
                }
            });

		dockReceiver = new DockServiceReceiver();
		registerReceiver(dockReceiver, new IntentFilter(getPackageName() + ".NOTIFICATION_PANEL"));


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
            final Notification notification = sbn.getNotification();

            if (sbn.isOngoing() && !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("show_ongoing", false)) {
            } else if (notification.contentView == null && !isBlackListed(sbn.getPackageName()) && !sbn.getPackageName().equals(AppUtils.currentApp)) {
                Bundle extras = notification.extras;

                String notificationTitle = extras.getString(Notification.EXTRA_TITLE);

                CharSequence notificationText = extras.getCharSequence(Notification.EXTRA_TEXT);

                ColorUtils.applySecondaryColor(sp, notifIcon);
                ColorUtils.applyMainColor(sp, notificationLayout);
                ColorUtils.applySecondaryColor(sp, notifCancelBtn);

                try {
                    Drawable notificationIcon = getPackageManager().getApplicationIcon(sbn.getPackageName());
                    notifIcon.setImageDrawable(notificationIcon);
                    ColorUtils.applyColor(notifIcon, ColorUtils.getDrawableDominantColor(notificationIcon));

                } catch (PackageManager.NameNotFoundException e) {
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
                                Resources res = getPackageManager().getResourcesForApplication(sbn.getPackageName());

                                Drawable drawable = res
                                    .getDrawable(res.getIdentifier(action.icon + "", "drawable", sbn.getPackageName()));
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
                            actionTv.setOnClickListener(new OnClickListener() {

                                    @Override
                                    public void onClick(View p1) {
                                        try {
                                            action.actionIntent.send();
                                            notificationLayout.setVisibility(View.GONE);
                                            notificationLayout.setAlpha(0);
                                        } catch (PendingIntent.CanceledException e) {
                                        }
                                    }
                                });

                            notifActionsLayout.addView(actionTv, lp);
                        }
                    }
                }

                notifCancelBtn.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View p1) {
                            notificationLayout.setVisibility(View.GONE);

                            if (sbn.isClearable())
                                cancelNotification(sbn.getKey());
                        }

                    });

                notificationLayout.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View p1) {
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
                        }
                    });

                notificationLayout.setOnLongClickListener(new OnLongClickListener() {

                        @Override
                        public boolean onLongClick(View p1) {
                            sp.edit()
                                .putString("blocked_notifications",
                                           sp.getString("blocked_notifications", "").trim() + " " + sbn.getPackageName())
                                .commit();
                            notificationLayout.setVisibility(View.GONE);
                            notificationLayout.setAlpha(0);
                            Toast.makeText(NotificationService.this, R.string.silenced_notifications, 5000).show();

                            if (sbn.isClearable())
                                cancelNotification(sbn.getKey());
                            return true;
                        }
                    });

                notificationLayout.animate().alpha(1).setDuration(300)
					.setInterpolator(new AccelerateDecelerateInterpolator()).setListener(new AnimatorListenerAdapter() {
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

	public void hideNotification() {
		handler.removeCallbacksAndMessages(null);
		handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    notificationLayout.animate().alpha(0).setDuration(300)
						.setInterpolator(new AccelerateDecelerateInterpolator())
						.setListener(new AnimatorListenerAdapter() {
							@Override
							public void onAnimationEnd(Animator animation) {
								notificationLayout.setVisibility(View.GONE);
								notificationLayout.setAlpha(0);
							}
						});

                }
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
            if (notification != null
                && (notification.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) == 0) {
                count++;

                if (notification.isClearable())
                    cancelableCount++;
            }
            if (Utils.notificationPanelVisible)
                cancelAllBtn.setVisibility(cancelableCount > 0 ? View.VISIBLE : View.INVISIBLE);

        }
        sendBroadcast(new Intent(getPackageName() + ".NOTIFICATION_COUNT_CHANGED").putExtra("count", count));
		
	}

	public void showNotificationPanel() {
		WindowManager.LayoutParams lp = Utils.makeWindowParams(Utils.dpToPx(this, 400), Utils.dpToPx(this, 400));
		lp.gravity = Gravity.BOTTOM | Gravity.RIGHT;
		lp.y = Utils.dpToPx(this, 60);
		lp.x = Utils.dpToPx(this, 2);
		lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

		notificationPanel = LayoutInflater.from(this).inflate(R.layout.notification_panel, null);
		cancelAllBtn = notificationPanel.findViewById(R.id.cancel_all_n_btn);
		notificationsLv = notificationPanel.findViewById(R.id.notification_lv);


		notificationsLv.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> p1, View p2, int p3, long p4) {
                    final StatusBarNotification sbn = (StatusBarNotification) p1.getItemAtPosition(p3);
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
            });

		cancelAllBtn.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View p1) {
                    cancelAllNotifications();
                }
            });

		ColorUtils.applyMainColor(sp, notificationPanel);
		wm.addView(notificationPanel, lp);

		updateNotificationPanel();

		notificationPanel.setOnTouchListener(new OnTouchListener() {

                @Override
                public boolean onTouch(View p1, MotionEvent p2) {
                    if (p2.getAction() == p2.ACTION_OUTSIDE && (p2.getY() < notificationPanel.getMeasuredHeight()
						|| p2.getX() < notificationPanel.getX())) {
                        hideNotificationPanel();
                    }
                    return false;
                }
            });

		Utils.notificationPanelVisible = true;
        updateNotificationCount();
	}

	public void hideNotificationPanel() {
		wm.removeView(notificationPanel);
		Utils.notificationPanelVisible = false;
		notificationsLv = null;
		notificationPanel = null;
		cancelAllBtn = null;
	}

	public void updateNotificationPanel() {
		notificationsLv.setAdapter(new NotificationAdapter(this, getActiveNotifications()));
	}

	class DockServiceReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context p1, Intent p2) {
			String action = p2.getStringExtra("action");
			if (action.equals("show"))
				showNotificationPanel();
			else
				hideNotificationPanel();
		}

	}

	class NotificationAdapter extends ArrayAdapter<StatusBarNotification> {
		private Context context;

		public NotificationAdapter(Context context, StatusBarNotification[] notifications) {
			super(context, R.layout.notification, notifications);
			this.context = context;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			final StatusBarNotification sbn = getItem(position);
			final Notification notification = sbn.getNotification();

			ViewHolder holder;

			if (convertView == null) {
				convertView = LayoutInflater.from(context).inflate(R.layout.notification_white, null);
				holder = new ViewHolder();
				holder.notifTitle = convertView.findViewById(R.id.notif_w_title_tv);
				holder.notifText = convertView.findViewById(R.id.notif_w_text_tv);
				holder.notifIcon = convertView.findViewById(R.id.notif_w_icon_iv);
				holder.notifCancelBtn = convertView.findViewById(R.id.notif_w_close_btn);
				//holder.notifPb = convertView.findViewById(R.id.notif_w_pb);
				holder.notifActionsLayout = convertView.findViewById(R.id.notif_actions_container);
				convertView.setTag(holder);

			} else
				holder = (ViewHolder) convertView.getTag();

			Notification.Action[] actions = notification.actions;
			Bundle extras = notification.extras;

			holder.notifActionsLayout.removeAllViews();

			if (actions != null) {
				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
				lp.weight = 1f;

				if (extras.get(Notification.EXTRA_MEDIA_SESSION) != null) {
					//lp.height = Utils.dpToPx(NotificationService.this, 30);
					for (final Notification.Action action : actions) {
						ImageView actionTv = new ImageView(NotificationService.this);
						try {
							Resources res = getPackageManager().getResourcesForApplication(sbn.getPackageName());

							Drawable drawable = res
                                .getDrawable(res.getIdentifier(action.icon + "", "drawable", sbn.getPackageName()));
							drawable.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
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

							holder.notifActionsLayout.addView(actionTv, lp);

						} catch (PackageManager.NameNotFoundException e) {

						}
					}
				} else {
					for (final Notification.Action action : actions) {
						TextView actionTv = new TextView(NotificationService.this);
						actionTv.setSingleLine(true);
						actionTv.setText(action.title);
						actionTv.setOnClickListener(new OnClickListener() {

                                @Override
                                public void onClick(View p1) {
                                    try {
                                        action.actionIntent.send();
                                    } catch (PendingIntent.CanceledException e) {
                                    }
                                }
                            });

						holder.notifActionsLayout.addView(actionTv, lp);
					}
				}
			}

			String notificationTitle = extras.getString(Notification.EXTRA_TITLE);
			CharSequence notificationText = extras.getCharSequence(Notification.EXTRA_TEXT);
			int progress = extras.getInt(Notification.EXTRA_PROGRESS);

			String p = progress != 0 ? " " + progress + "%" : "";

			holder.notifTitle.setText(notificationTitle + p);
			holder.notifText.setText(notificationText);

			try {
				Drawable notificationIcon = getPackageManager().getApplicationIcon(sbn.getPackageName());
				holder.notifIcon.setImageDrawable(notificationIcon);
                ColorUtils.applyColor(holder.notifIcon, ColorUtils.getDrawableDominantColor(notificationIcon));

			} catch (PackageManager.NameNotFoundException e) {
			}

			if (sbn.isClearable()) {

				holder.notifCancelBtn.setAlpha(1f);
				holder.notifCancelBtn.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View p1) {

                            if (sbn.isClearable())
                                cancelNotification(sbn.getKey());
                        }
                    });
			} else
				holder.notifCancelBtn.setAlpha(0f);

			return convertView;
		}

		private class ViewHolder {
			ImageView notifIcon, notifCancelBtn;
			TextView notifTitle, notifText;
			LinearLayout notifActionsLayout;
			ProgressBar notifPb;
		}

	}
}
