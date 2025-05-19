package com.example.demo1.service

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.demo1.data.entity.Position
import com.example.demo1.data.repository.PositionRepository
import kotlinx.coroutines.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.hilt.work.HiltWorker
import androidx.lifecycle.asLiveData
import com.example.demo1.ui.viewmodel.WebSocketViewModel
import androidx.lifecycle.Observer

@HiltWorker
class PatrolWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val positionRepository: PositionRepository,
//    private val webSocketService: Ros2WebSocketService
//    private val webSocketViewModel: WebSocketViewModel
) : CoroutineWorker(context, params) {


    companion object {
        private const val TAG = "PatrolWorker"
        const val WORK_NAME = "com.example.demo1.PATROL_WORK"
        const val EXTRA_TASK_ID = "task_id"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork...")
        // 所有要执行的任务内容都在这里
        return withContext(Dispatchers.IO) {
            try {
                val taskId = inputData.getInt(EXTRA_TASK_ID, -1)
                if (taskId == -1) {
                    Log.e(TAG, "taskId == -1")
                    return@withContext Result.failure()
                }

                // 获取所有点位
                val positions = positionRepository.getAllPositions()
                if (positions.isEmpty()) {
                    Log.e(TAG, "positions.isEmpty()")
                    return@withContext Result.failure()
                }

                // 按照sequence排序
                val sortedPositions = positions.sortedBy { it.sequence }

                // 执行巡逻任务
                executePatrol(sortedPositions)
                // 返回成功，可以带参数
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Get Exception: ${e.message}")
                // 返回失败，可以带参数
                Result.failure()
            }
        }
    }

    private suspend fun executePatrol(positions: List<Position>) {
            positions.forEach { task ->
                Log.d(TAG, "executePatrol: Position is $task")
            }
        // 发送加载地图指令
//        webSocketService.sendLoadMapCommand()
//        delay(5000) // 等待5秒让地图加载
//
//        // 发送设置起始位置指令
//        if (positions.isNotEmpty()) {
//            val homePosition = positions[0]
//            webSocketService.sendSetInitialPoseCommand(homePosition.x, homePosition.y, homePosition.z, homePosition.yaw)
//            delay(5000) // 等待5秒让机器人定位
//        }
//
//        // 依次移动到各个点位
//        for (position in positions) {
//            webSocketService.sendMoveToPositionCommand(position)
//            delay(15000) // 等待15秒让机器人移动到目标位置
//        }
//
//        // 回到起始位置
//        if (positions.isNotEmpty()) {
//            val homePosition = positions[0]
//            webSocketService.sendMoveToPositionCommand(homePosition)
//            delay(15000) // 等待15秒让机器人回到起始位置
//        }
    }

//    // DaggerWorkerFactory.ChildWorkerFactory
//    class PatrolWorkerFactory @Inject constructor(
//        private val positionRepository: PositionRepository,
//        private val webSocketService: Ros2WebSocketService
//    ) : WorkerFactory() {
////        override fun create(context: Context, params: WorkerParameters): ListenableWorker {
////            return PatrolWorker(context, params, positionRepository, webSocketService)
////        }
//        override fun createWorker(
//            appContext: Context,
//            workerClassName: String,
//            workerParameters: WorkerParameters
//        ): ListenableWorker {
//            return PatrolWorker(appContext, workerParameters, positionRepository, webSocketService)
//        }
//    }

//    // 设置巡逻任务
//    fun schedulePatrolTask(hour: Int, minute: Int, second: Int, taskId: Int) {
//        val now = Calendar.getInstance()
//        val targetTime = Calendar.getInstance().apply {
//            set(Calendar.HOUR_OF_DAY, hour)
//            set(Calendar.MINUTE, minute)
//            set(Calendar.SECOND, second)
//        }
//
//        // 如果目标时间已经过去，则设置为明天
//        if (targetTime.before(now)) {
//            targetTime.add(Calendar.DAY_OF_YEAR, 1)
//        }
//
//        val delayMillis = targetTime.timeInMillis - now.timeInMillis
//        // 创建workRequest
//        val workRequest = OneTimeWorkRequestBuilder<PatrolWorker>()
//            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
//            .setInputData(workDataOf(EXTRA_TASK_ID to taskId))
//            .addTag(WORK_NAME)
//            .build()
//        // 获取WorkManager单例，把workRequest推到队列里
//        WorkManager.getInstance(applicationContext).enqueue(workRequest)
//    }
}    