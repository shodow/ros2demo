package com.example.demo1

import VoiceWakeUpManager2
import androidx.lifecycle.lifecycleScope // 用于lifecycleScope
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.demo1.service.PatrolWorker
import com.example.demo1.service.PatrolWorker.Companion.EXTRA_TASK_ID
import com.example.demo1.service.PatrolWorker.Companion.WORK_NAME
import com.example.demo1.ui.MapActivity
import com.example.demo1.ui.PatrolTaskActivity
import com.example.demo1.ui.PositionListActivity
import com.example.demo1.ui.SettingsActivity
import com.example.demo1.ui.viewmodel.PatrolTaskViewModel
import com.example.demo1.ui.viewmodel.PositionViewModel
import com.example.demo1.ui.viewmodel.WebSocketViewModel
import dagger.hilt.android.AndroidEntryPoint

import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * 主页
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var TAG = "MainActivity"
//    private lateinit var wakeUpManager: VoiceWakeUpManager
    private lateinit var conversationManager: ConversationManager
    private lateinit var porcupineManager: VoiceWakeUpManager2
    private val porcupineAccessKey = "ciezDYHGF9BpCMKHFK/0EY/R08tqPqdrQ4UH2vLDVlJb0ReBuxYGPg=="
    private val keywordModelAssetPath = "Hello-Robot_en_android_v3_0_0.ppn"
//    lateinit var patrolWorker: PatrolWorker // 依赖注入
    private val taskViewModel: PatrolTaskViewModel by viewModels()
    private val positionViewModel: PositionViewModel by viewModels()
    private val webSocketViewModel: WebSocketViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 检查权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_CODE
            )
        } else {
            // 初始化语音唤醒功能
//            initManagers()

            // 跳转设置页面
            findViewById<Button>(R.id.btn_goto_setting).setOnClickListener {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }

            // 跳转ros控制页面
            findViewById<Button>(R.id.btn_goto_ros2).setOnClickListener {
                val intent = Intent(this, Ros2Activity::class.java)
                startActivity(intent)
            }

            // 跳转任务页面
            findViewById<Button>(R.id.btn_goto_task).setOnClickListener {
                val intent = Intent(this, PatrolTaskActivity::class.java)
                startActivity(intent)
            }

            // 跳转点位页面
            findViewById<Button>(R.id.btn_goto_position).setOnClickListener {
                val intent = Intent(this, PositionListActivity::class.java)
                startActivity(intent)
            }

            // 跳转地图页面
            findViewById<Button>(R.id.btn_goto_map).setOnClickListener {
                val intent = Intent(this, MapActivity::class.java)
                startActivity(intent)
            }

            // 开启定时任务
            findViewById<Button>(R.id.begin_work).setOnClickListener {
                beginWork()
            }
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun initManagers() {
        // 初始化 PorcupineManager
        porcupineManager = VoiceWakeUpManager2(this, this)
        porcupineManager.onWakeUpListener = {
            // 唤醒词检测成功的回调（在主线程）
            runOnUiThread {
                Toast.makeText(this, "唤醒词检测成功！", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "唤醒词检测成功")
                porcupineManager.stopListening()
                // 唤醒成功，开启豆包智能语音对话
//                startConversation()
            }
        }

        // 初始化 Porcupine 引擎
        porcupineManager.init(porcupineAccessKey, keywordModelAssetPath)

        // 初始化对话管理器
        conversationManager = ConversationManager(this)
    }

    // 开始智能语音对话
    private fun startConversation() {
        conversationManager.startListening { userMessage ->
            Log.d(TAG, "你说: $userMessage")
            showToast("你说: $userMessage")
            // 在协程中调用API
            lifecycleScope.launch(Dispatchers.IO) {
                val response = conversationManager.sendMessageToDoubao(userMessage)
                conversationManager.stopListening()
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "豆包回复: $response")
                    showToast("豆包回复: $response")
                    conversationManager.speakText(response)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        wakeUpManager.release()
        conversationManager.release()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initManagers()
        } else {
            showToast("需要麦克风权限才能使用语音功能")
            finish()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val RECORD_AUDIO_PERMISSION_CODE = 1001
    }

    private fun beginWork() {
        // 获取定时巡逻任务
        taskViewModel.allTasks.observe(this, Observer { tasks ->
            tasks.forEach { task ->
                if (task.isActive) {
                    // 对每个 task 执行操作，如更新 RecyclerView
                    Log.d(TAG, "处理任务: ${task.name}, hour = ${task.hour}, minute = ${task.minute}, second = ${task.second}, taskId = ${task.id}")
                    // 触发调度
                    schedulePatrolTask(
                        hour = task.hour,
                        minute = task.minute,
                        second = task.second,
                        taskId = task.id
                    )
                }
            }
        })
    }

    fun schedulePatrolTask(hour: Int, minute: Int, second: Int, taskId: Int) {
        val now = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
        }

        // 如果目标时间已经过去，则设置为明天
        if (targetTime.before(now)) {
            targetTime.add(Calendar.DAY_OF_YEAR, 1)
        }

        val delayMillis = targetTime.timeInMillis - now.timeInMillis
        // 创建workRequest
        val workRequest = OneTimeWorkRequestBuilder<PatrolWorker>()
//            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS) // 定时任务执行
            .setInitialDelay(2, TimeUnit.SECONDS) // 2s后执行
            .setInputData(workDataOf(EXTRA_TASK_ID to taskId))
            .addTag(WORK_NAME)
            .build()
        // 获取WorkManager单例，把workRequest推到队列里
        WorkManager.getInstance(applicationContext).enqueue(workRequest)
    }
}