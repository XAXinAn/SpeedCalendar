package com.example.speedcalendar.features.ai

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.speedcalendar.MainActivity
import com.example.speedcalendar.R
import com.example.speedcalendar.data.api.RetrofitClient
import com.example.speedcalendar.data.model.ChatMessageRequest
import com.example.speedcalendar.utils.CustomLifecycleOwner
import com.example.speedcalendar.utils.OcrHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingWindowService : Service() {

    companion object {
        private const val TAG = "FloatingWindowService"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "data"
        
        // 通知渠道
        private const val CHANNEL_ID = "floating_window_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: ComposeView
    private lateinit var lifecycleOwner: CustomLifecycleOwner
    private lateinit var ocrHelper: OcrHelper
    
    // MediaProjection 相关
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var resultCode: Int = Activity.RESULT_CANCELED
    private var resultData: Intent? = null
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    
    // UI 状态
    private var uiState by mutableStateOf(FloatingUIState.Collapsed)
    private var isProcessing by mutableStateOf(false)
    private var resultText by mutableStateOf("")
    private var errorText by mutableStateOf("")
    
    // 悬浮窗位置
    private var posX by mutableFloatStateOf(0f)
    private var posY by mutableFloatStateOf(100f)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            resultCode = it.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            resultData = it.getParcelableExtra(EXTRA_DATA)
            Log.d(TAG, "onStartCommand: resultCode=$resultCode, hasData=${resultData != null}")
        }
        
        // 启动前台服务（MediaProjection 需要）
        startForegroundService()
        
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleOwner = CustomLifecycleOwner()
        
        // 设置静态引用，供 ScreenCropActivity 回调
        ScreenCropActivity.setServiceInstance(this)
        
        // 初始化 OcrHelper
        ocrHelper = OcrHelper.getInstance(applicationContext)
        serviceScope.launch {
            val result = ocrHelper.initialize()
            Log.d(TAG, "OCR 引擎初始化: ${if (result) "成功" else "失败"}")
        }

        createFloatingWindow()
    }
    
    /**
     * 启动前台服务（MediaProjection 需要）
     */
    private fun startForegroundService() {
        // 创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "悬浮窗服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于悬浮窗截屏功能"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        
        // 创建通知
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("速记日历")
            .setContentText("悬浮窗正在运行")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        // 启动前台服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, 
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createFloatingWindow() {
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = posX.toInt()
            y = posY.toInt()
        }

        floatingView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                FloatingWindowContent(
                    state = uiState,
                    isProcessing = isProcessing,
                    resultText = resultText,
                    errorText = errorText,
                    onSingleTap = { handleSingleTap() },
                    onLongPress = { handleLongPress() },
                    onDrag = { dx, dy ->
                        layoutParams.x += dx.toInt()
                        layoutParams.y += dy.toInt()
                        posX = layoutParams.x.toFloat()
                        posY = layoutParams.y.toFloat()
                        windowManager.updateViewLayout(this@apply, layoutParams)
                    },
                    onClose = { stopSelf() },
                    onCollapse = { 
                        // 收起时清空结果
                        resultText = ""
                        errorText = ""
                        uiState = FloatingUIState.Collapsed 
                    },
                    onSelectImage = { /* TODO: 打开相册选图 */ }
                )
            }
        }

        windowManager.addView(floatingView, layoutParams)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    /**
     * 单击：截屏 -> OCR -> AI
     */
    private fun handleSingleTap() {
        if (isProcessing) {
            // 如果正在处理中，点击切换显示/隐藏处理状态
            uiState = if (uiState == FloatingUIState.Collapsed) {
                FloatingUIState.Processing
            } else {
                FloatingUIState.Collapsed
            }
            return
        }
        
        if (resultCode != Activity.RESULT_OK || resultData == null) {
            errorText = "截屏权限未获取，请重新点击悬浮窗按钮"
            uiState = FloatingUIState.Expanded
            return
        }
        
        serviceScope.launch {
            startScreenCapture()
        }
    }

    /**
     * 长按：展开扩展UI
     */
    private fun handleLongPress() {
        if (uiState == FloatingUIState.Expanded) {
            // 收起时清空结果
            resultText = ""
            errorText = ""
            uiState = FloatingUIState.Collapsed
        } else {
            // 展开时也清空之前的结果，显示快速操作菜单
            resultText = ""
            errorText = ""
            uiState = FloatingUIState.Expanded
        }
    }

    /**
     * 开始截屏流程
     */
    private suspend fun startScreenCapture() {
        isProcessing = true
        uiState = FloatingUIState.Collapsed
        resultText = ""
        errorText = ""

        try {
            // 1. 先隐藏悬浮窗
            withContext(Dispatchers.Main) {
                floatingView.visibility = android.view.View.INVISIBLE
            }
            
            // 等待悬浮窗隐藏
            delay(300)

            // 2. 执行截屏
            val bitmap = captureScreen()
            
            // 3. 恢复悬浮窗
            withContext(Dispatchers.Main) {
                floatingView.visibility = android.view.View.VISIBLE
            }

            if (bitmap == null) {
                errorText = "截屏失败"
                uiState = FloatingUIState.Expanded
                isProcessing = false
                return
            }

            Log.d(TAG, "截屏成功: ${bitmap.width}x${bitmap.height}")

            // TODO: 暂时跳过框选，直接将整张截图送给 OCR
            // 4. 启动裁剪界面
            // launchCropActivity(bitmap)
            
            // 直接处理整张截图
            processCroppedBitmap(bitmap)
            
        } catch (e: Exception) {
            Log.e(TAG, "截屏流程异常", e)
            withContext(Dispatchers.Main) {
                floatingView.visibility = android.view.View.VISIBLE
            }
            errorText = "截屏失败: ${e.message}"
            uiState = FloatingUIState.Expanded
            isProcessing = false
        }
    }

    /**
     * 执行屏幕截图
     */
    private suspend fun captureScreen(): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi

            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData!!)

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null, handler
            )

            // 等待截图完成
            delay(500)

            var bitmap: Bitmap? = null
            val image: Image? = imageReader?.acquireLatestImage()
            
            if (image != null) {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width

                bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                
                // 裁剪掉多余的部分
                if (bitmap.width > width) {
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                }
                
                image.close()
            }

            // 清理资源
            virtualDisplay?.release()
            imageReader?.close()
            mediaProjection?.stop()

            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "captureScreen 异常", e)
            null
        }
    }

    /**
     * 启动裁剪Activity
     */
    private fun launchCropActivity(bitmap: Bitmap) {
        // 保存截图到临时文件
        serviceScope.launch(Dispatchers.IO) {
            try {
                val file = java.io.File(cacheDir, "screenshot_temp.png")
                file.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                
                withContext(Dispatchers.Main) {
                    val intent = Intent(this@FloatingWindowService, ScreenCropActivity::class.java).apply {
                        putExtra(ScreenCropActivity.EXTRA_IMAGE_PATH, file.absolutePath)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "保存截图失败", e)
                withContext(Dispatchers.Main) {
                    errorText = "保存截图失败"
                    uiState = FloatingUIState.Expanded
                    isProcessing = false
                }
            }
        }
    }

    /**
     * 取消裁剪（从 ScreenCropActivity 回调）
     */
    fun cancelCrop() {
        Log.d(TAG, "裁剪已取消")
        isProcessing = false
        uiState = FloatingUIState.Collapsed
    }

    /**
     * 处理裁剪结果（从 ScreenCropActivity 回调）
     */
    fun processCroppedBitmap(bitmap: Bitmap) {
        serviceScope.launch {
            try {
                // 保持后台处理，不自动展开
                uiState = FloatingUIState.Collapsed
                isProcessing = true
                
                // OCR 识别
                Log.d(TAG, "开始 OCR 识别...")
                val ocrResult = ocrHelper.recognizeForSchedule(bitmap)
                
                if (!ocrResult.success) {
                    errorText = ocrResult.error ?: "OCR 识别失败"
                    uiState = FloatingUIState.Expanded
                    isProcessing = false
                    return@launch
                }
                
                Log.d(TAG, "OCR 识别成功: ${ocrResult.formattedText}")
                
                // 发送给 AI
                sendToAI(ocrResult.formattedText)
                
            } catch (e: Exception) {
                Log.e(TAG, "处理裁剪结果异常", e)
                errorText = "处理失败: ${e.message}"
                uiState = FloatingUIState.Expanded
                isProcessing = false
            }
        }
    }

    /**
     * 发送文本给 AI
     */
    private suspend fun sendToAI(text: String) {
        try {
            Log.d(TAG, "发送给 AI: $text")
            
            val response = withContext(Dispatchers.IO) {
                // token 由 AuthInterceptor 自动添加
                RetrofitClient.aiChatApiService.sendMessage(
                    request = ChatMessageRequest(message = text, sessionId = null)
                )
            }
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.code == 200 && body.data != null) {
                    resultText = body.data.message
                    Log.d(TAG, "AI 回复: $resultText")
                } else {
                    errorText = body?.message ?: "AI 处理失败"
                }
            } else {
                errorText = "请求失败: ${response.code()}"
            }
            
            uiState = FloatingUIState.Expanded
            isProcessing = false
            
        } catch (e: Exception) {
            Log.e(TAG, "发送给 AI 异常", e)
            errorText = "网络请求失败: ${e.message}"
            uiState = FloatingUIState.Expanded
            isProcessing = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清除静态引用
        ScreenCropActivity.setServiceInstance(null)
        serviceScope.cancel()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
        if (::lifecycleOwner.isInitialized) {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }
}

/**
 * 悬浮窗 UI 状态
 */
enum class FloatingUIState {
    Collapsed,   // 折叠状态（仅显示按钮）
    Expanded,    // 展开状态（显示结果/菜单）
    Processing   // 处理中
}

/**
 * 悬浮窗内容 Composable
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FloatingWindowContent(
    state: FloatingUIState,
    isProcessing: Boolean,
    resultText: String,
    errorText: String,
    onSingleTap: () -> Unit,
    onLongPress: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onClose: () -> Unit,
    onCollapse: () -> Unit,
    onSelectImage: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End
    ) {
        // 展开的内容
        AnimatedVisibility(
            visible = state == FloatingUIState.Expanded || state == FloatingUIState.Processing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    // 标题栏
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                isProcessing -> "处理中..."
                                errorText.isNotEmpty() -> "错误"
                                resultText.isNotEmpty() -> "AI 回复"
                                else -> "快速操作"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        IconButton(
                            onClick = onCollapse,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    when {
                        isProcessing -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                        errorText.isNotEmpty() -> {
                            Text(
                                text = errorText,
                                color = Color.Red,
                                fontSize = 13.sp
                            )
                        }
                        resultText.isNotEmpty() -> {
                            Text(
                                text = resultText,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .verticalScroll(rememberScrollState()),
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        else -> {
                            // 快速操作菜单
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.combinedClickable(
                                        onClick = { onSelectImage() }
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = "选择图片",
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                Color(0xFF7265E3).copy(alpha = 0.1f),
                                                CircleShape
                                            )
                                            .padding(8.dp),
                                        tint = Color(0xFF7265E3)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("相册", fontSize = 11.sp)
                                }
                                
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.combinedClickable(
                                        onClick = { onClose() }
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "关闭悬浮窗",
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                Color.Red.copy(alpha = 0.1f),
                                                CircleShape
                                            )
                                            .padding(8.dp),
                                        tint = Color.Red
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("关闭", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 悬浮按钮
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = if (isProcessing) Color(0xFF7265E3) else Color.Gray.copy(alpha = 0.8f),
                    shape = CircleShape
                )
                .combinedClickable(
                    onClick = { 
                        Log.d("FloatingWindow", "按钮被点击")
                        onSingleTap() 
                    },
                    onLongClick = { 
                        Log.d("FloatingWindow", "按钮被长按")
                        onLongPress() 
                    }
                )
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CameraAlt, 
                contentDescription = "截屏识别",
                tint = Color.White
            )
        }
    }
}
