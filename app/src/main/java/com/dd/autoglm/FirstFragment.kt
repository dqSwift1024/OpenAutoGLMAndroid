package com.dd.autoglm

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.dd.autoglm.databinding.FragmentFirstBinding
import com.dd.autoglm.viewmodel.ChatViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()

    private lateinit var chatAdapter: ChatMessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupInputArea()
        observeViewModel()
    }

    private fun setupToolbar() {
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            // Update toolbar title dynamically
            // The actual app name is observed from ViewModel
        }

        // Find the clear button in the toolbar (assuming it's part of MainActivity's menu)
        // For now, we'll assume a clear button will be handled in MainActivity's menu or a custom toolbar within FirstFragment
        // If you want a custom toolbar for FirstFragment, you'd inflate it here and set it as supportActionBar
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