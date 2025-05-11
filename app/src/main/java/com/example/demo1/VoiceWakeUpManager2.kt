import ai.picovoice.porcupine.*
import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * 使用picovoice实现语音唤醒
 */
class VoiceWakeUpManager2(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val lifecycleScope: LifecycleCoroutineScope = lifecycleOwner.lifecycleScope
) {
    private val TAG = "VoiceWakeUpManager2"
    private var porcupine: Porcupine? = null
    private var audioRecord: AudioRecord? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isRecording = false

    // 唤醒词模型路径（从assets复制到应用私有目录后的路径）
    private var keywordModelPath: String? = null

    // 唤醒成功回调
    var onWakeUpListener: (() -> Unit)? = null

    // 初始化Porcupine引擎
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun init(accessKey: String, keywordModelAssetPath: String) {
        try {
            // 将assets中的模型文件复制到应用私有目录
            keywordModelPath = copyAssetToFilesDir(keywordModelAssetPath)

            // 创建Porcupine实例
            porcupine = Porcupine.Builder()
                .setAccessKey(accessKey)
                .setKeywordPath(keywordModelPath!!)
                .setSensitivity(0.7f) // 灵敏度（0.0-1.0，越高越敏感）
                .build(context)

            // 启动音频录制和监听
            startListening()
        } catch (e: Exception) {
            e.printStackTrace()
            // 处理初始化失败
        }
    }

    // 启动音频监听
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startListening() {
        try {
            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(porcupine!!.sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .build()

            val bufferSize = AudioRecord.getMinBufferSize(
                porcupine!!.sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioRecord = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .build()

            isRecording = true

            // 在协程中进行音频录制和处理
            lifecycleScope.launch(Dispatchers.IO) {
                audioRecord?.startRecording()

                val buffer = ShortArray(porcupine!!.frameLength)

                while (isRecording) {
                    val readSize = audioRecord?.read(buffer, 0, porcupine!!.frameLength) ?: -1
                    if (readSize == porcupine!!.frameLength) {
                        processAudioFrame(buffer)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 处理启动失败
        }
        Log.d(TAG, "开始监听语音")
    }

    fun stopListening() {
        isRecording = false
        audioRecord?.stop()
        Log.d(TAG, "结束监听语音")
    }

    // 处理音频帧
    private fun processAudioFrame(frame: ShortArray) {
        try {
            porcupine?.let { p ->
                val keywordIndex = p.process(frame)
                if (keywordIndex >= 0) {
                    // 唤醒词检测成功，切换到主线程回调
                    mainHandler.post {
                        onWakeUpListener?.invoke()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 释放资源
    fun release() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        porcupine?.delete()
        porcupine = null
    }

    // 从assets复制文件到应用私有目录
    private fun copyAssetToFilesDir(assetPath: String): String {
        val fileName = File(assetPath).name
        val outFile = File(context.filesDir, fileName)

        if (!outFile.exists()) {
            context.assets.open(assetPath).use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        return outFile.absolutePath
    }
}