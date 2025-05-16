package com.example.demo1
import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import android.util.Log

@HiltAndroidApp
class RobotControlApplication: Application() {
    private var TAG = "RobotControlApplication"
    // 主构造函数（系统自动调用，无需手动实现）

    // init 块：在构造函数执行时调用（可选）
    init {
        // 轻量级初始化逻辑（如配置全局参数）
    }

    // 重写 onCreate：用于重量级初始化（如数据库、第三方库初始化）
    override fun onCreate() {
        super.onCreate()
        // 在这里进行应用级初始化
        initGlobalConfig()
    }

    private fun initGlobalConfig() {
        // 具体初始化逻辑（如初始化日志、网络库等）
        Log.d(TAG, "");
    }
}