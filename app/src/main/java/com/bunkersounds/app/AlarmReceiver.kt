package com.bunkersounds.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("bunker_alarm", "Bunker Sounds alarm", NotificationManager.IMPORTANCE_HIGH))
        val preset = intent.getStringExtra("wake_preset") ?: "Bunker Sounds"
        val sound = intent.getStringExtra("wake_sound")
        val music = intent.getStringExtra("wake_music")
        manager.notify(42, NotificationCompat.Builder(context, "bunker_alarm").setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Wake up with $preset").setContentText("Your gradual wake-up session is ready.").setDefaults(NotificationCompat.DEFAULT_ALL).setCategory(NotificationCompat.CATEGORY_ALARM).setAutoCancel(true).build())
        context.startActivity(Intent(context, MainActivity::class.java).putExtra("wake_preset", preset).putExtra("wake_sound", sound).putExtra("wake_music", music).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }
}
