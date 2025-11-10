package com.example.caats.utils;

import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.caats.R;
import com.example.caats.auth.LoginActivity; // Your main launcher activity
import com.example.caats.repository.StudentRepository; // Assuming you have this
import com.example.caats.ui.common.SplashActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    public static final String CHANNEL_ID = "CAATS_SESSION_CHANNEL";

    /**
     * Called when a message is received.
     * This is where you'll get the notification from your Supabase function.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = null;
        String body = null;

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a notification payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            title = remoteMessage.getData().get("title");
            body = remoteMessage.getData().get("body");
        }
        // Fallback for notification payload (if you ever send one by mistake)
        else if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message notification payload: " + remoteMessage.getNotification().getBody());
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        if (title != null && body != null) {
            sendLocalNotification(title, body);
        } else {
            Log.e(TAG, "Notification title or body is null.");
        }
    }

    /**
     * Called if the FCM registration token is updated.
     * This token must be sent to your Supabase backend.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        sendTokenToSupabase(token);
    }

    /**
     * send new FCM token to  Supabase .
     */
    private void sendTokenToSupabase(String token) {
        String authUid = PreferenceManager.getUserId(this);
        if (authUid == null || authUid.isEmpty()) {
            Log.w(TAG, "User not logged in, cannot send FCM token.");
            return;
        }

        // Call your repository to update the token
        StudentRepository.updateFcmToken(this, authUid, token, new StudentRepository.FcmTokenCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "FCM token updated in Supabase successfully.");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to update FCM token: " + error);
            }
        });
    }

    /**
     * Creates and displays a local notification on the device.
     */
    private void sendLocalNotification(String title, String body) {
            Intent intent = new Intent(this, SplashActivity.class);

            // This tells the app the user wants to go to the attendance screen.
            intent.putExtra("navigate_to", "MARK_ATTENDANCE");

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_logo)
                            .setContentTitle(title)
                            .setContentText(body)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent); // Set the new PendingIntent

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            int notificationId = (int) System.currentTimeMillis();

            try {
                notificationManager.notify(notificationId, notificationBuilder.build());
            } catch (SecurityException e) {
                Log.e(TAG, "Failed to show notification. POST_NOTIFICATIONS permission missing?", e);
            }
        }

    }