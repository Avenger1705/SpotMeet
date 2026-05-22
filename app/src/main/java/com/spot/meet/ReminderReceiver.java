package com.spot.meet;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

/**
 * BroadcastReceiver that fires when the AlarmManager triggers an event reminder.
 * Shows a high-priority notification that taps to open EventDetailActivity.
 */
public class ReminderReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID    = "spotmeet_reminders";
    public static final String EXTRA_TITLE   = "event_title";
    public static final String EXTRA_EVENT_ID = "event_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title   = intent.getStringExtra(EXTRA_TITLE);
        String eventId = intent.getStringExtra(EXTRA_EVENT_ID);
        if (title == null) title = "Upcoming Event";

        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Ensure channel exists (safe to call multiple times)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Event Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Reminders for your SpotMeet events");
            channel.enableVibration(true);
            nm.createNotificationChannel(channel);
        }

        // Tap notification → open EventDetailActivity
        Intent tapIntent = new Intent(context, EventDetailActivity.class);
        tapIntent.putExtra("EVENT_ID", eventId);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                context,
                eventId != null ? eventId.hashCode() : 0,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("⏰  SpotMeet Reminder")
                        .setContentText("\"" + title + "\" starts in 1 hour!")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText("Don't forget! \"" + title
                                        + "\" is starting in 1 hour. Tap to view details."))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setAutoCancel(true)
                        .setContentIntent(pi);

        int notifId = eventId != null ? eventId.hashCode() : (int) System.currentTimeMillis();
        nm.notify(notifId, builder.build());
    }
}
