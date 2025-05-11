package com.example.demo1.ui.viewmodel

import androidx.lifecycle.*
import com.example.demo1.data.entity.Position
import com.example.demo1.data.repository.PositionRepository
import kotlinx.coroutines.launch

class PositionViewModel(private val repository: PositionRepository) : ViewModel() {
    // 使用Flow方式（推荐）
    val allPositions: LiveData<List<Position>> = repository.allPositions.asLiveData()

    // 使用挂起函数方式（需要在协程中调用）
    private val _positions = MutableLiveData<List<Position>>()
    val positions: LiveData<List<Position>> = _positions

    fun loadAllPositions() = viewModelScope.launch {
        val positions = repository.getAllPositions()
        _positions.value = positions
    }

    fun insert(position: Position) = viewModelScope.launch {
        repository.insert(position)
    }

    fun update(position: Position) = viewModelScope.launch {
        repository.update(position)
    }

    fun delete(position: Position) = viewModelScope.launch {
        repository.delete(position)
    }

    fun deleteAll() = viewModelScope.launch {
        repository.deleteAll()
    }

    suspend fun getMaxSequence(): Int? {
        return repository.getMaxSequence()
    }

    suspend fun getById(id: Int): Position? {
        return repository.getById(id)
    }
}    