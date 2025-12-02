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

        // TODO: 数据同步问题 - 多设备和实时性改进
        // 当前问题：
        // 1. 多设备同步：设备A修改数据后，设备B不会自动更新，只有重新登录才会刷新
        // 2. 应用启动：从本地存储读取，不会从后端获取最新数据，可能显示过期信息
        // 3. 无推送机制：其他设备或后台更新数据后，当前设备无法感知
        //
        // 改进方案（按优先级）：
        // 方案1（推荐开发阶段）：定期刷新
        //   - 应用启动时调用后端API获取最新用户信息
        //   - 从后台切换到前台时刷新
        //   - 进入个人资料页面时刷新
        //
        // 方案2：缓存过期机制
        //   - UserPreferences中保存数据时间戳
        //   - 读取时检查是否过期（如1小时）
        //   - 过期则自动从后端刷新
        //
        // 方案3：版本号机制
        //   - 后端返回数据版本号
        //   - 前端每次请求时对比版本号
        //   - 不一致则拉取最新数据
        //
        // 方案4（生产环境推荐）：WebSocket推送
        //   - 后端数据变化时通过WebSocket推送
        //   - 前端接收通知后自动刷新
        //   - 实现真正的实时同步
        //
        // 方案5：用户手动刷新
        //   - 个人资料页面添加下拉刷新
        //   - 用户手动触发数据更新
    }

    /**
     * 检查登录状态
     */
    private fun checkLoginStatus() {
        val token = userPreferences.getAccessToken()
        val loggedIn = userPreferences.isLoggedIn()
        val info = userPreferences.getUserInfo()
        
        Log.d("AuthViewModel", "checkLoginStatus: token=${if (token != null) "存在(${token.take(20)}...)" else "null"}")
        Log.d("AuthViewModel", "checkLoginStatus: isLoggedIn=$loggedIn, userInfo=${if (info != null) "存在" else "null"}")
        
        _isLoggedIn.value = loggedIn
        _userInfo.value = info
    }

    /**
     * 刷新用户信息
     * 从本地存储重新加载用户信息
     */
    fun refreshUserInfo() {
        val newUserInfo = userPreferences.getUserInfo()
        Log.d("AuthViewModel", "刷新用户信息: avatar=${newUserInfo?.avatar}")
        _userInfo.value = newUserInfo
    }

    /**
     * 发送验证码
     */
    /* TODO: 暂时注释掉验证码登录功能
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
    */

    /**
     * 手机号登录
     */
    /* TODO: 暂时注释掉验证码登录功能
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
    */

    /**
     * 账号密码登录
     */
    fun login(phone: String, password: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                if (!isValidPhone(phone)) {
                    _errorMessage.value = "手机号格式不正确"
                    return@launch
                }

                if (password.isEmpty()) {
                    _errorMessage.value = "请输入密码"
                    return@launch
                }

                val request = PasswordLoginRequest(phone, password)
                val response = authApiService.login(request)

                handleLoginResponse(response)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "登录失败", e)
                _errorMessage.value = "网络连接失败：${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 注册
     */
    fun register(phone: String, password: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                if (!isValidPhone(phone)) {
                    _errorMessage.value = "手机号格式不正确"
                    return@launch
                }

                if (password.length < 6) {
                    _errorMessage.value = "密码长度至少6位"
                    return@launch
                }

                val request = RegisterRequest(phone, password)
                val response = authApiService.register(request)

                handleLoginResponse(response)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "注册失败", e)
                _errorMessage.value = "网络连接失败：${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun handleLoginResponse(response: retrofit2.Response<ApiResponse<LoginResponse>>) {
        if (response.isSuccessful) {
            val apiResponse = response.body()
            if (apiResponse != null && apiResponse.code == 200) {
                val loginResponse = apiResponse.data
                if (loginResponse != null) {
                    userPreferences.saveLoginInfo(loginResponse)
                    _userInfo.value = loginResponse.userInfo
                    _isLoggedIn.value = true
                    _successMessage.value = "操作成功"
                } else {
                    _errorMessage.value = "响应数据为空"
                }
            } else {
                _errorMessage.value = apiResponse?.message ?: "操作失败"
            }
        } else {
            _errorMessage.value = "网络请求失败：${response.code()}"
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
     * 更新用户信息
     */
    fun updateUserInfo(userId: String, username: String? = null, avatar: String? = null) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                // 创建更新请求
                val request = UpdateUserInfoRequest(
                    username = username,
                    avatar = avatar
                )

                // 调用API
                val response = authApiService.updateUserInfo(userId, request)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.code == 200) {
                        val updatedUserInfo = apiResponse.data
                        if (updatedUserInfo != null) {
                            // 更新本地状态
                            _userInfo.value = updatedUserInfo

                            // 更新本地存储
                            userPreferences.updateUserInfo(updatedUserInfo)

                            _successMessage.value = "更新成功"
                            Log.d("AuthViewModel", "用户信息更新成功")
                        } else {
                            _errorMessage.value = "更新响应数据为空"
                        }
                    } else {
                        _errorMessage.value = apiResponse?.message ?: "更新失败"
                    }
                } else {
                    _errorMessage.value = "网络请求失败：${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "更新用户信息失败", e)
                _errorMessage.value = "网络连接失败：${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
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
