package com.example.speedcalendar.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.speedcalendar.data.api.RetrofitClient
import com.example.speedcalendar.data.local.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * 头像管理ViewModel
 */
class AvatarViewModel(application: Application) : AndroidViewModel(application) {

    private val avatarApiService = RetrofitClient.avatarApiService
    private val userPreferences = UserPreferences.getInstance(application)

    // 上传状态
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    // 上传进度 (0-100)
    private val _uploadProgress = MutableStateFlow(0)
    val uploadProgress: StateFlow<Int> = _uploadProgress.asStateFlow()

    // 错误消息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // 成功消息
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    /**
     * 上传头像
     *
     * @param userId 用户ID
     * @param imageUri 图片URI
     */
    fun uploadAvatar(userId: String, imageUri: Uri) {
        viewModelScope.launch {
            try {
                _isUploading.value = true
                _uploadProgress.value = 0
                _errorMessage.value = null

                // 1. 压缩图片
                _uploadProgress.value = 20
                val compressedFile = compressImage(imageUri)
                if (compressedFile == null) {
                    _errorMessage.value = "图片处理失败"
                    return@launch
                }

                // 2. 构建请求
                _uploadProgress.value = 40
                val requestFile = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", compressedFile.name, requestFile)
                val userIdBody = userId.toRequestBody("text/plain".toMediaTypeOrNull())

                // 3. 上传
                _uploadProgress.value = 60
                val response = avatarApiService.uploadAvatar(userIdBody, filePart)

                _uploadProgress.value = 90

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.code == 200) {
                        val avatarUrl = apiResponse.data?.get("avatarUrl")

                        // 更新本地存储
                        if (avatarUrl != null) {
                            val currentUserInfo = userPreferences.getUserInfo()
                            Log.d("AvatarViewModel", "上传前的avatar: ${currentUserInfo?.avatar}")
                            if (currentUserInfo != null) {
                                val updatedUserInfo = currentUserInfo.copy(avatar = avatarUrl)
                                userPreferences.updateUserInfo(updatedUserInfo)
                                Log.d("AvatarViewModel", "本地用户信息已更新，新avatar: $avatarUrl")

                                // 验证更新是否成功
                                val verifyUserInfo = userPreferences.getUserInfo()
                                Log.d("AvatarViewModel", "验证更新后的avatar: ${verifyUserInfo?.avatar}")
                            }
                        }

                        _successMessage.value = "头像上传成功"
                        _uploadProgress.value = 100
                        Log.d("AvatarViewModel", "头像上传成功: $avatarUrl")
                    } else {
                        _errorMessage.value = apiResponse?.message ?: "上传失败"
                    }
                } else {
                    _errorMessage.value = "网络请求失败：${response.code()}"
                }

                // 删除临时文件
                compressedFile.delete()

            } catch (e: Exception) {
                Log.e("AvatarViewModel", "上传头像失败", e)
                _errorMessage.value = "上传失败：${e.message}"
            } finally {
                _isUploading.value = false
            }
        }
    }

    /**
     * 压缩图片
     * 将图片压缩到500KB以内，最大尺寸1080x1080
     *
     * @param imageUri 原始图片URI
     * @return 压缩后的文件
     */
    private suspend fun compressImage(imageUri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val context = getApplication<Application>().applicationContext
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext null

            // 读取原始图片
            var bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            // 计算缩放比例
            val maxSize = 1080
            val scale = if (bitmap.width > bitmap.height) {
                if (bitmap.width > maxSize) maxSize.toFloat() / bitmap.width else 1f
            } else {
                if (bitmap.height > maxSize) maxSize.toFloat() / bitmap.height else 1f
            }

            // 缩放图片
            if (scale < 1f) {
                val newWidth = (bitmap.width * scale).toInt()
                val newHeight = (bitmap.height * scale).toInt()
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            }

            // 压缩到指定大小
            var quality = 90
            val maxSizeKB = 500 * 1024 // 500KB
            var compressedData: ByteArray

            do {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                compressedData = outputStream.toByteArray()
                outputStream.close()

                if (compressedData.size > maxSizeKB && quality > 10) {
                    quality -= 10
                } else {
                    break
                }
            } while (true)

            Log.d("AvatarViewModel", "图片压缩完成: ${compressedData.size / 1024}KB, quality=$quality")

            // 保存到临时文件
            val tempFile = File(context.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
            val fileOutputStream = FileOutputStream(tempFile)
            fileOutputStream.write(compressedData)
            fileOutputStream.close()

            bitmap.recycle()

            tempFile

        } catch (e: Exception) {
            Log.e("AvatarViewModel", "压缩图片失败", e)
            null
        }
    }

    /**
     * 删除头像（恢复默认头像）
     *
     * @param userId 用户ID
     */
    fun deleteAvatar(userId: String) {
        viewModelScope.launch {
            try {
                _isUploading.value = true
                _errorMessage.value = null

                val response = avatarApiService.deleteAvatar(userId)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.code == 200) {
                        _successMessage.value = "已恢复默认头像"
                        Log.d("AvatarViewModel", "头像删除成功")
                    } else {
                        _errorMessage.value = apiResponse?.message ?: "删除失败"
                    }
                } else {
                    _errorMessage.value = "网络请求失败：${response.code()}"
                }

            } catch (e: Exception) {
                Log.e("AvatarViewModel", "删除头像失败", e)
                _errorMessage.value = "删除失败：${e.message}"
            } finally {
                _isUploading.value = false
            }
        }
    }

    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * 清除成功消息
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}
