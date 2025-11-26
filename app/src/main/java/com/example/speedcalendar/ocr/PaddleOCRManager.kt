package com.example.speedcalendar.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.equationl.paddleocr4android.OCR
import com.equationl.paddleocr4android.OcrConfig
import com.equationl.paddleocr4android.callback.OcrRunCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * PaddleOCR 管理类（使用 paddleocr4android）
 */
class PaddleOCRManager private constructor(private val context: Context) {

    private var ocr: OCR? = null
    private var isInitialized = false

    companion object {
        private const val TAG = "PaddleOCR"

        @Volatile
        private var instance: PaddleOCRManager? = null

        fun getInstance(context: Context): PaddleOCRManager {
            return instance ?: synchronized(this) {
                instance ?: PaddleOCRManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * 初始化 OCR 引擎
     */
    suspend fun init(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized) {
            Log.d(TAG, "OCR 已经初始化")
            return@withContext true
        }

        try {
            // 复制模型文件到缓存目录（每次都重新复制确保是最新的）
            val modelDir = File(context.cacheDir, "ocr_models")
            // 删除旧缓存，确保使用最新模型
            if (modelDir.exists()) {
                modelDir.deleteRecursively()
            }
            Log.d(TAG, "开始复制模型文件到缓存目录...")
            copyModelsToCache(modelDir)
            Log.d(TAG, "模型文件复制完成")

            // 配置 OCR - 使用绝对路径（以 / 开头表示外部路径）
            val config = OcrConfig()
            config.modelPath = "/" + modelDir.absolutePath  // 添加 / 前缀表示使用外部路径
            config.clsModelFilename = "cls.nb"
            config.detModelFilename = "det.nb"
            config.recModelFilename = "rec.nb"
            config.labelPath = "ppocr_keys_v1.txt"  // 相对于 modelPath 的路径
            config.cpuThreadNum = 4
            config.isRunDet = true
            config.isRunCls = true
            config.isRunRec = true

            // 初始化 OCR
            ocr = OCR(context)
            val result = ocr?.initModelSync(config)
            isInitialized = result?.getOrNull() == true

            Log.d(TAG, "OCR 初始化${if (isInitialized) "成功" else "失败"}")
            isInitialized

        } catch (e: Exception) {
            Log.e(TAG, "OCR 初始化异常", e)
            false
        }
    }

    /**
     * 执行 OCR 识别（协程版本）
     */
    suspend fun recognize(bitmap: Bitmap): OcrResult = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            return@withContext OcrResult(
                success = false,
                text = "",
                error = "OCR 引擎未初始化"
            )
        }

        try {
            val result = ocr?.runSync(bitmap)
            if (result?.isSuccess == true) {
                val data = result.getOrNull()!!
                val resultText = data.simpleText
                Log.d(TAG, "识别成功: $resultText, 耗时: ${data.inferenceTime}ms")
                return@withContext OcrResult(
                    success = true,
                    text = resultText,
                    inferenceTime = data.inferenceTime.toLong()
                )
            } else {
                val error = result?.exceptionOrNull()
                Log.e(TAG, "识别失败", error)
                return@withContext OcrResult(
                    success = false,
                    text = "",
                    error = "识别失败: ${error?.message}"
                )
            }
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
     * 复制 assets 中的模型文件到缓存目录
     */
    private fun copyModelsToCache(targetDir: File) {
        targetDir.mkdirs()

        val files = mapOf(
            "models/det.nb" to "det.nb",
            "models/rec.nb" to "rec.nb",
            "models/cls.nb" to "cls.nb",
            "ppocr_keys_v1.txt" to "ppocr_keys_v1.txt"
        )

        files.forEach { (assetPath, targetPath) ->
            val targetFile = File(targetDir, targetPath)
            targetFile.parentFile?.mkdirs()

            try {
                context.assets.open(assetPath).use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "复制文件: $assetPath -> ${targetFile.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "复制文件失败: $assetPath", e)
                throw e
            }
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        ocr?.releaseModel()
        isInitialized = false
        Log.d(TAG, "OCR 资源已释放")
    }
}

/**
 * OCR 识别结果
 */
data class OcrResult(
    val success: Boolean,
    val text: String,
    val inferenceTime: Long = 0,
    val error: String? = null
)
