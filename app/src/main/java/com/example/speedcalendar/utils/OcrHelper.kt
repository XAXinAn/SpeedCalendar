package com.example.speedcalendar.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.speedcalendar.ocr.OcrResult
import com.example.speedcalendar.ocr.PaddleOCRManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * OCR 工具类 - 封装 OCR 功能供 AI 聊天和悬浮窗复用
 * 
 * 功能：
 * 1. OCR 引擎初始化
 * 2. 图片识别
 * 3. 结果格式化（添加"帮我添加日程："前缀）
 */
class OcrHelper private constructor(private val context: Context) {

    private val engine = PaddleOCRManager.getInstance(context.applicationContext)
    private val initMutex = Mutex()
    
    private var isInitialized = false
    private var initError: String? = null

    companion object {
        private const val TAG = "OcrHelper"
        private const val SCHEDULE_PREFIX = "帮我添加日程："

        @Volatile
        private var instance: OcrHelper? = null

        /**
         * 获取 OcrHelper 单例
         */
        fun getInstance(context: Context): OcrHelper {
            return instance ?: synchronized(this) {
                instance ?: OcrHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * 初始化 OCR 引擎
     * @return true 表示初始化成功
     */
    suspend fun initialize(): Boolean = initMutex.withLock {
        if (isInitialized) {
            Log.d(TAG, "OCR 已经初始化")
            return true
        }

        try {
            Log.d(TAG, "开始初始化 OCR 引擎...")
            val result = engine.init()
            isInitialized = result
            initError = if (result) null else "OCR 引擎初始化失败"
            Log.d(TAG, "OCR 初始化${if (result) "成功" else "失败"}")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "OCR 初始化异常", e)
            initError = e.message
            return false
        }
    }

    /**
     * 检查 OCR 是否已初始化
     */
    fun isReady(): Boolean = isInitialized

    /**
     * 获取初始化错误信息
     */
    fun getInitError(): String? = initError

    /**
     * 从 Uri 加载 Bitmap
     */
    suspend fun loadBitmapFromUri(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载图片失败", e)
            null
        }
    }

    /**
     * 执行 OCR 识别
     * @param bitmap 要识别的图片
     * @return OCR 识别结果
     */
    suspend fun recognize(bitmap: Bitmap): OcrResult {
        if (!isInitialized) {
            val initResult = initialize()
            if (!initResult) {
                return OcrResult(
                    success = false,
                    text = "",
                    error = initError ?: "OCR 引擎未初始化"
                )
            }
        }

        return try {
            engine.recognize(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "OCR 识别异常", e)
            OcrResult(
                success = false,
                text = "",
                error = e.message
            )
        }
    }

    /**
     * 执行 OCR 识别并格式化为日程添加文本
     * @param bitmap 要识别的图片
     * @return 带有"帮我添加日程："前缀的识别文本，失败返回 null
     */
    suspend fun recognizeForSchedule(bitmap: Bitmap): OcrForScheduleResult {
        val result = recognize(bitmap)
        return if (result.success && result.text.isNotBlank()) {
            OcrForScheduleResult(
                success = true,
                formattedText = "$SCHEDULE_PREFIX${result.text}",
                rawText = result.text,
                inferenceTime = result.inferenceTime
            )
        } else {
            OcrForScheduleResult(
                success = false,
                formattedText = "",
                rawText = "",
                error = result.error ?: "识别结果为空"
            )
        }
    }

    /**
     * 从 Uri 加载图片并执行 OCR 识别（带日程前缀）
     * @param uri 图片 Uri
     * @return 带有"帮我添加日程："前缀的识别文本结果
     */
    suspend fun recognizeFromUri(uri: Uri): OcrForScheduleResult {
        val bitmap = loadBitmapFromUri(uri)
            ?: return OcrForScheduleResult(
                success = false,
                formattedText = "",
                rawText = "",
                error = "无法加载图片"
            )

        return try {
            recognizeForSchedule(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * 释放 OCR 资源
     */
    fun release() {
        engine.release()
        isInitialized = false
        Log.d(TAG, "OCR 资源已释放")
    }
}

/**
 * OCR 日程识别结果
 */
data class OcrForScheduleResult(
    val success: Boolean,
    val formattedText: String,  // 带有"帮我添加日程："前缀的文本
    val rawText: String,        // 原始识别文本
    val inferenceTime: Long = 0,
    val error: String? = null
)
