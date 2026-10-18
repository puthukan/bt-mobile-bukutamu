package com.puthukan.bukutamu

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val namaTamu = intent.getStringExtra("EXTRA_NAMA") ?: "Tamu"
        val tanggalAcara = intent.getStringExtra("EXTRA_TANGGAL") ?: ""

        val channelId = "channel_buku_tamu"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Buat Notification Channel untuk Android 8.0 (Oreo) ke atas
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pengingat Acara Tamu",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat tanggal acara tamu"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Buat Tampilan Notifikasi
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("Pengingat Acara Tamu")
            .setContentText("Hari ini ada acara untuk tamu: $namaTamu ($tanggalAcara)")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Pengingat jadwal acara tamu atas nama $namaTamu pada tanggal $tanggalAcara."))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // Tampilkan Notifikasi
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, builder.build())
    }
}