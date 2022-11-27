package com.whiture.apps.tamil.thousand.nights

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.*

class NotificationService: FirebaseMessagingService() {

    override fun onMessageReceived(bundle: RemoteMessage) {
        super.onMessageReceived(bundle)
        bundle.notification?.let {
            Handler(Looper.getMainLooper()).post { notify(bundle) }
        }
    }

    private fun notify(bundle: RemoteMessage) {
        // we will forward it to the Main Activity
        val data = bundle.data
        var intent = Intent(this, MainActivity::class.java).apply {
            // for app promotional
            data["app_id"]?.let { appId ->
                putExtra("app_id", appId)
                data["app_title"]?.let { putExtra("app_title", it) }
                data["app_description"]?.let { putExtra("app_description", it) }
                data["app_image_url"]?.let { putExtra("app_image_url", it) }
            }
            // for youtube video
            data["is_youtube"]?.let {
                putExtra("is_youtube", true)
                data["id"]?.let { putExtra("id", it) }
            }
            // for html link
            data["html_link"]?.let { putExtra("html_link", it) }
            // for articles
            data["tamil_articles"]?.let { _ ->
                putExtra("tamil_articles", true)
                data["tag_id"]?.let { putExtra("tag_id", it) }
                data["category_id"]?.let { putExtra("category_id", it) }
                data["article_id"]?.let { putExtra("article_id", it) }
                data["keyword"]?.let { putExtra("keyword", it) }
            }
            // add flags
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }

        val notificationPendingIntent = PendingIntent.getActivity(this, 0,
            intent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("ID_NOTIF_PROMO", "NOTIF_PROMO_MESSAGE",
                NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
            NotificationCompat.Builder(this, channel.id)
        }
        else {
            NotificationCompat.Builder(this)
        }

        builder.setContentTitle(bundle.notification?.title) //title
            .setContentText(bundle.notification?.body)  //message
            .setContentIntent(notificationPendingIntent)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        notificationManager.notify(Date().time.toInt(), builder.build())
    }

}

