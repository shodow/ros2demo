package com.example.demo1.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.demo1.service.Ros2WebSocketService

class BootReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // 启动WebSocket服务
            val serviceIntent = Intent(context, Ros2WebSocketService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}    