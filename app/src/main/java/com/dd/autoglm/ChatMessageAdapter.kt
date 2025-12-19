package com.dd.autoglm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dd.autoglm.viewmodel.ChatMessage
import com.dd.autoglm.viewmodel.MessageRole

class ChatMessageAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatMessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_ASSISTANT = 2
        private const val VIEW_TYPE_LOADING = 3
    }

    private var showLoadingIndicator: Boolean = false

    fun setShowLoadingIndicator(show: Boolean) {
        if (showLoadingIndicator != show) {
            showLoadingIndicator = show
            if (show) {
                notifyItemInserted(itemCount)
            } else {
                notifyItemRemoved(itemCount)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == currentList.size && showLoadingIndicator) {
            VIEW_TYPE_LOADING
        } else {
            when (currentList[position].role) {
                MessageRole.USER -> VIEW_TYPE_USER
                MessageRole.ASSISTANT -> VIEW_TYPE_ASSISTANT
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_USER -> UserMessageViewHolder(inflater.inflate(R.layout.item_chat_message_user, parent, false))
            VIEW_TYPE_ASSISTANT -> AssistantMessageViewHolder(inflater.inflate(R.layout.item_chat_message_assistant, parent, false))
            VIEW_TYPE_LOADING -> LoadingViewHolder(inflater.inflate(R.layout.item_chat_loading, parent, false))
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) != VIEW_TYPE_LOADING) {
            val message = currentList[position]
            when (holder) {
                is UserMessageViewHolder -> holder.bind(message)
                is AssistantMessageViewHolder -> holder.bind(message)
            }
        }
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + if (showLoadingIndicator) 1 else 0
    }

    class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageContent: TextView = itemView.findViewById(R.id.message_content)

        fun bind(message: ChatMessage) {
            messageContent.text = message.content
        }
    }

    class AssistantMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thinkingProcessText: TextView = itemView.findViewById(R.id.thinking_process_text)
        private val thinkingProcessToggle: TextView = itemView.findViewById(R.id.thinking_process_toggle)
        private val actionContentText: TextView = itemView.findViewById(R.id.action_content_text)

        private var isThinkingExpanded = false

        fun bind(message: ChatMessage) {
            // Thinking process
            if (!message.thinking.isNullOrBlank()) {
                thinkingProcessText.text = "思考过程:\n${message.thinking}"
                thinkingProcessToggle.visibility = View.VISIBLE
                thinkingProcessText.visibility = if (isThinkingExpanded) View.VISIBLE else View.GONE
                thinkingProcessToggle.text = if (isThinkingExpanded) "收起思考" else "思考过程..."

                thinkingProcessToggle.setOnClickListener { 
                    isThinkingExpanded = !isThinkingExpanded
                    thinkingProcessText.visibility = if (isThinkingExpanded) View.VISIBLE else View.GONE
                    thinkingProcessToggle.text = if (isThinkingExpanded) "收起思考" else "思考过程..."
                }
            } else {
                thinkingProcessText.visibility = View.GONE
                thinkingProcessToggle.visibility = View.GONE
            }

            // Action content
            if (!message.action.isNullOrBlank()) {
                actionContentText.text = message.action
                actionContentText.visibility = View.VISIBLE
            } else {
                actionContentText.visibility = View.GONE
            }
        }
    }

    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // No specific binding needed for a simple ProgressBar and TextView
    }

    private class ChatMessageDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}