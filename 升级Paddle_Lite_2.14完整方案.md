# 升级 Paddle Lite 到 2.14 版本完整方案

## 一、下载最新版 SDK（v2.14-rc）

**重要**: 使用 **c++_static.with_extra** 版本（内置所有依赖 + 全量算子集合，避免 "Please use Paddle-Lite lib with all ops" 崩溃）

### 1. 下载 ARM64 版本
```
链接: https://github.com/PaddlePaddle/Paddle-Lite/releases/download/v2.14-rc/inference_lite_lib.android.armv8.clang.c++_static.with_extra.tar.gz
保存到: C:\Users\18241\Desktop\paddle\armv8_2.14_static_with_extra.tar.gz
```

### 2. 下载 ARM32 版本
```
链接: https://github.com/PaddlePaddle/Paddle-Lite/releases/download/v2.14-rc/inference_lite_lib.android.armv7.clang.c++_static.with_extra.tar.gz
保存到: C:\Users\18241\Desktop\paddle\armv7_2.14_static_with_extra.tar.gz
```

**方式**: 用浏览器直接下载，或使用迅雷/IDM等下载工具

- **c++_static.with_extra vs c++_static vs c++_shared**:
- **c++_static.with_extra**: 静态链接 + **完整算子**，可以跑 PP-OCR 的所有算子（推荐）
- **c++_static**: 仅包含常用算子，加载 PP-OCR 时会触发 `Check failed: op ... tiny_publish` 崩溃
- **c++_shared**: 需要外部提供 libc++_shared.so（容易出问题）

---

## 二、解压并提取文件

### 1. 解压SDK包
```powershell
cd C:\Users\18241\Desktop\paddle
tar -xzf armv8_2.14_static_with_extra.tar.gz
tar -xzf armv7_2.14_static_with_extra.tar.gz
```

### 2. 删除旧文件
```powershell
cd C:\Users\18241\Desktop\SpeedCalendar
Remove-Item "app\libs\PaddlePredictor.jar" -Force
Remove-Item "app\src\main\jniLibs\*\*.so" -Recurse -Force
```

### 3. 复制新的 JAR 文件
```powershell
Copy-Item "C:\Users\18241\Desktop\paddle\inference_lite_lib.android.armv8.clang.c++_static.with_extra\java\PaddlePredictor.jar" "app\libs\"
```

### 4. 复制 ARM64 的 .so 文件（仅2个文件，无需 libc++_shared.so）
```powershell
# 复制到 app\src\main\jniLibs\arm64-v8a\
Copy-Item "C:\Users\18241\Desktop\paddle\inference_lite_lib.android.armv8.clang.c++_static.with_extra\cxx\lib\libpaddle_light_api_shared.so" "app\src\main\jniLibs\arm64-v8a\"
Copy-Item "C:\Users\18241\Desktop\paddle\inference_lite_lib.android.armv8.clang.c++_static.with_extra\java\so\libpaddle_lite_jni.so" "app\src\main\jniLibs\arm64-v8a\"
```

### 5. 复制 ARM32 的 .so 文件（仅2个文件，无需 libc++_shared.so）
```powershell
# 复制到 app\src\main\jniLibs\armeabi-v7a\
Copy-Item "C:\Users\18241\Desktop\paddle\inference_lite_lib.android.armv7.clang.c++_static.with_extra\cxx\lib\libpaddle_light_api_shared.so" "app\src\main\jniLibs\armeabi-v7a\"
Copy-Item "C:\Users\18241\Desktop\paddle\inference_lite_lib.android.armv7.clang.c++_static.with_extra\java\so\libpaddle_lite_jni.so" "app\src\main\jniLibs\armeabi-v7a\"
```

**关键优势**: c++_static 版本的 .so 文件体积更大，但包含了所有依赖，**不再需要 libc++_shared.so**

---

## 三、下载最新的模型转换工具

### 1. 下载 opt 工具（Linux版本）
```
链接: https://github.com/PaddlePaddle/Paddle-Lite/releases/download/v2.14-rc/opt_linux
保存到: C:\Users\18241\Desktop\paddle\opt_linux
```

### 2. 给工具添加执行权限（如果在Linux/WSL下）
```bash
chmod +x opt_linux
```

---

## 四、下载并转换最新模型文件

### 1. 下载 PP-OCRv4 原始模型（必须更新）

**重要**: 必须使用与 Paddle Lite 2.14 兼容的最新模型

#### 检测模型（det）
```
下载链接: https://paddleocr.bj.bcebos.com/PP-OCRv4/chinese/ch_PP-OCRv4_det_infer.tar
解压后目录: ch_PP-OCRv4_det_infer/
包含文件: inference.pdmodel, inference.pdiparams
```

#### 识别模型（rec）
```
下载链接: https://paddleocr.bj.bcebos.com/PP-OCRv4/chinese/ch_PP-OCRv4_rec_infer.tar
解压后目录: ch_PP-OCRv4_rec_infer/
包含文件: inference.pdmodel, inference.pdiparams
```

#### 方向分类模型（cls）
```
下载链接: https://paddleocr.bj.bcebos.com/dygraph_v2.0/ch/ch_ppocr_mobile_v2.0_cls_infer.tar
解压后目录: ch_ppocr_mobile_v2.0_cls_infer/
包含文件: inference.pdmodel, inference.pdiparams
```

**下载和解压命令**:
```powershell
cd C:\Users\18241\Desktop\paddle

# 下载（用浏览器或 Invoke-WebRequest）
$ProgressPreference = 'SilentlyContinue'
Invoke-WebRequest -Uri "https://paddleocr.bj.bcebos.com/PP-OCRv4/chinese/ch_PP-OCRv4_det_infer.tar" -OutFile "det.tar"
Invoke-WebRequest -Uri "https://paddleocr.bj.bcebos.com/PP-OCRv4/chinese/ch_PP-OCRv4_rec_infer.tar" -OutFile "rec.tar"
Invoke-WebRequest -Uri "https://paddleocr.bj.bcebos.com/dygraph_v2.0/ch/ch_ppocr_mobile_v2.0_cls_infer.tar" -OutFile "cls.tar"

# 解压
tar -xf det.tar
tar -xf rec.tar
tar -xf cls.tar
```

### 2. 转换检测模型
```bash
./opt_linux \
  --model_file=ch_PP-OCRv4_det_infer/inference.pdmodel \
  --param_file=ch_PP-OCRv4_det_infer/inference.pdiparams \
  --optimize_out=det \
  --optimize_out_type=naive_buffer \
  --valid_targets=arm
```
生成: `det.nb`

### 3. 转换识别模型
```bash
./opt_linux \
  --model_file=ch_PP-OCRv4_rec_infer/inference.pdmodel \
  --param_file=ch_PP-OCRv4_rec_infer/inference.pdiparams \
  --optimize_out=rec \
  --optimize_out_type=naive_buffer \
  --valid_targets=arm
```
生成: `rec.nb`

### 4. 转换方向分类模型
```bash
./opt_linux \
  --model_file=ch_ppocr_mobile_v2.0_cls_infer/inference.pdmodel \
  --param_file=ch_ppocr_mobile_v2.0_cls_infer/inference.pdiparams \
  --optimize_out=cls \
  --optimize_out_type=naive_buffer \
  --valid_targets=arm
```
生成: `cls.nb`

### 5. 复制新模型到项目
```powershell
Copy-Item "det.nb" "C:\Users\18241\Desktop\SpeedCalendar\app\src\main\assets\models\" -Force
Copy-Item "rec.nb" "C:\Users\18241\Desktop\SpeedCalendar\app\src\main\assets\models\" -Force
Copy-Item "cls.nb" "C:\Users\18241\Desktop\SpeedCalendar\app\src\main\assets\models\" -Force
```

---

## 五、验证文件结构

运行以下命令检查所有文件是否就位：

```powershell
cd C:\Users\18241\Desktop\SpeedCalendar

# 检查 JAR
Get-Item "app\libs\PaddlePredictor.jar"

# 检查 .so 文件（应该有4个，c++_static版本无需libc++_shared.so）
Get-ChildItem "app\src\main\jniLibs" -Recurse -Filter "*.so" | ForEach-Object { 
    "{0,-30} {1,10} KB" -f "$($_.Directory.Name)/$($_.Name)", ([math]::Round($_.Length/1KB,2)) 
}

# 检查模型文件（应该有3个）
Get-ChildItem "app\src\main\assets\models" -Filter "*.nb" | ForEach-Object {
    "{0,-15} {1,10} KB" -f $_.Name, ([math]::Round($_.Length/1KB,2))
}
```

**期望输出**:
```
arm64-v8a/libpaddle_light_api_shared.so    ~2000 KB (c++_static版本更大)
arm64-v8a/libpaddle_lite_jni.so           ~2000 KB (c++_static版本更大)
armeabi-v7a/libpaddle_light_api_shared.so  ~1100 KB
armeabi-v7a/libpaddle_lite_jni.so         ~1100 KB

det.nb         xxxx KB
rec.nb         xxxx KB
cls.nb         xxxx KB
```
> 如果日志中再次出现 `Check failed: op: Error: Please use Paddle-Lite lib with all ops`，说明仍然误用了 tiny_publish 版本，请重新复制 with_extra 目录下的 .so 文件。

---

## 六、构建并测试

### 1. 清理项目
```powershell
.\gradlew clean
```

### 2. 重新构建
```powershell
.\gradlew assembleDebug
```

### 3. 安装到手机测试
```powershell
.\gradlew installDebug
```

### 4. 查看日志
```powershell
adb logcat | Select-String "OCR"
```

**成功标志**: 日志中出现 "OCR 引擎初始化成功！"

---

## 七、常见问题

### Q1: c++_static 和 c++_shared 有什么区别？
**A**: 
- **c++_static**: C++标准库**静态链接**到.so中，体积更大（~2MB），但无需额外依赖
- **c++_shared**: C++标准库**动态链接**，体积小（~1MB），但需要额外的 libc++_shared.so
- **推荐使用 c++_static**，避免依赖问题

### Q2: 如果一定要用 c++_shared 版本怎么办？
**A**: 需要从 Android NDK 获取 libc++_shared.so
```powershell
# 1. 下载 NDK r21e (约600MB)
# https://dl.google.com/android/repository/android-ndk-r21e-windows-x86_64.zip

# 2. 解压后复制
Copy-Item "android-ndk-r21e\sources\cxx-stl\llvm-libc++\libs\arm64-v8a\libc++_shared.so" "app\src\main\jniLibs\arm64-v8a\"
Copy-Item "android-ndk-r21e\sources\cxx-stl\llvm-libc++\libs\armeabi-v7a\libc++_shared.so" "app\src\main\jniLibs\armeabi-v7a\"
```

### Q3: opt工具转换失败？
**A**: 
1. 确认opt工具版本是2.14-rc
2. 检查原始模型文件是否完整
3. 使用绝对路径

### Q4: 运行时还是报 library not found？
**A**: 
1. 确认使用的是 c++_static 版本（只需4个.so文件）
2. 用上面的验证命令检查.so文件是否都存在
3. 卸载旧APK后重新安装
4. 检查ABI过滤配置是否正确

---

## 八、回滚方案（如果新版有问题）

如果升级后出现问题，可以回退：

1. 恢复2.10版本的文件
2. 只添加缺失的 `libc++_shared.so`（从NDK获取）
3. 保持原有的 .nb 模型文件

---

## 总结

**升级核心改动**:
1. ✅ SDK从2.10 → 2.14 (使用 c++_static.with_extra 版本)
2. ✅ JAR和.so文件更新（4个.so文件，含完整算子，无需额外依赖）
3. ✅ 用新版opt工具重新转换模型为PP-OCRv4
4. ✅ 重新构建APK

**关键变化**:
- 从 c++_shared 改为 **c++_static** 版本
- .so文件从6个减少到4个（无需libc++_shared.so）
- 模型升级到PP-OCRv4（性能和精度提升）

**预期效果**:
- 解决 library not found 错误
- OCR功能正常工作
- 支持最新的模型格式
