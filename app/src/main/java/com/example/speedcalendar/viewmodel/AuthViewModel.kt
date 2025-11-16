package com.example.speedcalendar.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.speedcalendar.data.api.RetrofitClient
import com.example.speedcalendar.data.local.UserPreferences
import com.example.speedcalendar.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 认证ViewModel
 * 管理用户登录、验证码发送等业务逻辑
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authApiService = RetrofitClient.authApiService
    private val userPreferences = UserPreferences.getInstance(application)

    // 用户信息
    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo.asStateFlow()

    // 登录状态
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 错误消息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // 成功消息
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // 验证码倒计时
    private val _countdown = MutableStateFlow(0)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    init {
        // 初始化时检查登录状态
        checkLoginStatus()
    }

    /**
     * 检查登录状态
     */
    private fun checkLoginStatus() {
        _isLoggedIn.value = userPreferences.isLoggedIn()
        _userInfo.value = userPreferences.getUserInfo()
    }

    /**
     * 发送验证码
     */
    fun sendVerificationCode(phone: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                // 验证手机号格式
                if (!isValidPhone(phone)) {
                    _errorMessage.value = "手机号格式不正确"
                    return@launch
                }

                // 调用API
                val request = SendCodeRequest(phone)
                val response = authApiService.sendCode(request)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.code == 200) {
                        _successMessage.value = apiResponse.message
                        startCountdown()
                    } else {
                        _errorMessage.value = apiResponse?.message ?: "发送验证码失败"
                    }
                } else {
                    _errorMessage.value = "网络请求失败：${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "发送验证码失败", e)
                _errorMessage.value = "网络连接失败：${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 手机号登录
     */
    fun phoneLogin(phone: String, code: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                // 验证输入
                if (!isValidPhone(phone)) {
                    _errorMessage.value = "手机号格式不正确"
                    return@launch
                }

                if (code.length != 6) {
                    _errorMessage.value = "验证码必须为6位"
                    return@launch
                }

                // 调用API
                val request = PhoneLoginRequest(phone, code)
                val response = authApiService.phoneLogin(request)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.code == 200) {
                        val loginResponse = apiResponse.data
                        if (loginResponse != null) {
                            // 保存登录信息
                            userPreferences.saveLoginInfo(loginResponse)

                            // 更新状态
                            _userInfo.value = loginResponse.userInfo
                            _isLoggedIn.value = true
                            _successMessage.value = "登录成功"

                            Log.d("AuthViewModel", "登录成功，userId: ${loginResponse.userId}")
                        } else {
                            _errorMessage.value = "登录响应数据为空"
                        }
                    } else {
                        _errorMessage.value = apiResponse?.message ?: "登录失败"
                    }
                } else {
                    _errorMessage.value = "网络请求失败：${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "登录失败", e)
                _errorMessage.value = "网络连接失败：${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 登出
     */
    fun logout() {
        userPreferences.clearLoginInfo()
        _userInfo.value = null
        _isLoggedIn.value = false
        _successMessage.value = "已退出登录"
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

    /**
     * 验证手机号格式
     */
    private fun isValidPhone(phone: String): Boolean {
        return phone.matches(Regex("^1[3-9]\\d{9}$"))
    }

    /**
     * 开始验证码倒计时（60秒）
     */
    private fun startCountdown() {
        viewModelScope.launch {
            _countdown.value = 60
            while (_countdown.value > 0) {
                kotlinx.coroutines.delay(1000)
                _countdown.value -= 1
            }
        }
    }
}
