# 后端开发文档：手机号验证码登录功能

## 1. 功能概述

本次需求旨在实现一个标准的“手机号 + 验证码”登录流程。该流程分为两步：

1.  **请求验证码**：用户输入手机号，请求系统发送一个有时效性的验证码。
2.  **登录/注册**：用户使用手机号和收到的验证码进行登录。如果该手机号是首次登录，系统将自动为其创建一个新账户。

## 2. 数据库设计

为了支持此功能，我们至少需要一张核心的用户表。

### `users` 表 (用户表)

这张表用于存储所有用户的基本信息。

**SQL Schema:**

```sql
CREATE TABLE `users` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户唯一ID',
  `phone` VARCHAR(20) NOT NULL COMMENT '手机号，作为主要登录凭据',
  `username` VARCHAR(50) NULL COMMENT '用户名，可后续设置',
  `avatar` VARCHAR(255) NULL COMMENT '用户头像URL',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '账户创建时间',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '账户最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```

**字段说明:**

- `id`: 用户唯一标识，主键。
- `phone`: **核心字段**，用户的手机号，必须唯一。
- `username`: 用户名，允许初始为空。
- `avatar`: 用户头像的链接，允许为空。
- `created_at` / `updated_at`: 标准的时间戳字段。

## 3. 核心技术与依赖

-   **验证码存储**: 建议使用 **Redis** 来存储验证码。因为验证码是临时性数据，使用 Redis 性能更高，且能方便地设置过期时间。
-   **短信服务**: 需要集成一个第三方的短信网关服务（如阿里云、腾讯云等），用于向用户手机发送验证码。
-   **认证机制**: 登录成功后，应生成 **JWT (JSON Web Token)** 作为令牌（Token）返回给前端，用于后续接口的身份认证。

## 4. API 接口设计

### 4.1 发送验证码接口

-   **功能描述**: 向指定手机号发送一个6位数字的验证码。
-   **URL**: `/code`
-   **Method**: `POST`
-   **Content-Type**: `application/json`

**请求体 (Request Body):**

```json
{
  "phone": "13812345678"
}
```

**后端处理逻辑:**

1.  **参数校验**：检查 `phone` 字段是否存在且格式是否为合法的11位手机号。如果格式错误，返回 `400 Bad Request`。
2.  **发送频率限制**：检查该手机号在短时间内（例如60秒内）是否已请求过验证码，以防短信轰炸。如果请求过于频繁，返回 `429 Too Many Requests`。
3.  **生成验证码**：生成一个随机的6位数字验证码（例如 `123456`）。
4.  **存储验证码**：将验证码存入 Redis，`key` 可以是 `verification_code:13812345678`，`value` 为验证码本身，并设置过期时间（例如 `5分钟`）。
    -   *Redis 命令示例*: `SET verification_code:13812345678 "123456" EX 300`
5.  **发送短信**：调用第三方短信服务接口，将生成的验证码发送给用户。
6.  **返回响应**：如果短信发送成功，向前端返回成功响应。

**响应 (Response):**

-   **成功 (200 OK):**
    ```json
    {
      "code": 200,
      "message": "验证码已发送，请注意查收",
      "data": null
    }
    ```
-   **失败 (400 Bad Request):**
    ```json
    {
      "code": 400,
      "message": "手机号格式不正确",
      "data": null
    }
    ```

### 4.2 手机号登录/注册接口

-   **功能描述**: 使用手机号和验证码进行登录或自动注册。
-   **URL**: `/login/phone`
-   **Method**: `POST`
-   **Content-Type**: `application/json`

**请求体 (Request Body):**

```json
{
  "phone": "13812345678",
  "code": "123456"
}
```

**后端处理逻辑:**

1.  **参数校验**：检查 `phone` 和 `code` 字段是否都存在。
2.  **验证码校验**：
    a. 从 Redis 中根据手机号 (`verification_code:13812345678`) 读取正确的验证码。
    b. 对比用户传入的 `code` 和 Redis 中存储的 `code` 是否一致。
    c. 如果不一致或 Redis 中已过期/不存在，返回 `401 Unauthorized`。
3.  **验证码失效**：**无论登录是否成功，一旦验证通过，立即从 Redis 中删除该验证码**，防止重复使用。
    -   *Redis 命令示例*: `DEL verification_code:13812345678`
4.  **查找或创建用户**：
    a. 使用 `phone` 字段查询 `users` 表。
    b. **如果用户不存在**：在 `users` 表中插入一条新记录，创建一个新用户。
    c. **如果用户已存在**：获取该用户的 `id` 等信息。
5.  **生成 Token**：使用用户的 `id` 作为核心载荷 (Payload)，生成一个 JWT。
6.  **返回响应**：将生成的 Token 和用户基本信息返回给前端。

**响应 (Response):**

-   **成功 (200 OK):**
    ```json
    {
      "code": 200,
      "message": "登录成功",
      "data": {
        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "user": {
          "id": 1001,
          "phone": "13812345678",
          "username": null,
          "avatar": null
        }
      }
    }
    ```
-   **失败 (401 Unauthorized):**
    ```json
    {
      "code": 401,
      "message": "验证码错误或已失效",
      "data": null
    }
    ```
