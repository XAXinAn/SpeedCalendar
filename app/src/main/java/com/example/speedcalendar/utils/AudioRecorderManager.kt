package com.example.speedcalendar.utils

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 音频录制管理器
 * 用于实时录制音频并回调数据给语音识别 SDK
 */
class AudioRecorderManager(
    private val sampleRateInHz: Int = 16000,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    private val channels: Int = AudioFormat.CHANNEL_IN_MONO
) {
    companion object {
        private const val TAG = "AudioRecorderManager"
        
        @Volatile
        private var instance: AudioRecorderManager? = null
        
        fun getInstance(): AudioRecorderManager {
            return instance ?: synchronized(this) {
                instance ?: AudioRecorderManager().also { instance = it }
            }
        }
    }
    
    private val bufferSize: Int = AudioRecord.getMinBufferSize(sampleRateInHz, channels, audioFormat)
    private var recorder: AudioRecord? = null
    private val isRecording = AtomicBoolean(false)
    private var recordThread: Thread? = null
    private var callback: AudioDataCallback? = null
    
    /**
     * 音频数据回调接口
     */
    interface AudioDataCallback {
        fun onAudioData(data: ByteArray, size: Int)
        fun onAudioVolume(db: Double, volume: Int)
    }
    
    /**
     * 注册回调
     */
    fun registerCallback(callback: AudioDataCallback) {
        this.callback = callback
    }
    
    /**
     * 开始录音
     */
    fun startRecord() {
        if (isRecording.get()) {
            Log.w(TAG, "录音已在进行中")
            return
        }
        
        try {
            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRateInHz,
                channels,
                audioFormat,
                bufferSize
            )
            
            if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord 初始化失败")
                stopRecord()
                return
            }
            
            isRecording.set(true)
            recordThread = Thread(recordRunnable).apply { start() }
            Log.d(TAG, "录音已开始")
            
        } catch (e: Exception) {
            Log.e(TAG, "启动录音失败: ${e.message}")
            stopRecord()
        }
    }
    
    /**
     * 停止录音
     */
    fun stopRecord() {
        isRecording.set(false)
        
        try {
            recordThread?.let {
                if (it.isAlive) {
                    it.interrupt()
                    it.join(500)
                }
            }
            recordThread = null
            
            recorder?.let {
                if (it.state == AudioRecord.STATE_INITIALIZED) {
                    it.stop()
                }
                it.release()
            }
            recorder = null
            
            Log.d(TAG, "录音已停止")
            
        } catch (e: Exception) {
            Log.e(TAG, "停止录音异常: ${e.message}")
        } finally {
            instance = null
        }
    }
    
    /**
     * 录音线程
     */
    private val recordRunnable = Runnable {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
        
        try {
            val tempBuffer = ByteArray(bufferSize)
            recorder?.startRecording()
            
            while (isRecording.get()) {
                val bytesRead = recorder?.read(tempBuffer, 0, bufferSize) ?: -1
                
                if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION || 
                    bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                    continue
                }
                
                if (bytesRead > 0 && isRecording.get()) {
                    // 计算音量
                    val (db, volume) = calculateVolume(tempBuffer, bytesRead)
                    
                    // 回调音频数据
                    callback?.onAudioData(tempBuffer.copyOf(bytesRead), bytesRead)
                    callback?.onAudioVolume(db, volume)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "录音线程异常: ${e.message}")
        }
    }
    
    /**
     * 计算音量（RMS 方法）
     */
    private fun calculateVolume(buffer: ByteArray, size: Int): Pair<Double, Int> {
        var sumSquares = 0.0
        val sampleCount = size / 2  // 每个样本16位(2字节)
        
        for (i in 0 until size step 2) {
            // 将两个字节转换为一个16位短整型
            val sample = ((buffer[i].toInt() and 0xFF) or
                    ((buffer[i + 1].toInt() and 0xFF) shl 8)).toShort()
            sumSquares += sample.toDouble() * sample.toDouble()
        }
        
        // 计算RMS (均方根)
        val rms = sqrt(sumSquares / sampleCount)
        
        // 转换为分贝值
        var db = -120.0
        if (rms > 1e-10) {
            db = 20 * log10(rms / 32767.0)
        }
        
        // 映射到0-9音量等级
        var volume = 0
        if (db > -60) {
            volume = min(9, max(0, ((db + 60) * 9 / 40.0).toInt()))
        }
        
        return Pair(db, volume)
    }
}
