package com.medhavistudypoint.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "मेधावी स्टडी पॉइंट"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "नई अपडेट / नोटिस उपलब्ध है!"

        showNotification(title, body)
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "medhavi_alerts_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 🔊 डिफ़ॉल्ट नोटिफिकेशन टोन
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Android 8.0+ के लिए साउंड और वाइब्रेशन चैनल
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Class & Notice Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Live class, tests and notes notifications"
                enableVibration(true)
                vibrationPattern = longArrayOf(100, 200, 300, 400)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 👉 क्लिक करने पर MainActivity खुले और सीधे नोटिस बॉक्स ट्रिगर हो
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_NOTICE_DIALOG", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(100, 200, 300, 400))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }
}