package com.example.demo1.data.database

import android.content.Context
import androidx.room.Room
import com.example.demo1.data.dao.PatrolTaskDao
import com.example.demo1.data.dao.PositionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // 确保安装到全局单例组件
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ros2_robot_control_db" // 与AppDatabase的数据库名称一致
        ).build()
    }

    // 提供 PatrolTaskDao
    @Provides
    fun providePatrolTaskDao(appDatabase: AppDatabase): PatrolTaskDao {
        return appDatabase.patrolTaskDao()
    }

    // 提供 PositionDao
    @Provides
    fun providePositionDao(appDatabase: AppDatabase): PositionDao {
        return appDatabase.positionDao()
    }
}