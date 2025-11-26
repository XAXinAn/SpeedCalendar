# 下载 libc++_shared.so 脚本
# 用途：为 Paddle Lite c++_shared 版本下载必需的 C++ 标准库

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " 下载 libc++_shared.so" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# NDK r21e 的直接下载链接（可靠的 CDN）
$armv7Url = "https://raw.githubusercontent.com/android/ndk/r21/toolchains/llvm/prebuilt/windows-x86_64/sysroot/usr/lib/arm-linux-androideabi/libc++_shared.so"
$armv8Url = "https://raw.githubusercontent.com/android/ndk/r21/toolchains/llvm/prebuilt/windows-x86_64/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$armv7Dir = "$projectRoot\app\src\main\jniLibs\armeabi-v7a"
$armv8Dir = "$projectRoot\app\src\main\jniLibs\arm64-v8a"

# 确保目录存在
New-Item -ItemType Directory -Force -Path $armv7Dir | Out-Null
New-Item -ItemType Directory -Force -Path $armv8Dir | Out-Null

Write-Host "📥 下载 ARMv7 版本..." -ForegroundColor Yellow
try {
    Invoke-WebRequest -Uri $armv7Url -OutFile "$armv7Dir\libc++_shared.so" -TimeoutSec 60
    $file = Get-Item "$armv7Dir\libc++_shared.so"
    Write-Host "   ✓ 成功 ($([math]::Round($file.Length/1KB,2)) KB)" -ForegroundColor Green
} catch {
    Write-Host "   ✗ 失败: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   请手动下载: $armv7Url" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "📥 下载 ARMv8 版本..." -ForegroundColor Yellow
try {
    Invoke-WebRequest -Uri $armv8Url -OutFile "$armv8Dir\libc++_shared.so" -TimeoutSec 60
    $file = Get-Item "$armv8Dir\libc++_shared.so"
    Write-Host "   ✓ 成功 ($([math]::Round($file.Length/1KB,2)) KB)" -ForegroundColor Green
} catch {
    Write-Host "   ✗ 失败: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   请手动下载: $armv8Url" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " 当前 jniLibs 文件列表" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

Get-ChildItem "$projectRoot\app\src\main\jniLibs" -Recurse -Filter "*.so" | ForEach-Object {
    $arch = $_.Directory.Name
    $size = [math]::Round($_.Length/1KB,2)
    Write-Host "  [$arch] $($_.Name) - ${size} KB"
}

Write-Host ""
Write-Host "Done! Please rebuild the project." -ForegroundColor Green
