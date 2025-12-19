package com.dd.autoglm.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 对话历史仓库，用于保存和读取对话历史
 */
class ConversationHistoryRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("conversation_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val key = "conversation_histories"
    
    /**
     * 保存对话历史
     */
    fun saveHistory(history: ConversationHistory) {
        val histories = getAllHistories().toMutableList()
        // 添加到列表开头（最新的在前面）
        histories.add(0, history)
        // 限制最多保存100条历史记录
        if (histories.size > 100) {
            histories.removeAt(histories.size - 1)
        }
        saveHistories(histories)
    }
    
    /**
     * 获取所有对话历史
     */
    fun getAllHistories(): List<ConversationHistory> {
        val json = sharedPreferences.getString(key, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ConversationHistory>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 根据日期分组获取对话历史
     */
    fun getHistoriesGroupedByDate(): Map<String, List<ConversationHistory>> {
        val histories = getAllHistories()
        return histories.groupBy { it.date }
    }
    
    /**
     * 删除对话历史
     */
    fun deleteHistory(historyId: String) {
        val histories = getAllHistories().toMutableList()
        histories.removeAll { it.id == historyId }
        saveHistories(histories)
    }
    
    /**
     * 清空所有对话历史
     */
    fun clearAllHistories() {
        sharedPreferences.edit().remove(key).apply()
    }
    
    private fun saveHistories(histories: List<ConversationHistory>) {
        val json = gson.toJson(histories)
        sharedPreferences.edit().putString(key, json).apply()
    }
}

