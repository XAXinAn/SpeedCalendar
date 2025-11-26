package com.example.speedcalendar.features.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 截图裁剪Activity
 * 全屏显示截图，用户可以框选区域进行裁剪
 */
class ScreenCropActivity : ComponentActivity() {

    companion object {
        private const val TAG = "ScreenCropActivity"
        const val EXTRA_IMAGE_PATH = "image_path"
        
        // 用于与 FloatingWindowService 通信
        private var serviceInstance: FloatingWindowService? = null
        
        fun setServiceInstance(service: FloatingWindowService?) {
            serviceInstance = service
        }
    }

    private var imagePath: String? = null
    private var originalBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置全屏
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        if (imagePath == null) {
            Toast.makeText(this, "图片路径无效", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 加载截图
        originalBitmap = BitmapFactory.decodeFile(imagePath)
        if (originalBitmap == null) {
            Toast.makeText(this, "加载截图失败", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d(TAG, "加载截图: ${originalBitmap!!.width}x${originalBitmap!!.height}")

        setContent {
            CropScreen(
                bitmap = originalBitmap!!,
                onConfirm = { rect -> cropAndReturn(rect) },
                onCancel = { 
                    // 通知悬浮窗取消处理
                    serviceInstance?.cancelCrop()
                    finish() 
                }
            )
        }
    }

    /**
     * 裁剪并返回结果
     */
    private fun cropAndReturn(rect: RectF) {
        val bitmap = originalBitmap ?: return

        try {
            // 确保裁剪区域在图片范围内
            val left = rect.left.coerceIn(0f, bitmap.width.toFloat()).toInt()
            val top = rect.top.coerceIn(0f, bitmap.height.toFloat()).toInt()
            val right = rect.right.coerceIn(0f, bitmap.width.toFloat()).toInt()
            val bottom = rect.bottom.coerceIn(0f, bitmap.height.toFloat()).toInt()

            val width = (right - left).coerceAtLeast(1)
            val height = (bottom - top).coerceAtLeast(1)

            Log.d(TAG, "裁剪区域: left=$left, top=$top, width=$width, height=$height")

            val croppedBitmap = Bitmap.createBitmap(bitmap, left, top, width, height)

            // 通过静态引用传递结果给 FloatingWindowService
            serviceInstance?.processCroppedBitmap(croppedBitmap)

            finish()
        } catch (e: Exception) {
            Log.e(TAG, "裁剪失败", e)
            Toast.makeText(this, "裁剪失败: ${e.message}", Toast.LENGTH_SHORT).show()
            serviceInstance?.cancelCrop()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        originalBitmap?.recycle()
        originalBitmap = null
    }
}

/**
 * 裁剪界面 Composable
 */
@Composable
private fun CropScreen(
    bitmap: Bitmap,
    onConfirm: (RectF) -> Unit,
    onCancel: () -> Unit
) {
    // 裁剪框状态
    var startOffset by remember { mutableStateOf(Offset.Zero) }
    var endOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    var hasSelection by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. 显示截图作为背景
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "截图",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // 2. 半透明遮罩层 + 选择框
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            startOffset = offset
                            endOffset = offset
                            isDragging = true
                            hasSelection = false
                        },
                        onDrag = { change, _ ->
                            endOffset = change.position
                        },
                        onDragEnd = {
                            isDragging = false
                            hasSelection = true
                        }
                    )
                }
                .drawWithContent {
                    // 先画内容
                    drawContent()
                    
                    // 绘制半透明遮罩，选中区域挖空
                    if (hasSelection || isDragging) {
                        val left = minOf(startOffset.x, endOffset.x)
                        val top = minOf(startOffset.y, endOffset.y)
                        val right = maxOf(startOffset.x, endOffset.x)
                        val bottom = maxOf(startOffset.y, endOffset.y)

                        // 用 Path 挖空选中区域
                        val selectionPath = Path().apply {
                            addRect(Rect(left, top, right, bottom))
                        }

                        // 画遮罩（挖空选中区域）
                        clipPath(selectionPath, clipOp = ClipOp.Difference) {
                            drawRect(Color.Black.copy(alpha = 0.6f))
                        }

                        // 画选择框边框
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(left, top),
                            size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // 画四个角的装饰
                        val cornerLength = 20.dp.toPx()
                        val cornerStroke = 4.dp.toPx()
                        val cornerColor = Color(0xFF7265E3)

                        // 左上角
                        drawLine(cornerColor, Offset(left, top), Offset(left + cornerLength, top), cornerStroke)
                        drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLength), cornerStroke)
                        // 右上角
                        drawLine(cornerColor, Offset(right, top), Offset(right - cornerLength, top), cornerStroke)
                        drawLine(cornerColor, Offset(right, top), Offset(right, top + cornerLength), cornerStroke)
                        // 左下角
                        drawLine(cornerColor, Offset(left, bottom), Offset(left + cornerLength, bottom), cornerStroke)
                        drawLine(cornerColor, Offset(left, bottom), Offset(left, bottom - cornerLength), cornerStroke)
                        // 右下角
                        drawLine(cornerColor, Offset(right, bottom), Offset(right - cornerLength, bottom), cornerStroke)
                        drawLine(cornerColor, Offset(right, bottom), Offset(right, bottom - cornerLength), cornerStroke)
                    } else {
                        // 没有选择时，画半透明遮罩
                        drawRect(Color.Black.copy(alpha = 0.5f))
                    }
                }
        )

        // 提示文字
        if (!hasSelection && !isDragging) {
            Text(
                text = "拖动选择要识别的区域",
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // 底部操作栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 取消按钮
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Close, contentDescription = "取消")
                Spacer(modifier = Modifier.width(8.dp))
                Text("取消")
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 确认按钮
            Button(
                onClick = {
                    if (hasSelection) {
                        val left = minOf(startOffset.x, endOffset.x)
                        val top = minOf(startOffset.y, endOffset.y)
                        val right = maxOf(startOffset.x, endOffset.x)
                        val bottom = maxOf(startOffset.y, endOffset.y)
                        onConfirm(RectF(left, top, right, bottom))
                    }
                },
                enabled = hasSelection,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7265E3)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Check, contentDescription = "确认")
                Spacer(modifier = Modifier.width(8.dp))
                Text("确认")
            }
        }
    }
}
