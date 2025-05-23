package com.example.demo1.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.set
import androidx.lifecycle.ViewModelProvider
import com.example.demo1.R
import com.example.demo1.databinding.ActivityAddPositionBinding
import com.example.demo1.data.entity.Position
import com.example.demo1.service.Ros2WebSocketService
import com.example.demo1.ui.viewmodel.PositionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddPositionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddPositionBinding
    private lateinit var positionViewModel: PositionViewModel
    private var editingPosition: Position? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPositionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        positionViewModel = ViewModelProvider(this).get(PositionViewModel::class.java)

        // 获取传递的点位数据（如果有）
        editingPosition = intent.getSerializableExtra("position") as? Position

        setupViews()
    }

    private fun setupViews() {
        // 设置标题
        if (editingPosition != null) {
            title = "编辑点位"
            // 填充现有数据
            binding.etPositionName.setText(editingPosition?.name)
            binding.etX.setText(editingPosition?.x?.toString())
            binding.etY.setText(editingPosition?.y?.toString())
            binding.etZ.setText(editingPosition?.z?.toString())
            binding.etYaw.setText(editingPosition?.yaw?.toString())
            binding.etSequence.setText(editingPosition?.sequence?.toString())
        } else {
            title = "添加点位"
            // 设置默认序列
            CoroutineScope(Dispatchers.IO).launch {
                val maxSequence = positionViewModel.getMaxSequence()
                val nextSequence = (maxSequence ?: 0) + 1
                runOnUiThread {
                    binding.etSequence.setText(nextSequence.toString())
                }
            }
        }

        // 设置保存按钮点击事件
        binding.btnSavePosition.setOnClickListener { savePosition() }

        binding.btnGetPosition.setOnClickListener { getPosition() }
    }

    private fun savePosition() {
        val name = binding.etPositionName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入点位名称", Toast.LENGTH_SHORT).show()
            return
        }

        val x = binding.etX.text.toString().trim().toDoubleOrNull() ?: 0.0
        val y = binding.etY.text.toString().trim().toDoubleOrNull() ?: 0.0
        val z = binding.etZ.text.toString().trim().toDoubleOrNull() ?: 0.0
        val yaw = binding.etYaw.text.toString().trim().toDoubleOrNull() ?: 0.0
        val sequence = binding.etSequence.text.toString().trim().toIntOrNull() ?: 0

        if (editingPosition != null) {
            // 更新现有点位
            val updatedPosition = editingPosition!!.copy(
                name = name,
                x = x,
                y = y,
                z = z,
                yaw = yaw,
                yaw_z = Math.sin(yaw/2),
                yaw_w = Math.cos(yaw/2),
                sequence = sequence
            )
            positionViewModel.update(updatedPosition)
        } else {
            // 创建新点位
            val newPosition = Position(
                name = name,
                x = x,
                y = y,
                z = z,
                yaw = yaw,
                yaw_z = Math.sin(yaw/2),
                yaw_w = Math.cos(yaw/2),
                sequence = sequence
            )
            positionViewModel.insert(newPosition)
        }

        Toast.makeText(this, "点位已保存", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun getPosition() {
        val point = Ros2WebSocketService.getInstance()?.getPosion()
        if (point != null) {
            binding.etX.setText(point.x.toString())
            binding.etY.setText(point.y.toString())
            binding.etZ.setText(point.z.toString())
            binding.etYaw.setText(point.yaw.toString())
        }
    }
}    