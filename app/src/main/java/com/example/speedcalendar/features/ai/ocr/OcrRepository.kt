package com.example.speedcalendar.features.ai.ocr

import android.content.Context
import android.graphics.Bitmap
import com.example.speedcalendar.ocr.PaddleOCRManager
import com.example.speedcalendar.ocr.OcrResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class OcrRepository(context: Context) {
    private val engine = PaddleOCRManager.getInstance(context.applicationContext)
    private val initMutex = Mutex()

    suspend fun ensureInitialized(): Boolean = initMutex.withLock {
        engine.init()
    }

    suspend fun recognize(bitmap: Bitmap): OcrResult {
        ensureInitialized()
        return engine.recognize(bitmap)
    }

    fun release() = engine.release()
}
