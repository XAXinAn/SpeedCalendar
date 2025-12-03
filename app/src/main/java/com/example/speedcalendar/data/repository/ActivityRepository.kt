package com.example.speedcalendar.data.repository

import android.util.Log
import com.example.speedcalendar.data.api.RetrofitClient
import com.example.speedcalendar.data.model.ActivityMessage
import com.example.speedcalendar.data.model.ActivityMessagesResponse

/**
 * 活动消息仓库
 */
class ActivityRepository {

    private val apiService = RetrofitClient.activityApiService
    private val TAG = "ActivityRepository"

    /**
     * 获取消息列表
     */
    suspend fun getMessages(page: Int = 1, pageSize: Int = 20): Result<ActivityMessagesResponse> {
        return try {
            val response = apiService.getMessages(page, pageSize)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.code == 200 && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.message ?: "获取消息列表失败"))
                }
            } else {
                Result.failure(Exception("服务器错误: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取消息列表异常", e)
            Result.failure(e)
        }
    }

    /**
     * 获取未读消息数量
     */
    suspend fun getUnreadCount(): Result<Int> {
        return try {
            val response = apiService.getUnreadCount()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.code == 200 && body.data != null) {
                    Result.success(body.data.unreadCount)
                } else {
                    Result.failure(Exception(body?.message ?: "获取未读数量失败"))
                }
            } else {
                Result.failure(Exception("服务器错误: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取未读数量异常", e)
            Result.failure(e)
        }
    }

    /**
     * 标记单条消息已读
     */
    suspend fun markAsRead(messageId: String): Result<Unit> {
        return try {
            val response = apiService.markAsRead(messageId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.code == 200) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(body?.message ?: "标记已读失败"))
                }
            } else {
                Result.failure(Exception("服务器错误: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "标记已读异常", e)
            Result.failure(e)
        }
    }

    /**
     * 标记全部已读
     */
    suspend fun markAllAsRead(): Result<Int> {
        return try {
            val response = apiService.markAllAsRead()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.code == 200 && body.data != null) {
                    Result.success(body.data.readCount)
                } else {
                    Result.failure(Exception(body?.message ?: "标记全部已读失败"))
                }
            } else {
                Result.failure(Exception("服务器错误: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "标记全部已读异常", e)
            Result.failure(e)
        }
    }

    /**
     * 获取消息详情
     */
    suspend fun getMessageDetail(messageId: String): Result<ActivityMessage> {
        return try {
            val response = apiService.getMessageDetail(messageId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.code == 200 && body.data != null) {
                    Result.success(body.data)
                } else {
                    Result.failure(Exception(body?.message ?: "获取消息详情失败"))
                }
            } else {
                Result.failure(Exception("服务器错误: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取消息详情异常", e)
            Result.failure(e)
        }
    }
}
