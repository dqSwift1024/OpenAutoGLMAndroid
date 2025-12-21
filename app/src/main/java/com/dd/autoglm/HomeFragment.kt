package com.dd.autoglm

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.dd.autoglm.data.ConversationHistoryRepository
import com.dd.autoglm.databinding.FragmentHomeBinding
import com.dd.autoglm.viewmodel.ChatViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var historyRepository: ConversationHistoryRepository

    private lateinit var chatAdapter: ChatMessageAdapter
    private var isFirstUserMessage = true // 标记是否是第一条用户消息

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        historyRepository = ConversationHistoryRepository(requireContext())
        setupToolbar()
        setupRecyclerView()
        setupInputArea()
        setupKeyboardInsets()
        observeViewModel()
    }

    private fun setupToolbar() {
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            // Update toolbar title dynamically
            // The actual app name is observed from ViewModel
        }

        // Find the clear button in the toolbar (assuming it's part of MainActivity's menu)
        // For now, we'll assume a clear button will be handled in MainActivity's menu or a custom toolbar within HomeFragment
        // If you want a custom toolbar for HomeFragment, you'd inflate it here and set it as supportActionBar
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatMessageAdapter()
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chatAdapter
        }
    }

    private fun setupInputArea() {
        binding.sendButton.setOnClickListener {
            val userInput = binding.messageInput.text.toString()
            if (userInput.isNotBlank()) {
                // 如果是第一条用户消息，保存到历史记录
                if (isFirstUserMessage) {
                    val history = com.dd.autoglm.data.ConversationHistory.create(userInput)
                    historyRepository.saveHistory(history)
                    isFirstUserMessage = false
                }
                
                viewModel.sendMessage(userInput)
                binding.messageInput.text.clear()
                hideKeyboard()
            }
        }

        // Disable send button if input is empty or loading
        binding.messageInput.doOnTextChanged { text, _, _, _ ->
            binding.sendButton.isEnabled = text?.isNotBlank() == true && !viewModel.uiState.value.isLoading
        }

        // Initially disable send button
        binding.sendButton.isEnabled = false

        // Handle error card dismiss
        binding.closeErrorButton.setOnClickListener { 
            viewModel.clearError()
        }
    }
    
    /**
     * 使用WindowInsets处理键盘，确保输入框在键盘弹出时被顶上来
     */
    private fun setupKeyboardInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // 计算键盘的实际高度（减去系统导航栏高度）
            val keyboardHeight = if (imeInsets.bottom > 0) {
                imeInsets.bottom - systemBarsInsets.bottom
            } else {
                0
            }
            
            // 使用translationY来移动输入框，使其在键盘弹出时被顶上来
            binding.inputLayout.translationY = -keyboardHeight.toFloat()
            
            // 设置RecyclerView的底部padding，避免内容被输入框遮挡
            val inputLayoutHeight = binding.inputLayout.height
            val bottomPadding = if (keyboardHeight > 0) {
                keyboardHeight + inputLayoutHeight
            } else {
                inputLayoutHeight
            }
            
            binding.chatRecyclerView.setPadding(
                binding.chatRecyclerView.paddingLeft,
                binding.chatRecyclerView.paddingTop,
                binding.chatRecyclerView.paddingRight,
                bottomPadding
            )
            
            // 如果键盘已弹出，滚动到底部确保输入框可见
            if (keyboardHeight > 0) {
                binding.root.postDelayed({
                    scrollToBottom()
                }, 100)
            }
            
            insets
        }
        
        // 当输入框获得焦点时，确保它可见
        binding.messageInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // 延迟滚动，确保键盘已经弹出
                binding.root.postDelayed({
                    scrollToBottom()
                }, 300)
            }
        }
    }
    
    /**
     * 滚动RecyclerView到底部
     */
    private fun scrollToBottom() {
        if (::chatAdapter.isInitialized && chatAdapter.itemCount > 0) {
            binding.chatRecyclerView.post {
                binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { uiState ->
                    // Update messages
                    chatAdapter.submitList(uiState.messages) {
                        // Scroll to bottom when new messages are added or UI state changes
                        if (uiState.messages.isNotEmpty()) {
                            binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                        }
                    }

                    // Show/hide loading indicator
                    chatAdapter.setShowLoadingIndicator(uiState.isLoading)

                    // 不再显示"当前应用："信息

                    // Enable/disable send button
                    binding.sendButton.isEnabled = binding.messageInput.text.isNotBlank() && !uiState.isLoading

                    // Show/hide error card
                    if (uiState.error != null) {
                        binding.errorCard.visibility = View.VISIBLE
                        binding.errorText.text = uiState.error
                    } else {
                        binding.errorCard.visibility = View.GONE
                    }

                    // Show task completed message as Toast
                    uiState.taskCompletedMessage?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        viewModel.clearTaskCompletedMessage()
                        // 任务完成后，重置标记，下次发送消息时创建新的历史记录
                        isFirstUserMessage = true
                        // 刷新侧滑栏的历史记录列表
                        (activity as? MainActivity)?.refreshHistoryList()
                    }
                    
                    // 当消息列表被清空时（新会话开始），重置标记
                    if (uiState.messages.isEmpty() && !uiState.isLoading) {
                        isFirstUserMessage = true
                    }
                }
            }
        }
    }

    private fun hideKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.messageInput.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}