# **SpeedCalendar 后端API文档**

## **1. 认证 (Authentication)**

### **1.1 用户注册**

*   **URL**: `/api/auth/register`
*   **Method**: `POST`
*   **Description**: 注册一个新用户。
*   **Body**:
    ```json
    {
      "username": "string",
      "email": "string",
      "password": "string"
    }
    ```
*   **Response**:
    *   `201 Created`: 注册成功。
        ```json
        {
          "message": "User registered successfully"
        }
        ```
    *   `400 Bad Request`: 输入无效。
    *   `409 Conflict`: 用户名或邮箱已存在。

### **1.2 用户登录**

*   **URL**: `/api/auth/login`
*   **Method**: `POST`
*   **Description**: 用户登录并获取JWT。
*   **Body**:
    ```json
    {
      "email": "string",
      "password": "string"
    }
    ```
*   **Response**:
    *   `200 OK`: 登录成功。
        ```json
        {
          "token": "string (JWT)",
          "user": {
            "userId": "string",
            "username": "string",
            "email": "string",
            "avatarUrl": "string"
          }
        }
        ```
    *   `401 Unauthorized`: 凭证无效。

## **2. 日程 (Schedules)**

### **2.1 获取指定月份的日程**

*   **URL**: `/api/schedules`
*   **Method**: `GET`
*   **Description**: 获取指定用户在某年某月的所有日程。
*   **Headers**:
    *   `Authorization`: `Bearer <JWT>`
*   **Query Parameters**:
    *   `year`: `integer` (e.g., 2024)
    *   `month`: `integer` (e.g., 7)
*   **Response**:
    *   `200 OK`:
        ```json
        [
          {
            "scheduleId": "string",
            "userId": "string",
            "title": "string",
            "scheduleDate": "string (YYYY-MM-DD)",
            "startTime": "string (HH:mm)",
            "endTime": "string (HH:mm)",
            "location": "string",
            "isAllDay": "boolean",
            "createdAt": "long (timestamp)"
          }
        ]
        ```

### **2.2 新增日程**

*   **URL**: `/api/schedules`
*   **Method**: `POST`
*   **Description**: 为当前用户创建一个新的日程。
*   **Headers**:
    *   `Authorization`: `Bearer <JWT>`
*   **Body**:
    ```json
    {
      "title": "string",
      "scheduleDate": "string (YYYY-MM-DD)",
      "startTime": "string (HH:mm)",
      "endTime": "string (HH:mm)",
      "location": "string",
      "isAllDay": "boolean"
    }
    ```
*   **Response**:
    *   `201 Created`:
        ```json
        {
          "scheduleId": "string",
          "userId": "string",
          "title": "string",
          "scheduleDate": "string (YYYY-MM-DD)",
          "startTime": "string (HH:mm)",
          "endTime": "string (HH:mm)",
          "location": "string",
          "isAllDay": "boolean",
          "createdAt": "long (timestamp)"
        }
        ```

### **2.3 更新日程**

*   **URL**: `/api/schedules/{scheduleId}`
*   **Method**: `PUT`
*   **Description**: 更新一个已存在的日程。
*   **Headers**:
    *   `Authorization`: `Bearer <JWT>`
*   **Path Parameters**:
    *   `scheduleId`: `string`
*   **Body**:
    ```json
    {
      "title": "string",
      "scheduleDate": "string (YYYY-MM-DD)",
      "startTime": "string (HH:mm)",
      "endTime": "string (HH:mm)",
      "location": "string",
      "isAllDay": "boolean"
    }
    ```
*   **Response**:
    *   `200 OK`:
        ```json
        {
          "scheduleId": "string",
          // ... (updated schedule object)
        }
        ```
    *   `404 Not Found`: 日程不存在。

### **2.4 删除日程**

*   **URL**: `/api/schedules/{scheduleId}`
*   **Method**: `DELETE`
*   **Description**: 删除一个日程。
*   **Headers**:
    *   `Authorization`: `Bearer <JWT>`
*   **Path Parameters**:
    *   `scheduleId`: `string`
*   **Response**:
    *   `204 No Content`: 删除成功。
    *   `404 Not Found`: 日程不存在。

## **3. AI 聊天 (AI Chat)**

### **3.1 获取聊天会话列表**

*   **URL**: `/api/ai/chat/sessions`
*   **Method**: `GET`
*   **Description**: 获取指定用户的所有聊天会话。
*   **Headers**:
    *   `Authorization`: `Bearer <JWT>`
*   **Query Parameters**:
    *   `userId`: `string`
*   **Response**:
    *   `200 OK`:
        ```json
        [
          {
            "id": "string",
            "title": "string",
            "lastMessage": "string",
            "timestamp": "long"
          }
        ]
        ```

### **3.2 发送消息**

*   **URL**: `/api/ai/chat/message`
*   **Method**: `POST`
*   **Description**: 发送一条消息到指定的会话。如果 `sessionId` 为空，则创建一个新会话。
*   **Headers**:
    *   `Authorization`: `Bearer <JWT>`
*   **Body**:
    ```json
    {
      "message": "string",
      "sessionId": "string",
      "userId": "string"
    }
    ```
*   **Response**:
    *   `200 OK`:
        ```json
        {
          "sessionId": "string",
          "message": "string",
          "timestamp": "long"
        }
        ```

### **3.3 获取聊天记录**

*   **URL**: `/api/ai/chat/history/{sessionId}`
*   **Method**: `GET`
*   **Description**: 获取指定会话的所有聊天记录。
*   **Headers**:
    *   `Authorization`: `Bearer <JWT>`
*   **Path Parameters**:
    *   `sessionId`: `string`
*   **Response**:
    *   `200 OK`:
        ```json
        {
          "messages": [
            {
              "id": "string",
              "content": "string",
              "role": "string (user/ai)",
              "timestamp": "long"
            }
          ]
        }
        ```

### **3.4 创建新会话**

*   **URL**: `/api/ai/chat/sessions`
*   **Method**: `POST`
*   **Description**: 创建一个新的聊天会话。
*   **Headers**:
    *   `Authorization`: `Bearer <JWT>`
*   **Body**:
    ```json
    {
      "userId": "string"
    }
    ```
*   **Response**:
    *   `201 Created`:
        ```json
        {
          "id": "string",
          "title": "string",
          "lastMessage": "",
          "timestamp": "long"
        }
        ```

## **4. 用户 (Users)**

### **4.1 获取用户信息**

*   **URL**: `/api/users/{userId}`
*   **Method**: `GET`
*   **Description**: 获取指定用户的详细信息。
*   **Headers**:
    *   `Authorization`: `Bearer <JWT>`
*   **Path Parameters**:
    *   `userId`: `string`
*   **Response**:
    *   `200 OK`:
        ```json
        {
          "userId": "string",
          "username": "string",
          "email": "string",
          "avatarUrl": "string"
        }
        ```
    *   `404 Not Found`: 用户不存在。

### **4.2 更新用户信息**

*   **URL**: `/api/users/{userId}`
*   **Method**: `PUT`
*   **Description**: 更新用户的个人资料信息。
*   **Headers**:
    *   `Authorization`: `Bearer <JWT>`
*   **Path Parameters**:
    *   `userId`: `string`
*   **Body**:
    ```json
    {
      "username": "string"
    }
    ```
*   **Response**:
    *   `200 OK`:
        ```json
        {
          "userId": "string",
          "username": "string",
          "email": "string",
          "avatarUrl": "string"
        }
        ```
    *   `404 Not Found`: 用户不存在。

### **4.3 上传头像**

*   **URL**: `/api/users/{userId}/avatar`
*   **Method**: `POST`
*   **Description**: 上传用户的头像图片。
*   **Headers**:
    *   `Authorization`: `Bearer <JWT>`
*   **Path Parameters**:
    *   `userId`: `string`
*   **Body**:
    *   `multipart/form-data` with a file part named "avatar".
*   **Response**:
    *   `200 OK`:
        ```json
        {
          "avatarUrl": "string"
        }
        ```
    *   `400 Bad Request`: 无效的文件。
    *   `404 Not Found`: 用户不存在。
