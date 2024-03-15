package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationHelper(var co:Context,var msg:String,var linemsg: MutableList<String>,var titleOfNoti:String) {
    private val CHANNEL_ID = "message id"
    private val NOTIFICATION_ID = 123
    /**set notification**/
    fun Notification() {
        createNotificationChannel()
        val senInt = Intent(co,MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingInt = PendingIntent.getActivities(co, 0, arrayOf(senInt), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        /**set notification dialog**/

        if(linemsg.size == 4){
            val isnotification = NotificationCompat.Builder(co,CHANNEL_ID)
                .setSmallIcon(R.drawable.barcode)
                .setContentTitle(titleOfNoti)
                .setContentText(msg)
                .setContentIntent(pendingInt)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setStyle(NotificationCompat.InboxStyle()
                    .addLine(linemsg[0])
                    .addLine(linemsg[1])
                    .addLine(linemsg[2])
                    .addLine(linemsg[3]))
                .build()
            NotificationManagerCompat.from(co)
                .notify(NOTIFICATION_ID,isnotification)
        }
        if(linemsg.size == 3){
            val isnotification = NotificationCompat.Builder(co,CHANNEL_ID)
                .setSmallIcon(R.drawable.barcode)
                .setContentTitle(titleOfNoti)
                .setContentText(msg)
                .setContentIntent(pendingInt)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setStyle(NotificationCompat.InboxStyle()
                    .addLine(linemsg[0])
                    .addLine(linemsg[1])
                    .addLine(linemsg[2]))
                .build()
            NotificationManagerCompat.from(co)
                .notify(NOTIFICATION_ID,isnotification)
        }
        if(linemsg.size == 2){
            val isnotification = NotificationCompat.Builder(co,CHANNEL_ID)
                .setSmallIcon(R.drawable.barcode)
                .setContentTitle(titleOfNoti)
                .setContentText(msg)
                .setContentIntent(pendingInt)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setStyle(NotificationCompat.InboxStyle()
                    .addLine(linemsg[0])
                    .addLine(linemsg[1]))
                .build()
            NotificationManagerCompat.from(co)
                .notify(NOTIFICATION_ID,isnotification)
        }
        if(linemsg.size == 1){
            val isnotification = NotificationCompat.Builder(co,CHANNEL_ID)
                .setSmallIcon(R.drawable.barcode)
                .setContentTitle(titleOfNoti)
                .setContentText(msg)
                .setContentIntent(pendingInt)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setStyle(NotificationCompat.InboxStyle()
                    .addLine(linemsg[0]))
                .build()
            NotificationManagerCompat.from(co)
                .notify(NOTIFICATION_ID,isnotification)
        }

    }
    /*create createNotificationChannel*/
    private fun createNotificationChannel(){
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            val name = CHANNEL_ID
            val descrip = "Channel description"
            val imports = NotificationManager.IMPORTANCE_DEFAULT
            val channels = NotificationChannel(CHANNEL_ID,name,imports).apply {
                description = descrip
            }
            val notificationManager = co.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channels)

        }
    }
}