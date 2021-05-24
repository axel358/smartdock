package cu.axel.smartdock;
import android.app.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.service.notification.*;
import android.view.*;
import android.widget.*;
import android.content.pm.PackageManager.*;
import android.content.pm.*;
import android.view.animation.*;
import android.animation.*;
import android.content.*;
import android.view.View.*;
import android.app.PendingIntent.*;
import android.preference.*;

public class NotificationService extends NotificationListenerService
{
	private WindowManager wm;
	private WindowManager.LayoutParams layoutParams;
	private LinearLayout notificationLayout;
	private TextView notifTitle,notifText;
	private ImageView notifIcon;
	private Handler handler;

	@Override
	public void onCreate()
	{
		super.onCreate();
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

		notificationLayout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.notification, null);
		notificationLayout.setVisibility(View.GONE);

		notifTitle = notificationLayout.findViewById(R.id.notif_title_tv);
		notifText = notificationLayout.findViewById(R.id.notif_text_tv);
		notifIcon = notificationLayout.findViewById(R.id.notif_icon_iv);

		wm.addView(notificationLayout, layoutParams);

		notificationLayout.setAlpha(0);

		handler = new Handler();

	}


	@Override
	public void onNotificationPosted(StatusBarNotification sbn)
	{
		super.onNotificationPosted(sbn);

		final Notification notification = sbn.getNotification();

		if (sbn.isOngoing() && !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_show_ongoing", false))
		{}
		else if (notification.contentView == null && !Utils.isBlackListed(sbn.getPackageName()))
		{
			Bundle extras=notification.extras;

			String notificationTitle = extras.getString(Notification.EXTRA_TITLE);
			Bitmap notificationLargeIcon = ((Bitmap) extras.getParcelable(Notification.EXTRA_LARGE_ICON));
			CharSequence notificationText = extras.getCharSequence(Notification.EXTRA_TEXT);

			notifIcon.setBackgroundResource(R.drawable.circle_solid);

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

			wm.updateViewLayout(notificationLayout, layoutParams);
			handler.removeCallbacksAndMessages(null);
			handler.postDelayed(new Runnable(){

					@Override
					public void run()
					{
						notificationLayout.setVisibility(View.GONE);
					}
				}, 5000);
		}
	}

}
