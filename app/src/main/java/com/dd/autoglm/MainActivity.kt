package com.dd.autoglm

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.dd.autoglm.data.ConversationHistoryRepository
import com.dd.autoglm.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var historyRepository: ConversationHistoryRepository
    private lateinit var historyAdapter: HistoryListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置完整的沉浸式模式
        setupImmersiveMode()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        historyRepository = ConversationHistoryRepository(this)
        setupHistoryRecyclerView()

        setSupportActionBar(binding.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // AppBarConfiguration 需要包含所有顶级目标，包括 HomeFragment
        // 这样导航图标才能正常工作，但 HomeFragment 不会在侧滑栏菜单中显示（因为侧滑栏是自定义的）
        appBarConfiguration = AppBarConfiguration(setOf(
            R.id.HomeFragment, R.id.nav_history, R.id.nav_settings
        ), drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)

        findViewById<LinearLayout>(R.id.settings_button).setOnClickListener {
            navController.navigate(R.id.nav_settings)
            drawerLayout.closeDrawers()
        }

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }
        
        // 配置Toolbar：去掉背景，只显示导航图标（在setupActionBarWithNavController之后调用）
        setupToolbar()
        
        // 设置AppBarLayout的paddingTop，避免与状态栏重叠
        setupAppBarPadding()
        
        // 延迟设置导航图标颜色，确保图标已加载
        binding.toolbar.post {
            binding.toolbar.navigationIcon?.setTint(android.graphics.Color.BLACK)
        }
    }

    /**
     * 设置沉浸式模式 - 参考DeepSeek效果
     * 状态栏透明，内容延伸到状态栏下方，但保留状态栏图标显示
     */
    private fun setupImmersiveMode() {
        // 状态栏透明，内容延伸到状态栏下方，但不隐藏状态栏
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
        
        // 设置状态栏和导航栏为透明
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        // 设置状态栏图标为深色（黑色），适配浅色背景（灰白色#F0F0F0）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        
        // 防止软键盘改变窗口大小
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    /**
     * 配置Toolbar样式 - 设置灰白色背景，导航图标为深色
     */
    private fun setupToolbar() {
        binding.toolbar.apply {
            // 设置背景为灰白色
            setBackgroundColor(0xFFF0F0F0.toInt())
            // 设置导航图标颜色为深色（黑色），适配浅色背景
            navigationIcon?.setTint(android.graphics.Color.BLACK)
        }
    }
    
    /**
     * 设置AppBarLayout的paddingTop，避免与状态栏重叠
     */
    private fun setupAppBarPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(0, statusBarInsets.top, 0, 0)
            insets
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
    
    /**
     * 设置历史记录RecyclerView
     */
    private fun setupHistoryRecyclerView() {
        historyAdapter = HistoryListAdapter(
            onItemClick = { history ->
                // 点击历史记录，导航回首页
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.HomeFragment)
                binding.drawerLayout.closeDrawers()
            },
            onItemDelete = { history ->
                // 长按删除历史记录
                historyRepository.deleteHistory(history.id)
                refreshHistoryList()
            }
        )
        
        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = historyAdapter
        }
        
        refreshHistoryList()
    }
    
    /**
     * 刷新历史记录列表
     */
    fun refreshHistoryList() {
        val historiesGrouped = historyRepository.getHistoriesGroupedByDate()
        historyAdapter.submitList(historiesGrouped)
    }
    
    override fun onResume() {
        super.onResume()
        // 每次恢复时刷新历史记录列表
        if (::historyAdapter.isInitialized) {
            refreshHistoryList()
        }
    }
}
