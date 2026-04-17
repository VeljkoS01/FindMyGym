package com.findmygym.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.findmygym.app.R

object NotificationHelper {

    //ID kanala preko kog saljem notifikacije
    private const val CHANNEL_ID = "nearby_gym"

    fun ensureChannel(context: Context) {
        //Na Androidu 8+ notifikacije moraju da pripadaju nekom kanalu
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Nearby gyms",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        //Ako kanal vec postoji, createNotificationChannel ga nece duplirati
        manager.createNotificationChannel(channel)
    }

    fun showNearbyGym(context: Context, title: String, message: String) {
        // Pre slanja notifikacije osiguravamo se da kanal postoji
        ensureChannel(context)

        //Na Androidu 13+ mora da postoji dozvola za prikaz notifikacija
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            //Ako dozvola nije data, prekidamo funkciju bez slanja notifikacije
            if (!granted) return
        }

        //Kreiranje same notifikacije
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        // Prikaz notifikacije
        // ID pravimo na osnovu vremena da se nove notifikacije ne prepisuju medjusobno
        NotificationManagerCompat.from(context)
            .notify((System.currentTimeMillis() % 100000).toInt(), notif)
    }
}