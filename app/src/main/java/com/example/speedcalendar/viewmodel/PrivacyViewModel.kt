package com.example.speedcalendar.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.speedcalendar.data.api.RetrofitClient
import com.example.speedcalendar.data.model.PrivacySettingDTO
import com.example.speedcalendar.data.model.UpdatePrivacySettingsRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 隐私设置ViewModel
 *
 * TODO: 添加本地缓存机制
 * 当前问题：
 * 1. 没有本地缓存：每次打开页面都需要从后端获取，浪费流量和时间
 * 2. 离线不可用：没有网络时无法查看当前隐私设置
 * 3. 加载延迟：每次进入页面都有等待时间
 *
 * 改进建议：
 * 方案1：使用DataStore缓存
 *   - 保存最后一次获取的隐私设置
 *   - 进入页面时先显示缓存，再后台刷新
 *   - 提升用户体验
 *
 * 方案2：内存缓存 + 持久化
 *   - 内存中保持一份缓存（应用内生命周期）
 *   - DataStore保存持久化数据（跨应用启动）
 *   - 添加缓存时间戳，定期刷新
 *
 * 方案3：优化网络请求
 *   - 使用缓存-网络策略（Cache-then-Network）
 *   - 先展示缓存，同时发起请求
 *   - 请求成功后更新UI和缓存
 *
 * 示例实现：
 * ```kotlin
 * // 使用DataStore保存
 * private val dataStore = context.dataStore
 *
 * fun loadPrivacySettings(userId: String, forceRefresh: Boolean = false) {
 *     viewModelScope.launch {
 *         if (!forceRefresh) {
 *             // 先读取缓存
 *             val cached = dataStore.data.first()[PRIVACY_SETTINGS_KEY]
 *             if (cached != null) {
 *                 _privacySettings.value = Json.decodeFromString(cached)
 *             }
 *         }
 *
 *         // 再从网络获取
 *         val response = privacyApiService.getPrivacySettings(userId)
 *         if (response.isSuccessful) {
 *             val settings = response.body()?.data
 *             _privacySettings.value = settings
 *             // 更新缓存
 *             dataStore.edit { it[PRIVACY_SETTINGS_KEY] = Json.encodeToString(settings) }
 *         }
 *     }
 * }
 * ```
 *
 * 注意事项：
 * - 隐私设置相对稳定，缓存时间可以设置较长（如24小时）
 * - 用户修改后立即更新缓存
 * - 提供手动刷新选项
 */
class PrivacyViewModel(application: Application) : AndroidViewModel(application) {

    private val privacyApiService = RetrofitClient.privacyApiService
    // TODO: 添加DataStore或SharedPreferences用于本地缓存
    // private val dataStore = ...

    // 隐私设置列表
    private val _privacySettings = MutableStateFlow<List<PrivacySettingDTO>>(emptyList())
    val privacySettings: StateFlow<List<PrivacySettingDTO>> = _privacySettings.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 错误消息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // 成功消息
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    /**
     * 获取隐私设置
     */
    fun loadPrivacySettings(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val response = privacyApiService.getPrivacySettings(userId)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.code == 200) {
                        _privacySettings.value = apiResponse.data ?: emptyList()
                        Log.d("PrivacyViewModel", "隐私设置加载成功")
                    } else {
                        _errorMessage.value = apiResponse?.message ?: "获取隐私设置失败"
                    }
                } else {
                    _errorMessage.value = "网络请求失败：${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("PrivacyViewModel", "获取隐私设置失败", e)
                _errorMessage.value = "网络连接失败：${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 更新隐私设置
     */
    fun updatePrivacySettings(userId: String, settings: List<PrivacySettingDTO>) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val request = UpdatePrivacySettingsRequest(settings)
                val response = privacyApiService.updatePrivacySettings(userId, request)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.code == 200) {
                        _successMessage.value = "隐私设置更新成功"
                        // 更新本地状态
                        _privacySettings.value = settings
                        Log.d("PrivacyViewModel", "隐私设置更新成功")
                    } else {
                        _errorMessage.value = apiResponse?.message ?: "更新隐私设置失败"
                    }
                } else {
                    _errorMessage.value = "网络请求失败：${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("PrivacyViewModel", "更新隐私设置失败", e)
                _errorMessage.value = "网络连接失败：${e.message}"
            } finally {
                _isLoading.value = false
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
