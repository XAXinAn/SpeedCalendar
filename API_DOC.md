# **SpeedCalendar 后端 API 接口文档**

**基础路径:** `/api`

所有需要用户认证的接口，都需要在请求头 (Header) 中携带 `Authorization` 字段，值为 `Bearer <用户的JWT Token>`。

---

## **群组 (Group) 功能**

### 1. 新建群组

此接口用于用户创建一个新的群组，创建者自动成为该群组的第一个管理员。

-   **路径:** `/groups`
-   **方法:** `POST`
-   **认证:** 需要用户认证

**请求体 (Body):**
```json
{
  "name": "string"
}
```

**成功响应 (200 OK):**
```json
{
  "id": "string",
  "name": "string",
  "ownerId": "string",
  "invitationCode": "string" // 新增：群组的唯一邀请码
}
```

---

### 2. 加入群组 (通过邀请码)

此接口用于用户通过邀请码加入一个已经存在的群组。

-   **路径:** `/groups/join-with-code`
-   **方法:** `POST`
-   **认证:** 需要用户认证

**请求体 (Body):**
```json
{
  "invitationCode": "string"
}
```

**成功响应 (200 OK):**
```json
{
  "id": "string",
  "name": "string",
  "ownerId": "string",
  "invitationCode": "string"
}
```

**失败响应:**
- `404 Not Found`: 邀请码无效或对应的群组不存在。
- `409 Conflict`: 用户已经是该群组的成员。

---

### 3. 获取我加入的群组列表

此接口用于获取当前登录用户所属的所有群组列表。

-   **路径:** `/groups/my`
-   **方法:** `GET`
-   **认证:** 需要用户认证

**成功响应 (200 OK):**
```json
[
  {
    "groupId": "string",
    "groupName": "string",
    "role": "string" // "admin" or "member"
  }
]
```

---

### 4. 获取群组详情

此接口用于获取单个群组的详细信息。邀请码和删除权限将根据用户的角色在后端进行控制。

-   **路径:** `/groups/{groupId}`
-   **方法:** `GET`
-   **认证:** 需要用户认证

**成功响应 (200 OK):**
```json
{
  "id": "string",
  "name": "string",
  "ownerId": "string",
  "currentUserRole": "string", // 新增：当前用户在该群组的角色 ("admin" or "member")
  "invitationCode": "string", // 仅在 currentUserRole 为 "admin" 时返回
  "members": [
    {
      "userId": "string",
      "username": "string",
      "role": "string"
    }
  ]
}
```

**失败响应:**
- `403 Forbidden`: 用户不是该群组的成员。

---

## **日程 (Schedule) 功能 (修改)**

### 1. 获取日程列表

此接口用于获取在指定月份中，对当前用户可见的所有日程。

**可见范围:**
1.  用户为自己创建的**个人日程**。
2.  用户所在的所有**群组的日程**。

-   **路径:** `/schedules`
-   **方法:** `GET`
-   **认证:** 需要用户认证

**查询参数 (Query Parameters):**
- `year`: (integer, 必需) - 年份, e.g., `2023`
- `month`: (integer, 必需) - 月份, e.g., `11`

**成功响应 (200 OK):**
返回一个 `Schedule` 对象的数组。
```json
[
  {
    "scheduleId": "string",
    "userId": "string", // 创建者ID
    "title": "string",
    "scheduleDate": "string",
    "startTime": "string?",
    "endTime": "string?",
    "location": "string?",
    "isAllDay": "boolean",
    "createdAt": "long",
    "groupId": "string?" // 如果是群组日程，则为群组ID
  }
]
```

---

### 2. 添加日程

-   **路径:** `/schedules`
-   **方法:** `POST`

**请求体 (Body):**

在原有的 `AddScheduleRequest` 模型中，增加一个可为空的 `groupId` 字段。

```json
{
  "title": "string",
  "scheduleDate": "string",
  "startTime": "string?",
  "endTime": "string?",
  "location": "string?",
  "isAllDay": "boolean",
  "groupId": "string?" // 新增：关联的群组ID，个人日程则为null
}
```

---

### 3. 修改日程

-   **路径:** `/schedules/{scheduleId}`
-   **方法:** `PUT`

**请求体 (Body):**

与“添加日程”的请求体结构相同，同样包含可为空的 `groupId` 字段。
