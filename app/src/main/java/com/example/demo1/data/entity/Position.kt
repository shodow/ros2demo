package com.example.demo1.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "positions")
data class Position(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Double,
    val yaw_z: Double,
    val yaw_w: Double,
    val sequence: Int,
    val createdTime: Long = System.currentTimeMillis()
) : Serializable    