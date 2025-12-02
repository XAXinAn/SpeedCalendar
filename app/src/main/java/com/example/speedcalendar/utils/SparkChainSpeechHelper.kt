package com.example.speedcalendar.utils

import android.content.Context
import android.util.Log
import com.iflytek.sparkchain.core.LogLvl
import com.iflytek.sparkchain.core.SparkChain
import com.iflytek.sparkchain.core.SparkChainConfig
import com.iflytek.sparkchain.core.asr.ASR
import com.iflytek.sparkchain.core.asr.AsrCallbacks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 讯飞 SparkChain 语音识别帮助类
 * 封装语音听写 (ASR) 功能
 */
class SparkChainSpeechHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "SparkChainSpeech"
        
        // 讯飞开放平台配置 - 请替换为你的真实配置
        // 从 https://console.xfyun.cn/services/iat 获取
        private const val APP_ID = "97b47e77"
        private const val API_KEY = "69c5d0f6d9068cc028fa30d1d3b7e8c2"
        private const val API_SECRET = "Mjk2YTQ4NDg1OWNhZTFlMjkxODliZTg4"
        
        @Volatile
        private var isSDKInitialized = false
    }
    
    // 语音识别状态
    sealed class RecognitionState {
        object Idle : RecognitionState()
        object Listening : RecognitionState()
        data class Result(val text: String, val isFinal: Boolean) : RecognitionState()
        data class Error(val code: Int, val message: String) : RecognitionState()
    }
    
    private val _state = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val state: StateFlow<RecognitionState> = _state.asStateFlow()
    
    private val _volume = MutableStateFlow(0)
    val volume: StateFlow<Int> = _volume.asStateFlow()
    
    private var asr: ASR? = null
    private var audioRecorderManager: AudioRecorderManager? = null
    private val isRecording = AtomicBoolean(false)
    private val sessionId = AtomicInteger(0)
    private var resultBuilder = StringBuilder()
    
    // ASR 回调
    private val asrCallbacks = object : AsrCallbacks {
        override fun onResult(result: ASR.ASRResult?, userData: Any?) {
            result?.let {
                val status = it.status  // 0: 第一块, 1: 中间结果, 2: 最后一块
                val text = it.bestMatchText ?: ""
                
                Log.d(TAG, "识别结果: status=$status, text=$text")
                
                when (status) {
                    0 -> {
                        // 第一块结果
                        resultBuilder.clear()
                        resultBuilder.append(text)
                        _state.value = RecognitionState.Result(text, false)
                    }
                    1 -> {
                        // 中间结果（动态修正）
                        resultBuilder.clear()
                        resultBuilder.append(text)
                        _state.value = RecognitionState.Result(text, false)
                    }
                    2 -> {
                        // 最后一块结果
                        resultBuilder.clear()
                        resultBuilder.append(text)
                        _state.value = RecognitionState.Result(text, true)
                        stopListening()
                    }
                }
            }
        }
        
        override fun onError(error: ASR.ASRError?, userData: Any?) {
            error?.let {
                Log.e(TAG, "识别错误: code=${it.code}, msg=${it.errMsg}")
                _state.value = RecognitionState.Error(it.code, it.errMsg ?: "识别失败")
            }
            stopListening()
        }
        
        override fun onBeginOfSpeech() {
            Log.d(TAG, "开始说话")
        }
        
        override fun onEndOfSpeech() {
            Log.d(TAG, "结束说话")
        }
    }
    
    // 音频数据回调
    private val audioCallback = object : AudioRecorderManager.AudioDataCallback {
        override fun onAudioData(data: ByteArray, size: Int) {
            if (isRecording.get()) {
                asr?.write(data)
            }
        }
        
        override fun onAudioVolume(db: Double, volume: Int) {
            _volume.value = volume
        }
    }
    
    /**
     * 初始化 SDK
     * @return 初始化是否成功
     */
    fun initSDK(): Boolean {
        if (isSDKInitialized) {
            Log.d(TAG, "SDK 已初始化")
            return true
        }
        
        if (APP_ID.isEmpty() || API_KEY.isEmpty() || API_SECRET.isEmpty()) {
            Log.e(TAG, "请先配置讯飞 APPID、API_KEY、API_SECRET")
            return false
        }
        
        try {
            val config = SparkChainConfig.builder()
                .appID(APP_ID)
                .apiKey(API_KEY)
                .apiSecret(API_SECRET)
                .logLevel(LogLvl.VERBOSE.value)
            
            val ret = SparkChain.getInst().init(context.applicationContext, config)
            
            if (ret == 0) {
                isSDKInitialized = true
                Log.d(TAG, "SDK 初始化成功")
                return true
            } else {
                Log.e(TAG, "SDK 初始化失败，错误码: $ret")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "SDK 初始化异常: ${e.message}")
            return false
        }
    }
    
    /**
     * 检查 SDK 是否可用
     */
    fun isAvailable(): Boolean {
        return APP_ID.isNotEmpty() && API_KEY.isNotEmpty() && API_SECRET.isNotEmpty()
    }
    
    /**
     * 开始语音识别
     * @param language 语言，默认中文 "zh_cn"
     */
    fun startListening(language: String = "zh_cn") {
        if (isRecording.get()) {
            Log.w(TAG, "正在录音中")
            return
        }
        
        if (!isSDKInitialized) {
            if (!initSDK()) {
                _state.value = RecognitionState.Error(-1, "SDK 未初始化，请检查配置")
                return
            }
        }
        
        try {
            // 初始化 ASR
            if (asr == null) {
                asr = ASR()
                asr?.registerCallbacks(asrCallbacks)
            }
            
            // 配置参数
            asr?.apply {
                language(language)           // 语种：zh_cn 中文, en_us 英文
                domain("iat")                // 应用领域：iat 日常用语
                accent("mandarin")           // 方言：mandarin 普通话
                vinfo(true)                  // 返回端点帧偏移值
                if (language == "zh_cn") {
                    dwa("wpgs")              // 动态修正（仅中文）
                }
            }
            
            // 开始识别
            val sid = sessionId.incrementAndGet().toString()
            val ret = asr?.start(sid) ?: -1
            
            if (ret == 0) {
                isRecording.set(true)
                resultBuilder.clear()
                _state.value = RecognitionState.Listening
                
                // 启动录音
                audioRecorderManager = AudioRecorderManager.getInstance()
                audioRecorderManager?.registerCallback(audioCallback)
                audioRecorderManager?.startRecord()
                
                Log.d(TAG, "语音识别已启动")
            } else {
                Log.e(TAG, "启动语音识别失败，错误码: $ret")
                _state.value = RecognitionState.Error(ret, "启动识别失败")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "启动语音识别异常: ${e.message}")
            _state.value = RecognitionState.Error(-1, e.message ?: "未知错误")
        }
    }
    
    /**
     * 停止语音识别
     */
    fun stopListening() {
        if (!isRecording.get()) {
            return
        }
        
        try {
            isRecording.set(false)
            
            // 停止录音
            audioRecorderManager?.stopRecord()
            audioRecorderManager = null
            
            // 停止识别
            asr?.stop(false)  // false: 等待最后一包结果
            
            _volume.value = 0
            
            // 如果当前不是结果或错误状态，设置为 Idle
            if (_state.value is RecognitionState.Listening) {
                _state.value = RecognitionState.Idle
            }
            
            Log.d(TAG, "语音识别已停止")
            
        } catch (e: Exception) {
            Log.e(TAG, "停止语音识别异常: ${e.message}")
        }
    }
    
    /**
     * 取消语音识别
     */
    fun cancel() {
        isRecording.set(false)
        
        try {
            audioRecorderManager?.stopRecord()
            audioRecorderManager = null
            
            asr?.stop(true)  // true: 立即结束
            
            _volume.value = 0
            _state.value = RecognitionState.Idle
            resultBuilder.clear()
            
            Log.d(TAG, "语音识别已取消")
            
        } catch (e: Exception) {
            Log.e(TAG, "取消语音识别异常: ${e.message}")
        }
    }
    
    /**
     * 重置状态
     */
    fun resetState() {
        _state.value = RecognitionState.Idle
    }
    
    /**
     * 释放资源
     */
    fun destroy() {
        cancel()
        asr = null
        
        try {
            if (isSDKInitialized) {
                SparkChain.getInst().unInit()
                isSDKInitialized = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "释放 SDK 异常: ${e.message}")
        }
    }
}
