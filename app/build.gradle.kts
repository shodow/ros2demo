import java.util.Date
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("kapt") // 应用 KAPT 插件
    id("dagger.hilt.android.plugin") // 必须添加这一行
}

android {
    namespace = "com.example.demo1"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.demo1"
        minSdk = 25
        targetSdk = 35
        // 自动生成递增版本号（兼容Kotlin语法）
        versionCode = (Date().time / 1000).toInt()
        versionName = "1.0.${(Date().time / 1000) % 1000}" // 可选动态版本名

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.protolite.well.known.types)
    implementation(libs.androidx.room.common.jvm)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("javax.inject:javax.inject:1")
    implementation("androidx.preference:preference-ktx:1.2.1")
    // 确保包含 AndroidX 运行时
    implementation("androidx.hilt:hilt-work:1.0.0")
    // Hilt 依赖 - 使用最新稳定版本
    kapt("androidx.hilt:hilt-compiler:1.0.0")
    implementation("androidx.hilt:hilt-lifecycle-viewmodel:1.0.0-alpha03")
    implementation("com.google.dagger:hilt-android:2.48.1")
    kapt("com.google.dagger:hilt-android-compiler:2.48.1")
    // 添加 Hilt AndroidX 运行时
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")

    implementation("org.java-websocket:Java-WebSocket:1.5.3")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    // 添加网络请求和语音识别相关依赖
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("ai.picovoice:porcupine-android:3.0.0")
//    implementation("com.bytedance.speechengine:speechengine_asr_tob:2.3.0")
    // ros2
//    implementation("org.ros2.rcljava:rcljava:0.15.0")
//    implementation("org.ros2.rcljava:std_msgs:0.15.0")
 }