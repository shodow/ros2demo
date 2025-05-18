package com.example.demo1.ui

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.demo1.R
import com.example.demo1.databinding.ActivityPatrolTaskBinding
import com.example.demo1.data.entity.PatrolTask
import com.example.demo1.ui.adapter.PatrolTaskAdapter
import com.example.demo1.ui.viewmodel.PatrolTaskViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class PatrolTaskActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPatrolTaskBinding
    private val taskViewModel: PatrolTaskViewModel by viewModels()
    private lateinit var adapter: PatrolTaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatrolTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerView() {
        adapter = PatrolTaskAdapter(
            { task ->
                // 点击任务，打开编辑页面
                val intent = Intent(this, AddPatrolTaskActivity::class.java)
                intent.putExtra("task", task)
                startActivity(intent)
            },
            { task, isActive ->
                // 切换任务状态
                val updatedTask = task.copy(isActive = isActive)
                taskViewModel.update(updatedTask)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupObservers() {
        taskViewModel.allTasks.observe(this, Observer { tasks ->
            tasks?.let { adapter.setTasks(it) }
        })
    }

    private fun setupListeners() {
        binding.fabAddTask.setOnClickListener {
            // 打开添加任务页面
            val intent = Intent(this, AddPatrolTaskActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_patrol_task, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete_all -> {
                taskViewModel.deleteAll()
                Toast.makeText(this, "已删除所有任务", Toast.LENGTH_SHORT).show()
                true
            }
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}    