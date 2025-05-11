package com.example.demo1.data.dao

import androidx.room.*
import com.example.demo1.data.entity.Position
import kotlinx.coroutines.flow.Flow

@Dao
interface PositionDao {
    // 使用Flow进行响应式查询（非挂起函数）
    @Query("SELECT * FROM positions ORDER BY sequence ASC")
    fun getAll(): Flow<List<Position>>

    // 使用挂起函数进行一次性查询
    @Query("SELECT * FROM positions ORDER BY sequence ASC")
    suspend fun getAllSuspend(): List<Position>

    @Query("SELECT * FROM positions WHERE id = :id")
    suspend fun getById(id: Int): Position?

    @Insert
    suspend fun insert(position: Position): Long

    @Update
    suspend fun update(position: Position)

    @Delete
    suspend fun delete(position: Position)

    @Query("DELETE FROM positions")
    suspend fun deleteAll()

    @Query("SELECT MAX(sequence) FROM positions")
    suspend fun getMaxSequence(): Int?
}    