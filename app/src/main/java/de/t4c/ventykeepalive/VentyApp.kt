package de.t4c.ventykeepalive

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class VentyApp : Application() {
    companion object {
        const val CHANNEL_ID = "venty_watchdog_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Venty Keep Alive Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live status of Venty connection and keep-alive service"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
