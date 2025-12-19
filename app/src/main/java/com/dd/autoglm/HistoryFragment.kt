package com.dd.autoglm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dd.autoglm.data.ConversationHistory
import com.dd.autoglm.data.ConversationHistoryRepository
import com.dd.autoglm.databinding.FragmentHistoryBinding

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var historyRepository: ConversationHistoryRepository
    private lateinit var historyAdapter: HistoryListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        historyRepository = ConversationHistoryRepository(requireContext())
        setupRecyclerView()
        loadHistories()
    }
    
    private fun setupRecyclerView() {
        historyAdapter = HistoryListAdapter(
            onItemClick = { history ->
                // 点击历史记录，导航回首页并加载该对话
                // 这里可以根据需要实现加载历史对话的功能
            },
            onItemDelete = { history ->
                // 长按删除历史记录
                showDeleteDialog(history)
            }
        )
        
        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
        }
    }
    
    private fun loadHistories() {
        val historiesGrouped = historyRepository.getHistoriesGroupedByDate()
        historyAdapter.submitList(historiesGrouped)
    }
    
    private fun showDeleteDialog(history: ConversationHistory) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除历史记录")
            .setMessage("确定要删除这条历史记录吗？")
            .setPositiveButton("删除") { _, _ ->
                historyRepository.deleteHistory(history.id)
                loadHistories()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    override fun onResume() {
        super.onResume()
        // 每次显示时刷新历史记录列表
        loadHistories()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
