package com.example.demo1.ui.viewmodel

import androidx.lifecycle.*
import com.example.demo1.data.entity.PatrolTask
import com.example.demo1.data.repository.PatrolTaskRepository
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel // 使用这个注解替代ViewModelInject
class PatrolTaskViewModel @Inject constructor(
    private val repository: PatrolTaskRepository
) : ViewModel() {
    // 使用Flow方式（推荐）
    val allTasks: LiveData<List<PatrolTask>> = repository.allTasks.asLiveData()

    // 使用挂起函数方式（需要在协程中调用）
    private val _tasks = MutableLiveData<List<PatrolTask>>()
    val tasks: LiveData<List<PatrolTask>> = _tasks

    fun loadAllTasks() = viewModelScope.launch {
        val tasks = repository.getAllTasks()
        _tasks.value = tasks
    }

    fun insert(task: PatrolTask) = viewModelScope.launch {
        repository.insert(task)
    }

    fun update(task: PatrolTask) = viewModelScope.launch {
        repository.update(task)
    }

    fun delete(task: PatrolTask) = viewModelScope.launch {
        repository.delete(task)
    }

    fun deleteAll() = viewModelScope.launch {
        repository.deleteAll()
    }

    suspend fun getById(id: Int): PatrolTask? {
        return repository.getById(id)
    }

    suspend fun getActiveTasks(): LiveData<List<PatrolTask>> {
        return repository.getActiveTasks()
    }
}