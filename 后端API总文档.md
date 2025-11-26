# SpeedCalendar 后端 API 总文档

**版本: 1.0**

## 1. 概述

本文档是 SpeedCalendar App 所有后端 API 的权威参考。所有接口都遵循统一的结构和规范。

### 1.1. 基础信息

- **根路径 (Base URL)**: `http://10.0.2.2:8080/api` (此为 Android 模拟器访问地址)
- **认证方式**: 所有需要授权的接口，都需要在 HTTP Header 中传递 `Authorization: Bearer <token>`。

### 1.2. 统一响应结构

所有 API 的响应都遵循以下 JSON 格式：

```json
{
  "code": 200,
  "message": "响应消息",
  "data": { ... } // 或 [ ... ]
}
```

| 字段 | 类型 | 描述 |
| :--- | :--- | :--- |
| `code` | Integer | 业务状态码，`200` 或 `201` 表示成功 |
| `message`| String | 对当前响应的文字描述 |
| `data` | Object/Array | 实际的响应数据 |

### 1.3. HTTP 状态码

- `200 OK`: 请求成功。
- `201 Created`: 资源创建成功 (例如，添加新日程)。
- `204 No Content`: 操作成功，但响应体中无内容 (例如，删除成功)。
- `400 Bad Request`: 客户端请求参数错误。
- `401 Unauthorized`: 未提供或提供了无效的认证 Token。
- `404 Not Found`: 请求的资源不存在。
- `500 Internal Server Error`: 服务器内部发生未知错误。

---

## 2. 用户认证模块 (`/auth`)

此模块负责用户的登录和注册。

### 2.1. 发送短信验证码

- **路径**: `/auth/send-code`
- **方法**: `POST`

**请求体:**
```json
{
  "phone": "13800138000"
}
```

**成功响应 (200 OK):**
```json
{
  "code": 200,
  "message": "验证码已发送，请注意查收",
  "data": null
}
```

### 2.2. 手机号验证码登录

- **路径**: `/auth/login/phone`
- **方法**: `POST`

**请求体:**
```json
{
  "phone": "13800138000",
  "code": "123456"
}
```

**成功响应 (200 OK):**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": { ... } // 包含 token 和 userInfo 的对象
}
```

---

## 3. 日程管理模块 (`/schedules`)

此模块负责所有与用户日程相关的操作。

### 3.1. 添加新日程

- **路径**: `/schedules`
- **方法**: `POST`
- **认证**: 需要

**成功响应 (201 Created):**
```json
{
  "code": 201,
  "message": "日程创建成功",
  "data": { ... } // 返回完整的日程对象
}
```

### 3.2. 获取指定月份的日程列表

- **路径**: `/schedules/user/{userId}`
- **方法**: `GET`
- **认证**: 需要

**请求示例:** `GET /schedules/user/user_12345?month=2024-09`

**成功响应 (200 OK):**
```json
{
  "code": 200,
  "message": "获取成功",
  "data": [ ... ] // 日程对象数组
}
```

### 3.3. 更新日程

- **路径**: `/schedules/{scheduleId}`
- **方法**: `PUT`
- **认证**: 需要

**请求体:** 包含一个或多个要更新的字段的日程对象。

**成功响应 (200 OK):**
```json
{
  "code": 200,
  "message": "更新成功",
  "data": { ... } // 返回更新后的完整日程对象
}
```

### 3.4. 删除日程

- **路径**: `/schedules/{scheduleId}`
- **方法**: `DELETE`
- **认证**: 需要

**请求示例:** `DELETE /schedules/schedule-uuid-abc-123`

**成功响应 (204 No Content):** 服务器将返回一个没有内容的 `204` 响应。

---

## 4. AI 助手模块 (`/ai/chat`)

此模块负责与“极速精灵”AI 助手的所有交互。

(此模块的详细接口...)

