# PaddleOCR Android 集成 - Windows 快速开始指南

## 🚨 重要提示

**Windows 用户无需安装 PaddleLite Python 包！**

PaddleLite Python 包在 Windows 上不可用，但这不影响 Android 集成。你有两个简单的方案：

---

## ✅ 方案A：使用 paddleocr4android 库（推荐新手）

这是最简单的方案，无需任何模型转换。

### 步骤1：下载模型文件

在 Git Bash 中运行：
```bash
cd C:\Users\18241\Desktop\SpeedCalendar
bash download_ocr_models.sh
```

**说明**：脚本会自动下载原始模型（.pdmodel 和 .pdiparams），不需要转换为 .nb 格式。

### 步骤2：复制模型到 Android 项目

```bash
cp -r ocr_models/android_assets/* app/src/main/assets/
```

或者手动复制 `ocr_models/android_assets/` 目录下的所有文件到：
```
SpeedCalendar/app/src/main/assets/
```

### 步骤3：添加依赖

编辑 `app/build.gradle`：

```gradle
dependencies {
    // PaddleOCR4Android - 包含所有必要组件
    implementation 'io.github.mymonstercat:paddleocr4android:2.0.0'

    // 其他已有的依赖...
}
```

### 步骤4：初始化 OCR

创建文件 `app/src/main/java/com/example/speedcalendar/utils/OCRManager.kt`：

```kotlin
package com.example.speedcalendar.utils

import android.content.Context
import android.graphics.Bitmap
import com.benjaminwan.ocrlibrary.OcrEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OCRManager private constructor(private val context: Context) {

    private var ocrEngine: OcrEngine? = null
    private var isInitialized = false

    companion object {
        @Volatile
        private var instance: OCRManager? = null

        fun getInstance(context: Context): OCRManager {
            return instance ?: synchronized(this) {
                instance ?: OCRManager(context.applicationContext).also { instance = it }
            }
        }
    }

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isInitialized) return@withContext true

            ocrEngine = OcrEngine(context)

            // 配置模型路径（相对于 assets 目录）
            val result = ocrEngine?.initModels(
                detPath = "models/det",
                clsPath = "models/cls",
                recPath = "models/rec",
                keysPath = "ppocr_keys_v1.txt"
            )

            isInitialized = result == true
            isInitialized
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun recognizeText(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            return@withContext "OCR引擎未初始化"
        }

        try {
            val ocrResult = ocrEngine?.detect(bitmap)
            ocrResult?.strRes ?: "识别失败"
        } catch (e: Exception) {
            e.printStackTrace()
            "识别错误: ${e.message}"
        }
    }

    fun release() {
        isInitialized = false
        // paddleocr4android 的 release 逻辑（如果有）
    }
}
```

### 步骤5：使用示例

```kotlin
// 在 ViewModel 或 Activity 中
class HomeViewModel : ViewModel() {

    private val ocrManager = OCRManager.getInstance(getApplication())

    init {
        viewModelScope.launch {
            val success = ocrManager.initialize()
            if (success) {
                Log.d("OCR", "OCR引擎初始化成功")
            }
        }
    }

    fun recognizeScheduleImage(bitmap: Bitmap) {
        viewModelScope.launch {
            val text = ocrManager.recognizeText(bitmap)
            Log.d("OCR", "识别结果: $text")
            // 处理识别结果...
        }
    }
}
```

### 优点
- ✅ 集成简单，一行依赖即可
- ✅ 无需模型转换
- ✅ 适合快速验证功能

### 缺点
- ❌ APK 增大约 50MB
- ❌ 性能不如官方 Paddle Lite

---

## 🔧 方案B：从官方下载预转换模型（推荐生产环境）

如果你需要更小的 APK 和更好的性能，可以使用官方预转换的 .nb 模型。

### 步骤1：下载预转换模型

访问 PaddleOCR GitHub Releases：
https://github.com/PaddlePaddle/PaddleOCR/releases

下载已转换好的移动端模型包（通常包含 .nb 文件）。

### 步骤2：集成 Paddle Lite SDK

参考官方文档：
https://paddlepaddle.github.io/PaddleOCR/main/en/version2.x/legacy/lite.html

这种方法需要：
1. 手动下载 Paddle Lite AAR
2. 配置 JNI 和 C++ 依赖
3. 编写 JNI 调用代码

### 优点
- ✅ APK 增大仅 10-15MB
- ✅ 性能最优
- ✅ 官方支持

### 缺点
- ❌ 配置复杂
- ❌ 需要了解 JNI

---

## 📝 推荐步骤总结

**对于你的项目，建议先用方案A快速验证功能：**

1. ✅ 运行 `download_ocr_models.sh` 下载模型
2. ✅ 复制模型到 assets 目录
3. ✅ 添加 `paddleocr4android:2.0.0` 依赖
4. ✅ 创建 OCRManager.kt 工具类
5. ✅ 在需要的地方调用识别功能
6. ✅ 测试验证功能是否满足需求

**如果后续需要优化 APK 大小，再切换到方案B。**

---

## 🛠️ 故障排查

### 问题1：找不到模型文件
**原因**：模型未正确复制到 assets 目录

**解决**：
```bash
# 检查文件是否存在
ls -la app/src/main/assets/models/det/
ls -la app/src/main/assets/ppocr_keys_v1.txt
```

### 问题2：初始化失败
**原因**：模型路径配置错误

**解决**：确保路径相对于 assets 根目录，不要加 "assets/" 前缀

### 问题3：识别效果差
**原因**：图片质量、光线、角度等因素

**解决**：
- 确保图片清晰、光线充足
- 尝试预处理图片（灰度化、二值化）
- 调整 OCR 参数（阈值、缩放比例等）

---

## 📚 参考资料

- [paddleocr4android GitHub](https://github.com/mymonstercat/paddleocr4android)
- [PaddleOCR 官方文档](https://paddlepaddle.github.io/PaddleOCR/)
- [PaddleOCR Android 部署](https://paddlepaddle.github.io/PaddleOCR/main/en/version2.x/legacy/lite.html)

---

**现在就开始吧！运行模型下载脚本即可。**
