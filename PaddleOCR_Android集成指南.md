# PaddleOCR Android端集成指南

> 本地化OCR识别，无需云端API，用户下载即用

## 📋 目录

1. [方案对比](#方案对比)
2. [方案一：快速集成（推荐新手）](#方案一快速集成推荐新手)
3. [方案二：官方Paddle Lite（推荐生产环境）](#方案二官方paddle-lite推荐生产环境)
4. [性能优化建议](#性能优化建议)
5. [常见问题](#常见问题)

---

## 方案对比

| 特性 | 方案一：paddleocr4android | 方案二：官方Paddle Lite |
|------|--------------------------|------------------------|
| **集成难度** | ⭐ 简单 | ⭐⭐⭐ 中等 |
| **APK增量** | ~50MB | ~10-15MB |
| **性能** | 中等 | 优秀 |
| **推理速度** | 较快 | 很快 |
| **自定义能力** | 有限 | 强大 |
| **适用场景** | 快速验证、小型项目 | 生产环境、大型项目 |
| **官方支持** | 社区维护 | 官方维护 |

---

## 方案一：快速集成（推荐新手）

### 使用 paddleocr4android 第三方库

#### 步骤1：添加依赖

**项目级 build.gradle**：
```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }  // 添加这行
    }
}
```

**模块级 build.gradle**：
```gradle
dependencies {
    implementation 'com.github.equationl:paddleocr4android:v1.2.9'
}
```

#### 步骤2：下载模型文件

从官方仓库下载预训练模型（6个文件）：

```bash
# 中文OCR v3模型
wget https://paddleocr.bj.bcebos.com/PP-OCRv3/chinese/ch_PP-OCRv3_det_infer.tar
wget https://paddleocr.bj.bcebos.com/PP-OCRv3/chinese/ch_PP-OCRv3_rec_infer.tar
wget https://paddleocr.bj.bcebos.com/dygraph_v2.0/ch/ch_ppocr_mobile_v2.0_cls_infer.tar

# 解压后需要的文件：
# - ch_PP-OCRv3_det_infer/inference.pdmodel
# - ch_PP-OCRv3_det_infer/inference.pdiparams
# - ch_PP-OCRv3_rec_infer/inference.pdmodel
# - ch_PP-OCRv3_rec_infer/inference.pdiparams
# - ch_ppocr_mobile_v2.0_cls_infer/inference.pdmodel
# - ch_ppocr_mobile_v2.0_cls_infer/inference.pdiparams
```

**或者直接下载已整理好的模型包**：
- [GitHub Release](https://github.com/PaddlePaddle/PaddleOCR/releases)

#### 步骤3：将模型文件放入项目

```
app/src/main/assets/
├── models/
│   ├── det/
│   │   ├── inference.pdmodel
│   │   └── inference.pdiparams
│   ├── rec/
│   │   ├── inference.pdmodel
│   │   └── inference.pdiparams
│   └── cls/
│       ├── inference.pdmodel
│       └── inference.pdiparams
└── ppocr_keys_v1.txt  # 字典文件
```

#### 步骤4：集成代码

**Kotlin实现**：

```kotlin
package com.example.speedcalendar.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.equationl.paddleocr4android.Ocr
import com.equationl.paddleocr4android.bean.OcrConfig
import com.equationl.paddleocr4android.callback.InitCallback
import com.equationl.paddleocr4android.callback.OcrRunCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * PaddleOCR管理类
 */
class PaddleOCRManager private constructor(private val context: Context) {

    private var ocr: Ocr? = null
    private var isInitialized = false

    companion object {
        @Volatile
        private var instance: PaddleOCRManager? = null

        fun getInstance(context: Context): PaddleOCRManager {
            return instance ?: synchronized(this) {
                instance ?: PaddleOCRManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * 初始化OCR引擎
     */
    suspend fun init(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized) {
            Log.d("PaddleOCR", "OCR已经初始化")
            return@withContext true
        }

        try {
            // 复制模型文件到缓存目录
            val modelDir = File(context.cacheDir, "ocr_models")
            if (!modelDir.exists()) {
                copyModelsToCache(modelDir)
            }

            // 配置OCR
            val config = OcrConfig.Builder()
                .setDetPath(File(modelDir, "det/inference.pdmodel").absolutePath)
                .setDetParams(File(modelDir, "det/inference.pdiparams").absolutePath)
                .setRecPath(File(modelDir, "rec/inference.pdmodel").absolutePath)
                .setRecParams(File(modelDir, "rec/inference.pdiparams").absolutePath)
                .setClsPath(File(modelDir, "cls/inference.pdmodel").absolutePath)
                .setClsParams(File(modelDir, "cls/inference.pdiparams").absolutePath)
                .setKeyPath(File(modelDir, "ppocr_keys_v1.txt").absolutePath)
                .setUseGpu(false)  // 使用CPU
                .setCpuThreadNum(4)  // CPU线程数
                .setPrecision(OcrConfig.Precision.FP16)  // FP16精度
                .setRunType(OcrConfig.RunType.DET_REC_CLS)  // 检测+识别+方向分类
                .build()

            // 初始化OCR（同步方式）
            ocr = Ocr.getInstance(context)
            val result = ocr?.initModel(config)
            isInitialized = result == true

            Log.d("PaddleOCR", "OCR初始化${if (isInitialized) "成功" else "失败"}")
            isInitialized

        } catch (e: Exception) {
            Log.e("PaddleOCR", "OCR初始化异常", e)
            false
        }
    }

    /**
     * 执行OCR识别
     */
    suspend fun recognize(bitmap: Bitmap): OcrResult = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            return@withContext OcrResult(
                success = false,
                text = "",
                error = "OCR引擎未初始化"
            )
        }

        try {
            var resultText = ""
            var inferenceTime = 0L
            var success = false

            ocr?.run(bitmap, object : OcrRunCallback {
                override fun onSuccess(data: List<String>?, inTime: Long) {
                    success = true
                    resultText = data?.joinToString("\n") ?: ""
                    inferenceTime = inTime
                    Log.d("PaddleOCR", "识别成功: $resultText, 耗时: ${inTime}ms")
                }

                override fun onFail(code: Int, msg: String?) {
                    Log.e("PaddleOCR", "识别失败: code=$code, msg=$msg")
                }
            })

            // 等待识别完成（简单实现，实际应使用协程或回调）
            Thread.sleep(100)

            OcrResult(
                success = success,
                text = resultText,
                inferenceTime = inferenceTime
            )

        } catch (e: Exception) {
            Log.e("PaddleOCR", "OCR识别异常", e)
            OcrResult(
                success = false,
                text = "",
                error = e.message
            )
        }
    }

    /**
     * 复制assets中的模型文件到缓存目录
     */
    private fun copyModelsToCache(targetDir: File) {
        targetDir.mkdirs()

        val files = mapOf(
            "models/det/inference.pdmodel" to "det/inference.pdmodel",
            "models/det/inference.pdiparams" to "det/inference.pdiparams",
            "models/rec/inference.pdmodel" to "rec/inference.pdmodel",
            "models/rec/inference.pdiparams" to "rec/inference.pdiparams",
            "models/cls/inference.pdmodel" to "cls/inference.pdmodel",
            "models/cls/inference.pdiparams" to "cls/inference.pdiparams",
            "ppocr_keys_v1.txt" to "ppocr_keys_v1.txt"
        )

        files.forEach { (assetPath, targetPath) ->
            val targetFile = File(targetDir, targetPath)
            targetFile.parentFile?.mkdirs()

            context.assets.open(assetPath).use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        Log.d("PaddleOCR", "模型文件复制完成")
    }

    /**
     * 释放资源
     */
    fun release() {
        ocr?.release()
        isInitialized = false
        Log.d("PaddleOCR", "OCR资源已释放")
    }
}

/**
 * OCR识别结果
 */
data class OcrResult(
    val success: Boolean,
    val text: String,
    val inferenceTime: Long = 0,
    val error: String? = null
)
```

#### 步骤5：使用示例

**在ViewModel中使用**：

```kotlin
class ImageOcrViewModel(application: Application) : AndroidViewModel(application) {

    private val ocrManager = PaddleOCRManager.getInstance(application)

    private val _ocrResult = MutableStateFlow<OcrResult?>(null)
    val ocrResult: StateFlow<OcrResult?> = _ocrResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // 应用启动时初始化OCR
        viewModelScope.launch {
            ocrManager.init()
        }
    }

    fun recognizeImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = ocrManager.recognize(bitmap)
                _ocrResult.value = result
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ocrManager.release()
    }
}
```

**在UI中使用**：

```kotlin
@Composable
fun OcrScreen(viewModel: ImageOcrViewModel = viewModel()) {
    val ocrResult by viewModel.ocrResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // 图片选择器
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // 加载bitmap
            val bitmap = loadBitmapFromUri(uri)
            selectedBitmap = bitmap
            bitmap?.let { viewModel.recognizeImage(it) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 选择图片按钮
        Button(onClick = { launcher.launch("image/*") }) {
            Text("选择图片")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 显示图片
        selectedBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 显示识别结果
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            ocrResult?.let { result ->
                if (result.success) {
                    Text(
                        text = "识别结果：\n${result.text}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "耗时：${result.inferenceTime}ms",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "识别失败：${result.error}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
```

---

## 方案二：官方Paddle Lite（推荐生产环境）

### 优势
- ✅ APK体积更小（10-15MB）
- ✅ 推理速度更快
- ✅ 支持模型量化（INT8）
- ✅ 官方长期维护

### 步骤1：环境准备

**1.1 安装PaddleLite转换工具**：
```bash
pip install paddlelite==2.10
```

**1.2 下载并转换模型**：

```bash
# 下载PP-OCRv3模型
wget https://paddleocr.bj.bcebos.com/PP-OCRv3/chinese/ch_PP-OCRv3_det_infer.tar
wget https://paddleocr.bj.bcebos.com/PP-OCRv3/chinese/ch_PP-OCRv3_rec_infer.tar
wget https://paddleocr.bj.bcebos.com/dygraph_v2.0/ch/ch_ppocr_mobile_v2.0_cls_infer.tar

# 解压
tar -xf ch_PP-OCRv3_det_infer.tar
tar -xf ch_PP-OCRv3_rec_infer.tar
tar -xf ch_ppocr_mobile_v2.0_cls_infer.tar

# 转换为.nb格式（适用于移动端）
paddle_lite_opt \
    --model_file=ch_PP-OCRv3_det_infer/inference.pdmodel \
    --param_file=ch_PP-OCRv3_det_infer/inference.pdiparams \
    --optimize_out=ch_PP-OCRv3_det_infer/det \
    --valid_targets=arm

paddle_lite_opt \
    --model_file=ch_PP-OCRv3_rec_infer/inference.pdmodel \
    --param_file=ch_PP-OCRv3_rec_infer/inference.pdiparams \
    --optimize_out=ch_PP-OCRv3_rec_infer/rec \
    --valid_targets=arm

paddle_lite_opt \
    --model_file=ch_ppocr_mobile_v2.0_cls_infer/inference.pdmodel \
    --param_file=ch_ppocr_mobile_v2.0_cls_infer/inference.pdiparams \
    --optimize_out=ch_ppocr_mobile_v2.0_cls_infer/cls \
    --valid_targets=arm

# 生成的.nb文件：
# - det.nb
# - rec.nb
# - cls.nb
```

### 步骤2：集成Paddle Lite SDK

**2.1 添加依赖**：

```gradle
dependencies {
    implementation 'com.baidu.paddle.lite:android_api:2.10.0'
}
```

**2.2 配置NDK**：

```gradle
android {
    defaultConfig {
        ndk {
            abiFilters 'armeabi-v7a', 'arm64-v8a'
        }
    }
}
```

### 步骤3：实现OCR代码

**PaddleLiteOCR.kt**：

```kotlin
package com.example.speedcalendar.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.baidu.paddle.lite.MobileConfig
import com.baidu.paddle.lite.PaddlePredictor
import com.baidu.paddle.lite.PowerMode
import com.baidu.paddle.lite.Tensor
import java.io.File

class PaddleLiteOCR(private val context: Context) {

    private var detPredictor: PaddlePredictor? = null
    private var recPredictor: PaddlePredictor? = null
    private var clsPredictor: PaddlePredictor? = null

    fun init(): Boolean {
        return try {
            val modelDir = copyModelsToCache()

            // 初始化检测模型
            detPredictor = createPredictor(File(modelDir, "det.nb").absolutePath)

            // 初始化识别模型
            recPredictor = createPredictor(File(modelDir, "rec.nb").absolutePath)

            // 初始化方向分类模型
            clsPredictor = createPredictor(File(modelDir, "cls.nb").absolutePath)

            Log.d("PaddleLite", "模型初始化成功")
            true
        } catch (e: Exception) {
            Log.e("PaddleLite", "模型初始化失败", e)
            false
        }
    }

    private fun createPredictor(modelPath: String): PaddlePredictor {
        val config = MobileConfig()
        config.setModelFromFile(modelPath)
        config.setThreads(4)
        config.setPowerMode(PowerMode.LITE_POWER_HIGH)

        return PaddlePredictor.createPaddlePredictor(config)
    }

    fun recognize(bitmap: Bitmap): String {
        // 1. 检测文字区域
        val boxes = detectText(bitmap)

        // 2. 对每个区域进行识别
        val results = boxes.map { box ->
            val croppedBitmap = cropBitmap(bitmap, box)
            recognizeText(croppedBitmap)
        }

        return results.joinToString("\n")
    }

    private fun detectText(bitmap: Bitmap): List<Box> {
        // TODO: 实现文字检测逻辑
        // 1. 预处理图片
        // 2. 输入到检测模型
        // 3. 后处理得到文字框
        return emptyList()
    }

    private fun recognizeText(bitmap: Bitmap): String {
        // TODO: 实现文字识别逻辑
        // 1. 预处理图片
        // 2. 输入到识别模型
        // 3. 解码得到文字
        return ""
    }

    private fun copyModelsToCache(): File {
        val modelDir = File(context.cacheDir, "paddle_lite_models")
        modelDir.mkdirs()

        listOf("det.nb", "rec.nb", "cls.nb").forEach { filename ->
            context.assets.open("models/$filename").use { input ->
                File(modelDir, filename).outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        return modelDir
    }

    fun release() {
        detPredictor?.destroy()
        recPredictor?.destroy()
        clsPredictor?.destroy()
    }
}

data class Box(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float
)
```

**注意**：完整的Paddle Lite实现较为复杂，需要处理：
1. 图像预处理（归一化、缩放）
2. Tensor数据转换
3. 后处理（NMS、解码等）

建议参考官方Android Demo：
- [PaddleOCR Android Demo](https://github.com/PaddlePaddle/PaddleOCR/tree/release/2.6/deploy/lite)

---

## 性能优化建议

### 1. 模型选择
- **PP-OCRv3**：精度最高，速度中等
- **PP-OCRv2 mobile**：速度最快，精度较高
- **量化模型（INT8）**：体积更小，速度更快

### 2. 推理优化
```kotlin
// 使用FP16精度（速度快，精度略降）
config.setPrecision(OcrConfig.Precision.FP16)

// 增加CPU线程数
config.setCpuThreadNum(4)

// 使用GPU加速（需要设备支持）
config.setUseGpu(true)
```

### 3. 图片预处理
```kotlin
// 压缩图片到合适大小
fun resizeBitmap(bitmap: Bitmap, maxWidth: Int = 960): Bitmap {
    if (bitmap.width <= maxWidth) return bitmap

    val ratio = maxWidth.toFloat() / bitmap.width
    val newHeight = (bitmap.height * ratio).toInt()

    return Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
}
```

### 4. 异步处理
```kotlin
// 在后台线程处理OCR
viewModelScope.launch(Dispatchers.Default) {
    val result = ocrManager.recognize(bitmap)
    withContext(Dispatchers.Main) {
        _ocrResult.value = result
    }
}
```

---

## 常见问题

### Q1: APK体积增加太多怎么办？
**A**:
1. 使用Paddle Lite官方方案（10-15MB）
2. 使用量化模型（INT8）
3. 按需加载模型（首次使用时下载）

### Q2: 识别速度慢怎么办？
**A**:
1. 压缩输入图片到960px以内
2. 使用FP16精度
3. 增加CPU线程数
4. 考虑使用mobile版模型

### Q3: 识别准确率低怎么办？
**A**:
1. 使用PP-OCRv3模型
2. 确保图片清晰度足够
3. 对图片进行预处理（去噪、二值化）
4. 训练自定义模型

### Q4: 支持哪些语言？
**A**:
- 中文、英文、数字（内置）
- 其他语言需要下载对应模型

### Q5: 如何支持竖排文字？
**A**:
使用方向分类模型（cls），配置时设置：
```kotlin
.setRunType(OcrConfig.RunType.DET_REC_CLS)
```

---

## 推荐学习资源

### 官方文档
- [PaddleOCR GitHub](https://github.com/PaddlePaddle/PaddleOCR)
- [Paddle Lite文档](https://paddlepaddle.github.io/Paddle-Lite)
- [Android部署指南](https://paddlepaddle.github.io/PaddleOCR/ppocr/infer_deploy/android_demo.html)

### 社区资源
- [PaddleOCR4Android](https://github.com/equationl/paddleocr4android)
- [CSDN Android部署教程](https://blog.csdn.net/YY007H/article/details/124774019)
- [知乎PaddleOCR系列](https://zhuanlan.zhihu.com/p/551649164)

### 模型下载
- [官方模型库](https://paddlepaddle.github.io/PaddleOCR/ppocr/model_list.html)
- [百度云盘分享](https://pan.baidu.com/s/1getAprT2l_JqwhjwML0g9g) （提取码：lmv7）

---

## 下一步计划

### 阶段1：基础集成（1-2天）
- [ ] 添加依赖
- [ ] 下载模型文件
- [ ] 集成基础代码
- [ ] 测试单张图片识别

### 阶段2：功能完善（2-3天）
- [ ] 添加图片选择功能
- [ ] 添加相机拍照功能
- [ ] 优化UI/UX
- [ ] 添加识别结果编辑

### 阶段3：性能优化（1-2天）
- [ ] 压缩模型体积
- [ ] 优化推理速度
- [ ] 添加缓存机制
- [ ] 性能监控

### 阶段4：生产部署（1天）
- [ ] 错误处理
- [ ] 日志记录
- [ ] 权限管理
- [ ] 发布测试

---

**总结**：
- **新手推荐**：使用 paddleocr4android 快速集成
- **生产环境**：使用官方 Paddle Lite，APK体积更小，性能更好
- **预计开发时间**：5-8天完成完整功能

如有问题，欢迎随时咨询！🎉
