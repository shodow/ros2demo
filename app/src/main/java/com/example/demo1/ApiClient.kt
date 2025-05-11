package com.example.demo1

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.Response

// 数据模型类
data class ChatRequest(
    val model: String = "doubao-pro",  // 使用的AI模型（默认：豆包专业版）
    val messages: List<Message>        // 对话消息列表
)

/**
 * 作用：表示对话中的一条消息。
 * 字段说明：
 * role：消息发送者的角色，常见值有：
 * "user"：用户发送的消息。
 * "assistant"：AI 回复的消息。
 * "system"：系统指令（用于设置 AI 的行为）。
 * content：消息的具体文本内容。
 */
data class Message(
    val role: String,      // 消息角色（如 "user", "assistant", "system"）
    val content: String    // 消息内容
)

data class ChatResponse(
    val choices: List<Choice>  // AI 生成的回复列表（通常包含一条回复，但可能有多条候选）。
)

data class Choice(
    val message: Message  // AI生成的消息，包含 role 和 content 的消息对象，通常 role 为 "assistant"。
)

// API服务接口
interface DoubaoApi {
    @POST("chat/completions")
    @Headers("Content-Type: application/json")
    suspend fun sendMessage(@Body request: ChatRequest): Response<ChatResponse>
}

// API客户端初始化，创建 Retrofit 服务接口来调用豆包 API
object ApiClient {
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.doubao.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(
                OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .build())
            .build()
    }

    val doubaoService: DoubaoApi by lazy {
        retrofit.create(DoubaoApi::class.java)
    }
}