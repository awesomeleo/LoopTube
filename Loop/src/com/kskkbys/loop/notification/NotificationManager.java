package com.kskkbys.loop.notification;

import android.app.Notification;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;

import com.kskkbys.loop.R;

public class NotificationManager {

	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	private static final int NOTIFICATION_ID = 1000;

	/**
	 * Show Now Playing notification
	 * @param context
	 * @param videoTitle
	 * @param isPlaying
	 */
	public static void show(Context context, String videoTitle, boolean isPlaying) {

		android.app.NotificationManager nm = (android.app.NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

		// In this sample, we'll use the same text for the ticker and the expanded notification
		CharSequence text = "Now Playing: " + videoTitle;

		NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
		.setContentIntent(PendingIntentFactory.getLaunchIntent(context))
		.setSmallIcon(R.drawable.ic_stat_name)
		.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher))
		.setTicker(text)
		.setContentTitle(context.getText(R.string.loop_app_name))
		.setContentText(text)
		.setWhen(System.currentTimeMillis())
		.setOngoing(true);

		/*
		// 4.4で動かないので一旦外す
		if (isPlaying) {
			builder
			.addAction(R.drawable.rewind, "Prev", PendingIntentFactory.getPrevIntent(context.getApplicationContext()))
			.addAction(R.drawable.pause, "Pause", PendingIntentFactory.getPauseIntent(context.getApplicationContext()))
			.addAction(R.drawable.forward, "Next", PendingIntentFactory.getNextIntent(context.getApplicationContext()));
		} else {
			builder
			.addAction(R.drawable.rewind, "Prev", PendingIntentFactory.getPrevIntent(context))
			.addAction(R.drawable.play, "Play", PendingIntentFactory.getPlayIntent(context))
			.addAction(R.drawable.forward, "Next", PendingIntentFactory.getNextIntent(context));
		}*/

		Notification notification = builder.build();

		// Send the notification.
		nm.notify(NOTIFICATION_ID, notification);
	}

	/**
	 * Cancel the notification
	 * @param context
	 */
	public static void cancel(Context context) {
		android.app.NotificationManager nm = (android.app.NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(NOTIFICATION_ID);
	}

}
