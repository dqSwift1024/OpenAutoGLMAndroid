package com.dd.autoglm.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

class AutoGLMAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AutoGLMService"
        private var instance: AutoGLMAccessibilityService? = null

        fun getInstance(): AutoGLMAccessibilityService? = instance

        fun isServiceEnabled(): Boolean = instance != null
    }

    private val _currentApp = MutableStateFlow<String?>(null)
    val currentApp: StateFlow<String?> = _currentApp.asStateFlow()

    private val _latestScreenshot = MutableStateFlow<Bitmap?>(null)
    val latestScreenshot: StateFlow<Bitmap?> = _latestScreenshot.asStateFlow()
    private var promptView: LinearLayout? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        removePromptView()
        super.onDestroy()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            when (it.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    val packageName = it.packageName?.toString()
                    _currentApp.value = packageName
                }
            }
        }
    }

    override fun onInterrupt() {
        // 服务中断
    }

    fun takeScreenshot(callback: (Bitmap?) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Log.d(TAG, "开始截图...")
                takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    mainExecutor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(result: ScreenshotResult) {
                            Log.d(TAG, "截图成功")
                            try {
                                val hardwareBuffer = result.hardwareBuffer
                                val hardwareBitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, null)
                                hardwareBuffer.close()

                                // 将 HARDWARE 格式的 Bitmap 转换为可访问的格式
                                val bitmap =
                                    if (hardwareBitmap != null && hardwareBitmap.config == Bitmap.Config.HARDWARE) {
                                        // 转换为 ARGB_8888 格式以便访问像素
                                        hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false)
                                    } else {
                                        hardwareBitmap
                                    }

                                // 回收硬件 Bitmap（如果已转换）
                                if (bitmap != hardwareBitmap) {
                                    hardwareBitmap?.recycle()
                                }

                                _latestScreenshot.value = bitmap
                                Log.d(
                                    TAG,
                                    "截图转换成功，尺寸: ${bitmap?.width}x${bitmap?.height}, 格式: ${bitmap?.config}"
                                )
                                callback(bitmap)
                            } catch (e: Exception) {
                                Log.e(TAG, "处理截图失败", e)
                                callback(null)
                            }
                        }

                        override fun onFailure(errorCode: Int) {
                            Log.e(TAG, "截图失败，错误码: $errorCode")
                            callback(null)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "调用截图API失败", e)
                callback(null)
            }
        } else {
            Log.w(
                TAG,
                "Android版本低于R (API 30)，不支持截图。当前版本: ${Build.VERSION.SDK_INT}"
            )
            callback(null)
        }
    }

    suspend fun takeScreenshotSuspend(): Bitmap? = suspendCancellableCoroutine { continuation ->
        takeScreenshot { bitmap ->
            continuation.resume(bitmap)
        }
    }

    fun getRootNode(): AccessibilityNodeInfo? {
        return rootInActiveWindow
    }

    fun findNodeByText(text: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val nodes = root.findAccessibilityNodeInfosByText(text)
        return nodes.firstOrNull()
    }

    fun tap(x: Float, y: Float) {
        val path = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()

        dispatchGesture(gesture, null, null)
    }

    fun longPress(x: Float, y: Float, durationMs: Long = 3000L) {
        val path = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()

        dispatchGesture(gesture, null, null)
    }

    fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long = 300) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()

        dispatchGesture(gesture, null, null)
    }

    fun performBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun performHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun performClick(node: AccessibilityNodeInfo): Boolean {
        return if (node.isClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            // 如果节点不可点击，尝试找到父节点
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    val result = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    parent.recycle()
                    return result
                }
                val oldParent = parent
                parent = parent.parent
                oldParent.recycle()
            }
            false
        }
    }

    fun setText(node: AccessibilityNodeInfo, text: String) {
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    /**
     * 在当前界面上方显示确认或接管条。无障碍浮层不依赖本应用处于前台。
     */
    suspend fun awaitDecision(message: String, takeover: Boolean): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val resumed = AtomicBoolean(false)
            fun finish(result: Boolean) {
                if (!resumed.compareAndSet(false, true)) return
                mainExecutor.execute { removePromptView() }
                if (continuation.isActive) continuation.resume(result)
            }
            continuation.invokeOnCancellation {
                mainExecutor.execute { removePromptView() }
            }
            mainExecutor.execute {
                removePromptView()
                val container = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(20), dp(16), dp(20), dp(16))
                    setBackgroundColor(Color.WHITE)
                }
                container.addView(TextView(this).apply {
                    text = if (takeover) {
                        "$message\n\n请在当前页面手动完成后点继续。"
                    } else {
                        message
                    }
                    setTextColor(Color.parseColor("#212121"))
                    textSize = 16f
                })
                val actions = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END
                    setPadding(0, dp(12), 0, 0)
                }
                if (takeover) {
                    actions.addView(promptButton("继续") { finish(true) })
                } else {
                    actions.addView(promptButton("取消") { finish(false) })
                    actions.addView(promptButton("确认") { finish(true) })
                }
                container.addView(actions)
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.BOTTOM
                }
                overlayWindowManager().addView(container, params)
                promptView = container
            }
        }
    }

    private fun promptButton(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            setOnClickListener { onClick() }
        }
    }

    private fun removePromptView() {
        val view = promptView ?: return
        promptView = null
        try {
            overlayWindowManager().removeView(view)
        } catch (e: Exception) {
            Log.w(TAG, "移除提示浮层失败", e)
        }
    }

    private fun overlayWindowManager(): WindowManager {
        return getSystemService(WINDOW_SERVICE) as WindowManager
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
