package com.example.demo1.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.demo1.data.entity.PatrolTask
import kotlinx.coroutines.flow.Flow

@Dao
interface PatrolTaskDao {
    // 使用Flow进行响应式查询（非挂起函数）
    @Query("SELECT * FROM patrol_tasks ORDER BY createdTime DESC")
    fun getAll(): Flow<List<PatrolTask>>

    // 使用挂起函数进行一次性查询
    @Query("SELECT * FROM patrol_tasks ORDER BY createdTime DESC")
    suspend fun getAllSuspend(): List<PatrolTask>

    @Query("SELECT * FROM patrol_tasks WHERE id = :id")
    suspend fun getById(id: Int): PatrolTask?

    @Insert
    suspend fun insert(task: PatrolTask): Long

    @Update
    suspend fun update(task: PatrolTask)

    @Delete
    suspend fun delete(task: PatrolTask)

    @Query("DELETE FROM patrol_tasks")
    suspend fun deleteAll()

    @Query("SELECT * FROM patrol_tasks WHERE isActive = 1")
    fun getActiveTasks(): LiveData<List<PatrolTask>>
}    