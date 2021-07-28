package cu.axel.smartdock.services;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View.OnHoverListener;
import android.view.MotionEvent;
import cu.axel.smartdock.widgets.HoverInterceptorLayout;
import cu.axel.smartdock.R;

public class NotificationService extends NotificationListenerService
{
	private WindowManager wm;
	private WindowManager.LayoutParams layoutParams;
	private HoverInterceptorLayout notificationLayout;
	private TextView notifTitle,notifText;
	private ImageView notifIcon,notifCancelBtn;
	private Handler handler;
    private SharedPreferences sp;

	@Override
	public void onCreate()
	{
		super.onCreate();

        sp = PreferenceManager.getDefaultSharedPreferences(this);
		wm = (WindowManager) getSystemService(WINDOW_SERVICE);
		layoutParams = new WindowManager.LayoutParams();
		layoutParams.width = 300;
		layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		layoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
		layoutParams.format = PixelFormat.TRANSLUCENT;
		layoutParams.y = 5;
		layoutParams.x = 5;
		layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |  WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
		}
		else
			layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;

		notificationLayout = (HoverInterceptorLayout) LayoutInflater.from(this).inflate(R.layout.notification, null);
		notificationLayout.setVisibility(View.GONE);

		notifTitle = notificationLayout.findViewById(R.id.notif_title_tv);
		notifText = notificationLayout.findViewById(R.id.notif_text_tv);
		notifIcon = notificationLayout.findViewById(R.id.notif_icon_iv);
        notifCancelBtn = notificationLayout.findViewById(R.id.notif_close_btn);

		wm.addView(notificationLayout, layoutParams);

		handler = new Handler();
        notificationLayout.setAlpha(0);

        notificationLayout.setOnHoverListener(new OnHoverListener(){

                @Override
                public boolean onHover(View p1, MotionEvent p2)
                {
                    if (p2.getAction() == MotionEvent.ACTION_HOVER_ENTER)
                    {
                        notifCancelBtn.setVisibility(View.VISIBLE);
                        handler.removeCallbacksAndMessages(null);
                    }
                    else if (p2.getAction() == MotionEvent.ACTION_HOVER_EXIT)
                    {
                        new Handler().postDelayed(new Runnable(){

                                @Override
                                public void run()
                                {
                                    notifCancelBtn.setVisibility(View.INVISIBLE);
                                }
                            }, 200);
                        hideNotification();
                    }
                    return false;
                }
            });
        notifCancelBtn.setOnClickListener(new OnClickListener(){

                @Override
                public void onClick(View p1)
                {
                    notificationLayout.setVisibility(View.GONE);
                }


            });

	}

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn)
    {
        super.onNotificationRemoved(sbn);
        updateNotificationCount();
    }


	@Override
	public void onNotificationPosted(StatusBarNotification sbn)
	{
		super.onNotificationPosted(sbn);

        updateNotificationCount();

		final Notification notification = sbn.getNotification();

		if (sbn.isOngoing() && !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_show_ongoing", false))
		{}
		else if (notification.contentView == null && !isBlackListed(sbn.getPackageName()))
		{
			Bundle extras=notification.extras;

			String notificationTitle = extras.getString(Notification.EXTRA_TITLE);
			Bitmap notificationLargeIcon = ((Bitmap) extras.getParcelable(Notification.EXTRA_LARGE_ICON));
			CharSequence notificationText = extras.getCharSequence(Notification.EXTRA_TEXT);

            switch (sp.getString("pref_theme", "dark"))
            {
                case "pref_theme_dark":
                    notifIcon.setBackgroundResource(R.drawable.circle_solid_dark);
                    notificationLayout.setBackgroundResource(R.drawable.round_rect_solid_dark);
                    notifCancelBtn.setBackgroundResource(R.drawable.circle_solid_dark);
                    break;
                case "pref_theme_black":
                    notifIcon.setBackgroundResource(R.drawable.circle_solid_black);
                    notificationLayout.setBackgroundResource(R.drawable.round_rect_solid_black);
                    notifCancelBtn.setBackgroundResource(R.drawable.circle_solid_black);
                    break;
                case "pref_theme_transparent":
                    notifIcon.setBackgroundResource(R.drawable.circle_transparent);
                    notificationLayout.setBackgroundResource(R.drawable.round_rect_transparent);
                    notifCancelBtn.setBackgroundResource(R.drawable.circle_transparent);
                    break;
            }


			if (notificationLargeIcon == null)
			{
				try
				{
					Drawable notificationIcon = getPackageManager().getApplicationIcon(sbn.getPackageName());
					notifIcon.setPadding(12, 12, 12, 12);
					notifIcon.setImageDrawable(notificationIcon);

				}
				catch (PackageManager.NameNotFoundException e)
				{}
			}
			else
			{
				notifIcon.setPadding(8, 8, 8, 8);
				notifIcon.setImageBitmap(notificationLargeIcon);

			}
			notifTitle.setText(notificationTitle);
			notifText.setText(notificationText);

			notificationLayout.setOnClickListener(new OnClickListener(){

					@Override
					public void onClick(View p1)
					{
                        notificationLayout.setVisibility(View.GONE);
                        notificationLayout.setAlpha(0);

						PendingIntent intent = notification.contentIntent;
						if (intent != null)
						{
							try
							{
								intent.send();
							}
							catch (PendingIntent.CanceledException e)
							{}}
					}
				});

			notificationLayout.animate()
				.alpha(1)
				.setDuration(300)
				.setInterpolator(new AccelerateDecelerateInterpolator())
				.setListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationStart(Animator animation)
					{
						notificationLayout.setVisibility(View.VISIBLE);
					}
				});

            hideNotification();
        }
	}

    public void hideNotification()
    {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable(){

                @Override
                public void run()
                {
                    notificationLayout.animate()
                        .alpha(0)
                        .setDuration(300)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation)
                            {
                                notificationLayout.setVisibility(View.GONE);
                                notificationLayout.setAlpha(0);
                            }
                        });

                }
            }, Integer.parseInt(sp.getString("pref_notification_timeout", "5000")));

    }

    public boolean isBlackListed(String packageName)
    {
        String ignoredPackages = sp.getString("pref_blocked_notifications", "android");
        return ignoredPackages.contains(packageName);
    }

    private void updateNotificationCount()
    {
        int count = 0;

        StatusBarNotification[] notifications;
        try
        {
            notifications = getActiveNotifications();
        }
        catch (Exception e)
        {
            notifications = new StatusBarNotification[0];
        }

        if (notifications != null)
        {
            for (StatusBarNotification notification : notifications)
            {
                if (notification != null
                    && (notification.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) == 0
                    && notification.isClearable()) count++;
            }
            sendBroadcast(new Intent(getPackageName() + ".NOTIFICATION_COUNT_CHANGED").putExtra("count", count));
        }
        else
        {
            //Toast.makeText(this,"null",5000).show();
        }

    }


}
