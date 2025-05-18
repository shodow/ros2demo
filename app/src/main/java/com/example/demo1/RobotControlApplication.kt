package com.example.demo1
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.demo1.service.PatrolWorker
import com.example.demo1.service.Ros2WebSocketService
import javax.inject.Inject

@HiltAndroidApp
class RobotControlApplication: Application()
//    , Configuration.Provider
{
    private var TAG = "RobotControlApplication"

//    @Inject
//    lateinit var patrolWorkerFactory : PatrolWorker.PatrolWorkerFactory
    // 主构造函数（系统自动调用，无需手动实现）

    // init 块：在构造函数执行时调用（可选）
    init {
        // 轻量级初始化逻辑（如配置全局参数）
    }

    // 重写 onCreate：用于重量级初始化（如数据库、第三方库初始化）
    override fun onCreate() {
        super.onCreate()
        // 在这里进行应用级初始化
        initGlobalConfig(this)
    }

    private fun initGlobalConfig(context: Context) {
        // 具体初始化逻辑（如初始化日志、网络库等）
        Log.d(TAG, "initGlobalConfig")
        // 开启服务
        // 启动服务（例如在 Application 类或主 Activity 中）
//        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
//        val serverIp = prefs.getString("websocket_server_url", "ws://192.168.1.100:9090")
//        Log.d(TAG, "serverIp: $serverIp")
//        val intent = Intent(context, Ros2WebSocketService::class.java)
//        intent.putExtra("serverUrl", serverIp)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            context.startForegroundService(intent)
//        } else {
//            context.startService(intent)
//        }
        // 停止服务
//        val intent = Intent(context, TaskCheckService::class.java)
//        context.stopService(intent)
    }
//    override fun getWorkManagerConfiguration() =
//        Configuration.Builder()
//            .setMinimumLoggingLevel(Log.DEBUG)
//            .setWorkerFactory(patrolWorkerFactory)
//            .build()
}