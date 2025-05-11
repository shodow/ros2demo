package com.example.demo1.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.demo1.data.dao.PositionDao
import com.example.demo1.data.dao.PatrolTaskDao
import com.example.demo1.data.entity.Position
import com.example.demo1.data.entity.PatrolTask

@Database(entities = [Position::class, PatrolTask::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun positionDao(): PositionDao
    abstract fun patrolTaskDao(): PatrolTaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ros2_robot_control_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}    