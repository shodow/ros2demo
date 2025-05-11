package com.example.demo1.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "patrol_tasks")
data class PatrolTask(
    @PrimaryKey(autoGenerate = true) var id: Int = -1,
    var name: String,
    var hour: Int,
    var minute: Int,
    var second: Int,
    var isActive: Boolean = true,
    var createdTime: Long = System.currentTimeMillis()
)    