package com.example.demo1.data.repository

import androidx.lifecycle.LiveData
import com.example.demo1.data.dao.PatrolTaskDao
import com.example.demo1.data.entity.PatrolTask
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PatrolTaskRepository @Inject constructor(
    private val patrolTaskDao: PatrolTaskDao
) {
    // 使用Flow进行响应式查询
    val allTasks: Flow<List<PatrolTask>> = patrolTaskDao.getAll()

    // 使用挂起函数进行一次性查询
    suspend fun getAllTasks(): List<PatrolTask> {
        return patrolTaskDao.getAllSuspend()
    }

    suspend fun insert(task: PatrolTask): Long {
        return patrolTaskDao.insert(task)
    }

    suspend fun update(task: PatrolTask) {
        patrolTaskDao.update(task)
    }

    suspend fun delete(task: PatrolTask) {
        patrolTaskDao.delete(task)
    }

    suspend fun deleteAll() {
        patrolTaskDao.deleteAll()
    }

    suspend fun getById(id: Int): PatrolTask? {
        return patrolTaskDao.getById(id)
    }

    suspend fun getActiveTasks(): LiveData<List<PatrolTask>> {
        return patrolTaskDao.getActiveTasks()
    }
}