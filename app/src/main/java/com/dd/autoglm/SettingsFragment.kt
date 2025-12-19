package com.dd.autoglm

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dd.autoglm.databinding.FragmentSettingsBinding
import com.dd.autoglm.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeUiState()
    }

    private fun setupListeners() {
        binding.goToAccessibilitySettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.apiKeyInput.doOnTextChanged { text, _, _, _ ->
            viewModel.updateApiKey(text.toString())
        }

        binding.baseUrlInput.doOnTextChanged { text, _, _, _ ->
            viewModel.updateBaseUrl(text.toString())
        }

        binding.modelNameInput.doOnTextChanged { text, _, _, _ ->
            viewModel.updateModelName(text.toString())
        }

        binding.saveButton.setOnClickListener {
            viewModel.saveSettings()
        }

        // 点击状态卡可以清除错误信息
        binding.statusCard.setOnClickListener {
            viewModel.clearError()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    // 更新输入框内容，仅当文本不同时
                    if (binding.apiKeyInput.text.toString() != uiState.apiKey) {
                        binding.apiKeyInput.setText(uiState.apiKey)
                    }
                    if (binding.baseUrlInput.text.toString() != uiState.baseUrl) {
                        binding.baseUrlInput.setText(uiState.baseUrl)
                    }
                    if (binding.modelNameInput.text.toString() != uiState.modelName) {
                        binding.modelNameInput.setText(uiState.modelName)
                    }

                    // 更新无障碍服务状态
                    binding.accessibilityStatus.text = if (uiState.isAccessibilityEnabled) "状态: 已启用" else "状态: 未启用"
                    binding.goToAccessibilitySettings.text = if (uiState.isAccessibilityEnabled) "已开启" else "去开启"
                    binding.goToAccessibilitySettings.isEnabled = !uiState.isAccessibilityEnabled

                    // 更新加载状态
                    binding.saveProgress.isVisible = uiState.isLoading
                    binding.saveButton.isEnabled = !uiState.isLoading

                    // 显示保存状态和反馈
                    val statusMessage = when {
                        uiState.saveSuccess == true -> "设置已保存"
                        uiState.error != null -> uiState.error
                        else -> null
                    }

                    binding.statusCard.isVisible = statusMessage != null
                    binding.statusText.text = statusMessage

                    // 如果保存成功，3秒后自动隐藏状态卡
                    if (uiState.saveSuccess == true) {
                        lifecycleScope.launch {
                            delay(3000)
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 当用户从系统设置返回时，刷新无障碍服务的状态
        viewModel.checkAccessibilityService()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
