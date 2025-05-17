pluginManagement {
    plugins {
        // 声明 Hilt 插件版本
//        id("dagger.hilt.android.plugin") version "2.48.1"
        // Kotlin 插件（如果未在其他地方声明）
        id("org.jetbrains.kotlin.android") version "1.8.22"
    }
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    // 禁止模块单独声明仓库（统一在此配置）
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 添加 ROS2 Maven 仓库（关键位置！）
        maven {
            url = uri("https://repo.ros2.org/maven/")
        }
    }
}

rootProject.name = "demo1"
include(":app")
 