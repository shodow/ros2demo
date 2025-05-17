package com.example.demo1.data.repository

import com.example.demo1.data.dao.PositionDao
import com.example.demo1.data.entity.Position
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PositionRepository @Inject constructor(
    private val positionDao: PositionDao
) {
    // 使用Flow进行响应式查询
    val allPositions: Flow<List<Position>> = positionDao.getAll()

    // 使用挂起函数进行一次性查询
    suspend fun getAllPositions(): List<Position> {
        return positionDao.getAllSuspend()
    }

    suspend fun insert(position: Position): Long {
        return positionDao.insert(position)
    }

    suspend fun update(position: Position) {
        positionDao.update(position)
    }

    suspend fun delete(position: Position) {
        positionDao.delete(position)
    }

    suspend fun deleteAll() {
        positionDao.deleteAll()
    }

    suspend fun getMaxSequence(): Int? {
        return positionDao.getMaxSequence()
    }

    suspend fun getById(id: Int): Position? {
        return positionDao.getById(id)
    }
}    