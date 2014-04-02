package com.cajama.background;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.cajama.malarialite.R;

// DEPRECATED
public class NotificationHelper {
    private Context mContext;
    private int NOTIFICATION_ID = 1;
    private Notification mNotification;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder nh;
    private PendingIntent mContentIntent;
    private CharSequence mContentTitle;
    public NotificationHelper(Context context)
    {
        mContext = context;
    }

    /**
     * Put the notification into the status bar
     */
    public void createNotification(int flags) {
        //get the notification manager
        String contentTitle = null;
        String contentText = null;
        switch(flags) {
            case 1:
                contentTitle = "Updating Malaria App...";
                contentText = "";
                break;
            case 2:
                contentTitle = "Sending queued files";
                contentText = "";
                break;
            default: break;
        }

        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        nh = new NotificationCompat.Builder(mContext)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_launcher);

        Intent notificationIntent = new Intent();
        mContentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);


        //show the notification
        if (contentText != null && contentTitle != null) mNotificationManager.notify(NOTIFICATION_ID, nh.build());

    }

    /**
     * Receives progress updates from the background task and updates the status bar notification appropriately
     * @param percentageComplete
     */
    public void progressUpdate(int percentageComplete) {
        //build up the new status message
        CharSequence contentText = percentageComplete + "% complete";
        //publish it to the status bar

        nh.setContentText(contentText);

        mNotificationManager.notify(NOTIFICATION_ID, nh.build());
    }

    /**
     * called when the background task is complete, this removes the notification from the status bar.
     * We could also use this to add a new ‘task complete’ notification
     */
    public void completed(int flags)    {
        //remove the notification from the status bar
        //mNotificationManager.cancel(NOTIFICATION_ID);
        String toastContent = "";

        switch (flags) {
            case 1:
                toastContent = "Updated app downloaded";
                break;
            case 2:
                toastContent = "Sent queued reports";
                break;
            default: break;
        }

        //Toast.makeText(mContext, toastContent, Toast.LENGTH_LONG).show();
        /*nh.setContentTitle("Updated Malaria App.");
        nh.setContentText("Complete.");*/
        mNotificationManager.cancel(NOTIFICATION_ID);

    }
}