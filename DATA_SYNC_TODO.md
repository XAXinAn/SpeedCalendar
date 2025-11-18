# 数据同步和缓存改进 TODO 清单

本文档总结了项目中关于数据同步、本地缓存和实时更新的所有待改进项。

---

## 📋 TODO 位置索引

### 1. AuthViewModel.kt (第52-82行)
**文件路径：** `app/src/main/java/com/example/speedcalendar/viewmodel/AuthViewModel.kt`

**问题：** 多设备同步和实时性

**当前问题：**
- 多设备同步：设备A修改数据后，设备B不会自动更新，只有重新登录才会刷新
- 应用启动：从本地存储读取，不会从后端获取最新数据，可能显示过期信息
- 无推送机制：其他设备或后台更新数据后，当前设备无法感知

**改进方案（按优先级）：**

#### 方案1（推荐开发阶段）：定期刷新
- 应用启动时调用后端API获取最新用户信息
- 从后台切换到前台时刷新
- 进入个人资料页面时刷新

#### 方案2：缓存过期机制
- UserPreferences中保存数据时间戳
- 读取时检查是否过期（如1小时）
- 过期则自动从后端刷新

#### 方案3：版本号机制
- 后端返回数据版本号
- 前端每次请求时对比版本号
- 不一致则拉取最新数据

#### 方案4（生产环境推荐）：WebSocket推送
- 后端数据变化时通过WebSocket推送
- 前端接收通知后自动刷新
- 实现真正的实时同步

#### 方案5：用户手动刷新
- 个人资料页面添加下拉刷新
- 用户手动触发数据更新

---

### 2. UserPreferences.kt (第13-47行)
**文件路径：** `app/src/main/java/com/example/speedcalendar/data/local/UserPreferences.kt`

**问题：** 缓存过期机制

**当前问题：**
- 没有缓存时间戳：无法判断本地数据是否过期
- 没有过期检查：数据可能长期不更新，与后端不一致
- 没有版本控制：无法追踪数据更新历史

**改进建议：**

#### 方案1：添加时间戳字段
```kotlin
private const val KEY_USER_INFO_TIMESTAMP = "user_info_timestamp"
private const val CACHE_VALID_DURATION = 60 * 60 * 1000L // 1小时

fun getUserInfo(): UserInfo? {
    val timestamp = prefs.getLong(KEY_USER_INFO_TIMESTAMP, 0)
    if (System.currentTimeMillis() - timestamp > CACHE_VALID_DURATION) {
        return null // 缓存过期，需要重新获取
    }
    val json = prefs.getString(KEY_USER_INFO, null)
    return json?.let { gson.fromJson(it, UserInfo::class.java) }
}
```

#### 方案2：添加数据版本号
- 新增 KEY_USER_INFO_VERSION 字段
- 后端API返回version字段
- 每次请求时对比版本，不一致则刷新

#### 方案3：缓存策略配置
- 支持配置缓存时长（用户设置、隐私设置等不同时长）
- 重要数据短缓存，不常变数据长缓存

**需要添加的常量：**
```kotlin
// TODO: 添加这些字段到 companion object
private const val KEY_USER_INFO_TIMESTAMP = "user_info_timestamp"
private const val KEY_USER_INFO_VERSION = "user_info_version"
```

---

### 3. EditProfileScreen.kt (第40-75行)
**文件路径：** `app/src/main/java/com/example/speedcalendar/features/mine/settings/EditProfileScreen.kt`

**问题：** 页面数据刷新机制

**当前问题：**
- 页面打开时不刷新：只显示本地缓存数据，可能过期
- 无手动刷新：用户无法主动拉取最新数据
- 多设备不同步：其他设备修改后，当前设备看不到更新

**改进建议：**

#### 方案1：进入页面时自动刷新
```kotlin
LaunchedEffect(Unit) {
    userInfo?.userId?.let { userId ->
        viewModel.fetchUserInfoFromServer(userId) // 需要添加此方法
    }
}
```

#### 方案2：下拉刷新
```kotlin
val pullRefreshState = rememberPullRefreshState(
    refreshing = isRefreshing,
    onRefresh = {
        userInfo?.userId?.let { viewModel.fetchUserInfoFromServer(it) }
    }
)
Box(Modifier.pullRefresh(pullRefreshState)) {
    // 现有内容
    PullRefreshIndicator(isRefreshing, pullRefreshState)
}
```

#### 方案3：定时刷新
- 每次从后台切到前台时刷新
- 使用 Lifecycle 监听应用状态

---

### 4. PrivacyViewModel.kt (第18-71行)
**文件路径：** `app/src/main/java/com/example/speedcalendar/viewmodel/PrivacyViewModel.kt`

**问题：** 隐私设置没有本地缓存

**当前问题：**
- 没有本地缓存：每次打开页面都需要从后端获取，浪费流量和时间
- 离线不可用：没有网络时无法查看当前隐私设置
- 加载延迟：每次进入页面都有等待时间

**改进建议：**

#### 方案1：使用DataStore缓存
- 保存最后一次获取的隐私设置
- 进入页面时先显示缓存，再后台刷新
- 提升用户体验

#### 方案2：内存缓存 + 持久化
- 内存中保持一份缓存（应用内生命周期）
- DataStore保存持久化数据（跨应用启动）
- 添加缓存时间戳，定期刷新

#### 方案3：优化网络请求
- 使用缓存-网络策略（Cache-then-Network）
- 先展示缓存，同时发起请求
- 请求成功后更新UI和缓存

**示例实现：**
```kotlin
// 使用DataStore保存
private val dataStore = context.dataStore

fun loadPrivacySettings(userId: String, forceRefresh: Boolean = false) {
    viewModelScope.launch {
        if (!forceRefresh) {
            // 先读取缓存
            val cached = dataStore.data.first()[PRIVACY_SETTINGS_KEY]
            if (cached != null) {
                _privacySettings.value = Json.decodeFromString(cached)
            }
        }

        // 再从网络获取
        val response = privacyApiService.getPrivacySettings(userId)
        if (response.isSuccessful) {
            val settings = response.body()?.data
            _privacySettings.value = settings
            // 更新缓存
            dataStore.edit { it[PRIVACY_SETTINGS_KEY] = Json.encodeToString(settings) }
        }
    }
}
```

**注意事项：**
- 隐私设置相对稳定，缓存时间可以设置较长（如24小时）
- 用户修改后立即更新缓存
- 提供手动刷新选项

**需要添加：**
```kotlin
// TODO: 添加这个字段
private val dataStore = ...
```

---

### 5. MainScreen.kt (第42-74行)
**文件路径：** `app/src/main/java/com/example/speedcalendar/ui/MainScreen.kt`

**问题：** 应用启动和生命周期管理

**当前问题：**
- 冷启动只读缓存：应用启动时不从后端获取最新数据
- 后台返回不刷新：从后台切到前台时不更新数据
- 数据可能过期：长时间不使用后，本地数据与后端不一致

**改进建议：**

#### 方案1：应用启动时刷新
```kotlin
LaunchedEffect(Unit) {
    val lastUpdateTime = userPreferences.getLastUpdateTime()
    if (System.currentTimeMillis() - lastUpdateTime > REFRESH_THRESHOLD) {
        authViewModel.fetchUserInfoFromServer()
    }
}
```

#### 方案2：监听生命周期
```kotlin
val lifecycleOwner = LocalLifecycleOwner.current
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            authViewModel.refreshIfNeeded()
        }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
}
```

#### 方案3：智能刷新策略
- 短时间内（5分钟）返回前台：不刷新
- 长时间（1小时）返回前台：自动刷新
- 首次启动：强制刷新

**注意事项：**
- 避免频繁刷新浪费流量
- 显示刷新进度指示器
- 刷新失败时使用缓存数据

---

## 🔍 快速查找所有TODO

### 在IDE中搜索
使用全局搜索：`TODO.*数据同步|TODO.*缓存|TODO.*刷新`

### 使用命令行
```bash
cd SpeedCalendar/app
grep -rn "TODO.*数据同步\|TODO.*缓存\|TODO.*刷新" --include="*.kt"
```

---

## 📊 改进优先级建议

### 🔴 高优先级（开发阶段必须）
1. **UserPreferences 添加时间戳** - 防止数据长期不更新
2. **AuthViewModel 应用启动刷新** - 确保数据基本一致性
3. **EditProfileScreen 进入时刷新** - 提升用户体验

### 🟡 中优先级（生产环境推荐）
4. **PrivacyViewModel 添加缓存** - 减少网络请求
5. **MainScreen 生命周期监听** - 后台返回时刷新
6. **EditProfileScreen 下拉刷新** - 用户主动控制

### 🟢 低优先级（长期优化）
7. **WebSocket 推送机制** - 实时同步
8. **版本号控制** - 精确追踪更新
9. **智能刷新策略** - 平衡性能和实时性

---

## 🛠️ 实施步骤建议

### 第一阶段：基础缓存
1. UserPreferences 添加时间戳字段
2. AuthViewModel 添加 fetchUserInfoFromServer() 方法
3. 应用启动时检查缓存并刷新

### 第二阶段：页面刷新
4. EditProfileScreen 添加进入时刷新
5. PrivacyViewModel 添加本地缓存
6. 各页面添加下拉刷新

### 第三阶段：生命周期
7. MainScreen 添加生命周期监听
8. 实现智能刷新策略
9. 优化刷新频率和时机

### 第四阶段：高级功能
10. WebSocket 推送（生产环境）
11. 版本号控制
12. 离线模式优化

---

## 📝 开发注意事项

1. **测试场景**：
   - 多设备登录同一账号
   - 长时间后台后恢复
   - 无网络环境
   - 频繁前后台切换

2. **性能考虑**：
   - 避免频繁网络请求
   - 使用合理的缓存时长
   - 显示加载状态

3. **用户体验**：
   - 显示刷新进度
   - 失败时友好提示
   - 提供手动刷新选项

4. **数据一致性**：
   - 更新后立即刷新缓存
   - 处理并发更新冲突
   - 保证关键数据准确性

---

生成时间：2025-11-18
版本：v1.0
