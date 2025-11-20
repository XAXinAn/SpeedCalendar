# AI聊天功能后端接口文档

## 概述

本文档定义了AI聊天功能的后端API接口规范。所有接口遵循项目统一的API响应格式。

### 基础信息

- **Base URL**: `http://10.0.2.2:8080/api/`（Android模拟器）
- **响应格式**: JSON
- **统一响应结构**:
```json
{
  "code": 200,
  "message": "成功",
  "data": {...}
}
```

### 状态码说明

- `200`: 请求成功
- `400`: 请求参数错误
- `401`: 未授权
- `403`: 禁止访问
- `404`: 资源不存在
- `500`: 服务器内部错误

---

## 1. 发送聊天消息

### 接口信息
- **URL**: `/api/ai/chat/message`
- **Method**: `POST`
- **描述**: 发送用户消息给AI，获取AI回复

### 请求参数

**Headers**:
```
Content-Type: application/json
```

**Body**:
```json
{
  "message": "帮我安排明天下午3点的会议",
  "sessionId": "session-uuid-123",
  "userId": "user-uuid-456",
  "context": {
    "source": "ocr",
    "additionalInfo": "..."
  }
}
```

**参数说明**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| message | String | 是 | 用户发送的消息内容 |
| sessionId | String | 否 | 会话ID，首次对话时可为null，系统会自动创建新会话 |
| userId | String | 是 | 用户ID |
| context | Map<String, Any> | 否 | 上下文信息，如OCR识别结果等 |

### 响应示例

**成功响应** (200):
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "sessionId": "session-uuid-123",
    "message": "好的，我已经为您安排了明天下午3点的会议。需要我添加到日程中吗？",
    "timestamp": 1700123456789,
    "toolCalls": [
      {
        "toolName": "schedule_analyzer",
        "parameters": {
          "time": "2024-11-21 15:00:00",
          "title": "会议"
        },
        "result": "已识别会议时间和标题"
      }
    ]
  }
}
```

**参数说明**:

| 参数 | 类型 | 说明 |
|------|------|------|
| sessionId | String | 会话ID，用于后续对话 |
| message | String | AI的回复内容 |
| timestamp | Long | 消息时间戳（毫秒） |
| toolCalls | Array | AI调用的工具列表（可选） |

---

## 2. 创建新会话

### 接口信息
- **URL**: `/api/ai/chat/session`
- **Method**: `POST`
- **描述**: 创建一个新的聊天会话

### 请求参数

**Body**:
```json
{
  "userId": "user-uuid-456",
  "title": "今日待办事项"
}
```

**参数说明**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | String | 是 | 用户ID |
| title | String | 否 | 会话标题，可为null，系统会根据首条消息自动生成 |

### 响应示例

**成功响应** (200):
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": "session-uuid-789",
    "title": "今日待办事项",
    "createdAt": 1700123456789,
    "updatedAt": 1700123456789
  }
}
```

---

## 3. 获取聊天历史

### 接口信息
- **URL**: `/api/ai/chat/history/{sessionId}`
- **Method**: `GET`
- **描述**: 获取指定会话的聊天历史记录

### 请求参数

**Path Parameters**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | String | 是 | 会话ID |

**Query Parameters**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 0 | 页码（从0开始） |
| size | Integer | 否 | 50 | 每页数量 |

### 响应示例

**成功响应** (200):
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "messages": [
      {
        "id": "msg-uuid-001",
        "content": "帮我安排明天下午3点的会议",
        "role": "user",
        "timestamp": 1700123456789,
        "toolCalls": null
      },
      {
        "id": "msg-uuid-002",
        "content": "好的，我已经为您安排了明天下午3点的会议。",
        "role": "ai",
        "timestamp": 1700123457890,
        "toolCalls": [
          {
            "toolName": "schedule_analyzer",
            "parameters": {
              "time": "2024-11-21 15:00:00"
            },
            "result": "已识别"
          }
        ]
      }
    ],
    "totalPages": 1,
    "totalElements": 2
  }
}
```

**参数说明**:

| 参数 | 类型 | 说明 |
|------|------|------|
| messages | Array | 消息列表 |
| messages[].id | String | 消息ID |
| messages[].content | String | 消息内容 |
| messages[].role | String | 消息角色："user" 或 "ai" |
| messages[].timestamp | Long | 消息时间戳 |
| messages[].toolCalls | Array | 工具调用记录（仅AI消息） |
| totalPages | Integer | 总页数 |
| totalElements | Integer | 总消息数 |

---

## 4. 获取用户会话列表

### 接口信息
- **URL**: `/api/ai/chat/sessions/{userId}`
- **Method**: `GET`
- **描述**: 获取用户的所有聊天会话列表

### 请求参数

**Path Parameters**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | String | 是 | 用户ID |

**Query Parameters**:

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 0 | 页码（从0开始） |
| size | Integer | 否 | 20 | 每页数量 |

### 响应示例

**成功响应** (200):
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "sessions": [
      {
        "id": "session-uuid-123",
        "title": "今日待办事项",
        "createdAt": 1700123456789,
        "updatedAt": 1700125678901
      },
      {
        "id": "session-uuid-456",
        "title": "OCR识别会议记录",
        "createdAt": 1700120000000,
        "updatedAt": 1700122000000
      }
    ],
    "totalPages": 1,
    "totalElements": 2
  }
}
```

**参数说明**:

| 参数 | 类型 | 说明 |
|------|------|------|
| sessions | Array | 会话列表，按updatedAt倒序排列 |
| sessions[].id | String | 会话ID |
| sessions[].title | String | 会话标题 |
| sessions[].createdAt | Long | 创建时间戳 |
| sessions[].updatedAt | Long | 最后更新时间戳 |
| totalPages | Integer | 总页数 |
| totalElements | Integer | 总会话数 |

---

## 5. 删除会话

### 接口信息
- **URL**: `/api/ai/chat/session/{sessionId}`
- **Method**: `DELETE`
- **描述**: 删除指定的聊天会话及其所有消息

### 请求参数

**Path Parameters**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | String | 是 | 会话ID |

**Query Parameters**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | String | 是 | 用户ID（用于权限验证） |

### 响应示例

**成功响应** (200):
```json
{
  "code": 200,
  "message": "会话已删除",
  "data": null
}
```

---

## 6. 更新会话标题

### 接口信息
- **URL**: `/api/ai/chat/session/{sessionId}/title`
- **Method**: `PUT`
- **描述**: 更新会话的标题

### 请求参数

**Path Parameters**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | String | 是 | 会话ID |

**Body**:
```json
{
  "title": "新的会话标题",
  "userId": "user-uuid-456"
}
```

**参数说明**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | 是 | 新的会话标题 |
| userId | String | 是 | 用户ID（用于权限验证） |

### 响应示例

**成功响应** (200):
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": "session-uuid-123",
    "title": "新的会话标题",
    "createdAt": 1700123456789,
    "updatedAt": 1700130000000
  }
}
```

---

## 工具调用（Tool Calls）说明

### 概述

AI在处理用户请求时，可能需要调用特定工具来完成任务。这些工具调用信息会在响应中返回。

### 支持的工具类型

#### 1. schedule_analyzer（日程分析器）
- **功能**: 从用户消息中提取日程信息
- **参数**:
  - `time`: 时间（ISO 8601格式或描述性文本）
  - `title`: 日程标题
  - `location`: 地点（可选）
  - `description`: 描述（可选）

#### 2. schedule_creator（日程创建器）
- **功能**: 创建新的日程事件
- **参数**:
  - `title`: 日程标题
  - `startTime`: 开始时间
  - `endTime`: 结束时间
  - `location`: 地点（可选）
  - `description`: 描述（可选）
  - `reminder`: 提醒设置（可选）

#### 3. ocr_processor（OCR处理器）
- **功能**: 处理OCR识别结果
- **参数**:
  - `text`: OCR识别的文本
  - `confidence`: 置信度
  - `type`: 文档类型（如：会议记录、发票等）

### 示例

```json
{
  "toolCalls": [
    {
      "toolName": "schedule_analyzer",
      "parameters": {
        "time": "2024-11-21T15:00:00",
        "title": "团队会议",
        "location": "会议室A"
      },
      "result": "已成功解析日程信息"
    },
    {
      "toolName": "schedule_creator",
      "parameters": {
        "title": "团队会议",
        "startTime": "2024-11-21T15:00:00",
        "endTime": "2024-11-21T16:00:00",
        "location": "会议室A"
      },
      "result": "日程创建成功，ID: event-uuid-789"
    }
  ]
}
```

---

## OCR集成示例

### 场景描述

用户使用OCR功能识别会议记录，然后将识别结果发送给AI助手进行处理。

### 调用流程

1. **OCR识别**
   - 用户拍照或上传图片
   - OCR服务识别文字
   - 提取识别结果

2. **发送给AI**
```json
{
  "message": "这是一份会议记录，请帮我提取重要信息并创建日程",
  "sessionId": null,
  "userId": "user-uuid-456",
  "context": {
    "source": "ocr",
    "ocrText": "会议时间：2024年11月21日 下午3:00\n会议地点：会议室A\n参会人员：张三、李四、王五\n议题：项目进度讨论",
    "confidence": 0.95
  }
}
```

3. **AI处理响应**
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "sessionId": "session-uuid-new",
    "message": "我已经分析了这份会议记录，为您提取了以下信息：\n\n- 会议时间：2024年11月21日 下午3:00\n- 会议地点：会议室A\n- 参会人员：张三、李四、王五\n- 议题：项目进度讨论\n\n我已经为您创建了一个日程提醒。是否需要我添加更多细节？",
    "timestamp": 1700123456789,
    "toolCalls": [
      {
        "toolName": "ocr_processor",
        "parameters": {
          "text": "会议时间：2024年11月21日...",
          "type": "meeting_record"
        },
        "result": "已成功解析OCR文本"
      },
      {
        "toolName": "schedule_creator",
        "parameters": {
          "title": "项目进度讨论会议",
          "startTime": "2024-11-21T15:00:00",
          "endTime": "2024-11-21T16:00:00",
          "location": "会议室A",
          "description": "参会人员：张三、李四、王五"
        },
        "result": "日程创建成功"
      }
    ]
  }
}
```

---

## 错误处理

### 错误响应格式

```json
{
  "code": 400,
  "message": "请求参数错误：缺少userId",
  "data": null
}
```

### 常见错误码

| 错误码 | 说明 | 处理建议 |
|--------|------|----------|
| 400 | 请求参数错误 | 检查请求参数是否完整且格式正确 |
| 401 | 未授权 | 检查用户登录状态 |
| 404 | 会话不存在 | 创建新会话或检查sessionId是否正确 |
| 429 | 请求过于频繁 | 实施请求限流，稍后重试 |
| 500 | 服务器内部错误 | 联系技术支持或稍后重试 |
| 503 | AI服务暂时不可用 | 稍后重试，显示友好提示 |

---

## 数据库设计建议

### 表结构

#### 1. chat_sessions（聊天会话表）
```sql
CREATE TABLE chat_sessions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    title VARCHAR(255),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_updated_at (updated_at)
);
```

#### 2. chat_messages（聊天消息表）
```sql
CREATE TABLE chat_messages (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    role VARCHAR(10) NOT NULL,
    timestamp BIGINT NOT NULL,
    tool_calls JSON,
    INDEX idx_session_id (session_id),
    INDEX idx_timestamp (timestamp),
    FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE
);
```

#### 3. tool_executions（工具执行记录表）
```sql
CREATE TABLE tool_executions (
    id VARCHAR(36) PRIMARY KEY,
    message_id VARCHAR(36) NOT NULL,
    tool_name VARCHAR(50) NOT NULL,
    parameters JSON NOT NULL,
    result TEXT,
    status VARCHAR(20) NOT NULL,
    created_at BIGINT NOT NULL,
    INDEX idx_message_id (message_id),
    FOREIGN KEY (message_id) REFERENCES chat_messages(id) ON DELETE CASCADE
);
```

---

## 性能优化建议

### 1. 缓存策略
- 使用Redis缓存最近的对话历史
- 缓存用户的会话列表
- 缓存AI模型的响应（相似问题）

### 2. 异步处理
- AI响应采用异步处理，避免长时间阻塞
- 使用消息队列处理工具调用
- 实现WebSocket或SSE进行实时消息推送

### 3. 分页优化
- 聊天历史默认只加载最近50条
- 实现虚拟滚动，按需加载更多历史记录
- 会话列表分页，避免一次加载过多数据

---

## 安全性建议

### 1. 权限验证
- 所有接口需要验证用户身份
- 验证用户是否有权访问指定会话
- 实施请求频率限制，防止滥用

### 2. 数据加密
- 传输层使用HTTPS
- 敏感信息加密存储
- 定期清理过期会话数据

### 3. 内容审核
- 对用户输入进行敏感词过滤
- 限制单条消息长度
- 记录异常请求日志

---

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2024-11-20 | 初始版本，定义基础API接口 |

---

## 联系方式

如有问题或建议，请联系开发团队。
