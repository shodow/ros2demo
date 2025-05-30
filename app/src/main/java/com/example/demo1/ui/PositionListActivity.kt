package com.example.demo1.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.demo1.R
import com.example.demo1.databinding.ActivityPositionListBinding
import com.example.demo1.ui.adapter.PositionAdapter
import com.example.demo1.ui.viewmodel.PositionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PositionListActivity : AppCompatActivity() {
    private var TAG = "PositionListActivity"
    private lateinit var binding: ActivityPositionListBinding
    private val positionViewModel: PositionViewModel by viewModels()
    private lateinit var adapter: PositionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPositionListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerView() {
        adapter = PositionAdapter { position ->
            // 点击点位，打开编辑页面
            val intent = Intent(this, AddPositionActivity::class.java)
            intent.putExtra("position", position)
            startActivity(intent)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupObservers() {
        positionViewModel.allPositions.observe(this, Observer { positions ->
            positions?.let {
                adapter.setPositions(it)
                Log.d(TAG, "it = $it")
            }
        })
    }

    private fun setupListeners() {
        binding.fabAddPosition.setOnClickListener {
            // 打开添加点位页面
            val intent = Intent(this, AddPositionActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_position_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete_all -> {
                positionViewModel.deleteAll()
                Toast.makeText(this, "已删除所有点位", Toast.LENGTH_SHORT).show()
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