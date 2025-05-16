package com.example.demo1.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.demo1.databinding.ActivityAddPatrolTaskBinding
import com.example.demo1.data.entity.PatrolTask
import com.example.demo1.ui.viewmodel.PatrolTaskViewModel
import java.util.*

class AddPatrolTaskActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddPatrolTaskBinding
    private lateinit var taskViewModel: PatrolTaskViewModel
    private lateinit var editingTask: PatrolTask
    private var selectedHour: Int = 0
    private var selectedMinute: Int = 0
    private var selectedSecond: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPatrolTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        taskViewModel = ViewModelProvider(this).get(PatrolTaskViewModel::class.java)

        // 获取传递的任务数据（如果有）
        editingTask.id = intent.getSerializableExtra("task_id") as Int
        editingTask.isActive = intent.getSerializableExtra("task_isActive") as Boolean
        editingTask.name = intent.getSerializableExtra("task_name") as String
        editingTask.hour = intent.getSerializableExtra("task_hour") as Int
        editingTask.minute = intent.getSerializableExtra("task_minute") as Int
        editingTask.second = intent.getSerializableExtra("task_second") as Int
        editingTask.createdTime = intent.getSerializableExtra("task_createdTime") as Long

        setupViews()
    }

    private fun setupViews() {
        // 设置标题
        if (editingTask.id != -1) {
            title = "编辑巡逻任务"
            // 填充现有数据
            binding.etTaskName.setText(editingTask?.name)
            selectedHour = editingTask?.hour ?: 0
            selectedMinute = editingTask?.minute ?: 0
            selectedSecond = editingTask?.second ?: 0
            updateTimeDisplay()
        } else {
            title = "添加巡逻任务"
            // 设置当前时间为默认值
            val calendar = Calendar.getInstance()
            selectedHour = calendar.get(Calendar.HOUR_OF_DAY)
            selectedMinute = calendar.get(Calendar.MINUTE)
            updateTimeDisplay()
        }

        // 设置时间选择器点击事件
        binding.btnSelectTime.setOnClickListener { showTimePicker() }

        // 设置保存按钮点击事件
        binding.btnSaveTask.setOnClickListener { saveTask() }
    }

    private fun showTimePicker() {
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedHour = hourOfDay
                selectedMinute = minute
                selectedSecond = 0 // 默认秒为0
                updateTimeDisplay()
            },
            selectedHour,
            selectedMinute,
            true // 24小时制
        )
        timePickerDialog.show()
    }

    private fun updateTimeDisplay() {
        binding.tvSelectedTime.text = String.format("%02d:%02d:%02d", selectedHour, selectedMinute, selectedSecond)
    }

    private fun saveTask() {
        val name = binding.etTaskName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入任务名称", Toast.LENGTH_SHORT).show()
            return
        }

        if (editingTask.id != -1) {
            // 更新现有任务
            val updatedTask = editingTask!!.copy(
                name = name,
                hour = selectedHour,
                minute = selectedMinute,
                second = selectedSecond
            )
            taskViewModel.update(updatedTask)
        } else {
            // 创建新任务
            val newTask = PatrolTask(
                name = name,
                hour = selectedHour,
                minute = selectedMinute,
                second = selectedSecond
            )
            taskViewModel.insert(newTask)
        }

        Toast.makeText(this, "任务已保存", Toast.LENGTH_SHORT).show()
        finish()
    }
}    