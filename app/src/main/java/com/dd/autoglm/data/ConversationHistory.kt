package com.dd.autoglm.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 对话历史数据类
 */
data class ConversationHistory(
    val id: String,
    val title: String, // 用户发送的第一条消息作为标题
    val firstMessage: String, // 用户发送的第一条消息
    val timestamp: Long, // 创建时间戳
    val date: String // 格式化后的日期字符串
) {
    companion object {
        /**
         * 创建新的对话历史
         */
        fun create(userMessage: String): ConversationHistory {
            val timestamp = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.format(Date(timestamp))
            
            // 使用用户消息的前50个字符作为标题
            val title = if (userMessage.length > 50) {
                userMessage.take(50) + "..."
            } else {
                userMessage
            }
            
            return ConversationHistory(
                id = timestamp.toString(),
                title = title,
                firstMessage = userMessage,
                timestamp = timestamp,
                date = date
            )
        }
    }
}

