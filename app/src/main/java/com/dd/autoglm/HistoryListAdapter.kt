package com.dd.autoglm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dd.autoglm.data.ConversationHistory

/**
 * 历史记录列表适配器
 */
class HistoryListAdapter(
    private val onItemClick: (ConversationHistory) -> Unit,
    private val onItemDelete: (ConversationHistory) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    private val items = mutableListOf<HistoryItem>()
    
    sealed class HistoryItem {
        data class DateHeader(val date: String) : HistoryItem()
        data class Conversation(val history: ConversationHistory) : HistoryItem()
    }
    
    fun submitList(histories: Map<String, List<ConversationHistory>>) {
        items.clear()
        histories.keys.sortedDescending().forEach { date ->
            items.add(HistoryItem.DateHeader(date))
            histories[date]?.forEach { history ->
                items.add(HistoryItem.Conversation(history))
            }
        }
        notifyDataSetChanged()
    }
    
    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is HistoryItem.DateHeader -> VIEW_TYPE_DATE_HEADER
            is HistoryItem.Conversation -> VIEW_TYPE_CONVERSATION
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_DATE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_history_date_header, parent, false)
                DateHeaderViewHolder(view)
            }
            VIEW_TYPE_CONVERSATION -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_history_conversation, parent, false)
                ConversationViewHolder(view, onItemClick, onItemDelete)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is HistoryItem.DateHeader -> {
                (holder as DateHeaderViewHolder).bind(item.date)
            }
            is HistoryItem.Conversation -> {
                (holder as ConversationViewHolder).bind(item.history)
            }
        }
    }
    
    override fun getItemCount(): Int = items.size
    
    companion object {
        private const val VIEW_TYPE_DATE_HEADER = 0
        private const val VIEW_TYPE_CONVERSATION = 1
    }
    
    class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateText: TextView = itemView.findViewById(R.id.date_text)
        
        fun bind(date: String) {
            dateText.text = formatDate(date)
        }
        
        private fun formatDate(date: String): String {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date())
            val yesterday = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))
            
            return when (date) {
                today -> "今天"
                yesterday -> "昨天"
                else -> {
                    val dateObj = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(date)
                    java.text.SimpleDateFormat("M月d日", java.util.Locale.getDefault()).format(dateObj ?: java.util.Date())
                }
            }
        }
    }
    
    class ConversationViewHolder(
        itemView: View,
        private val onItemClick: (ConversationHistory) -> Unit,
        private val onItemDelete: (ConversationHistory) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.title_text)
        
        fun bind(history: ConversationHistory) {
            titleText.text = history.title
            itemView.setOnClickListener { onItemClick(history) }
            itemView.setOnLongClickListener {
                onItemDelete(history)
                true
            }
        }
    }
}

