# Open-AutoGLM-Android

用自然语言操控 Android 手机的客户端。应用把当前屏幕截图和任务发给视觉语言模型，再通过无障碍服务执行点击、滑动、输入等操作，直到任务完成。

默认对接智谱开放平台的 AutoGLM 手机模型，也可以改成兼容的 OpenAI Chat Completions 接口。

## 功能

- 对话式下发任务，展示模型思考过程和执行结果
- 截图后循环调用模型，并在手机上执行返回的操作
- 保存对话历史，可从侧边栏重新查看
- 在设置页配置 API Key、Base URL 和模型名称

## 环境要求

- Android Studio，并安装 Android SDK 36
- JDK 11
- 一台 Android 10（API 29）或更高版本的真机

模拟器上的截图经常是黑屏，建议用真机调试。

## 构建与运行

用 Android Studio 打开本仓库，等待 Gradle 同步完成后运行 `app`。

也可以在命令行构建：

```bash
./gradlew :app:assembleDebug
```

调试包输出在 `app/build/outputs/apk/debug/`。

## 使用前配置

1. 安装并打开应用，进入设置。
2. 打开「无障碍服务」，在系统设置里启用 **Open-AutoGLM-Android**。该服务用于截图，以及执行点击、滑动和输入。
3. 在[智谱开放平台](https://open.bigmodel.cn/)申请 API Key，填入设置页。
4. 确认 Base URL 和模型名称后保存。未修改时使用：

| 配置项 | 默认值 |
| --- | --- |
| Base URL | `https://open.bigmodel.cn/api/paas/v4` |
| Model Name | `autoglm-phone-9b` |

5. 回到「新对话」，输入任务并发送。例如：「打开设置，查看关于手机」。

API Key 只保存在本机 DataStore 中，不会写入仓库。

## 模型可执行的操作

客户端会把模型返回的 `do(...)` / `finish(...)` 转成无障碍操作。当前已实现：

| 指令 | 作用 |
| --- | --- |
| `Launch` | 按应用名启动应用 |
| `Tap` | 点击。坐标范围是左上角 `(0,0)` 到右下角 `(999,999)` |
| `Type` / `Type_Name` | 向当前焦点输入框写入文本。`Type_Name` 与 `Type` 相同 |
| `Swipe` | 从起点滑到终点，时长按距离落在 1–2 秒 |
| `Long Press` | 长按约 3 秒 |
| `Double Tap` | 双击 |
| `Back` | 返回 |
| `Home` | 回到桌面 |
| `Wait` | 等待页面加载 |
| `Take_over` | 暂停并提示手动完成登录或验证，确认后继续 |
| `Note` / `Call_API` | 记录当前页面后继续，不额外请求接口 |
| `Interact` | 多个候选项时继续根据当前屏幕执行 |
| `finish` | 结束任务并返回说明 |

带 `message` 的 `Tap` 会在屏幕底部弹出确认。取消后本次任务结束。支付、隐私相关点击会走这条路径。`Take_over` 同样用这条浮层，不要求先切回本应用。

## 项目结构

```text
app/src/main/java/com/dd/autoglm/
├── HomeFragment.kt              新对话
├── HistoryFragment.kt           历史记录
├── SettingsFragment.kt          无障碍与 API 配置
├── network/ModelClient.kt       模型请求与提示词
├── domain/ActionExecutor.kt     解析并执行操作
├── service/                     无障碍服务与截图
└── data/                        设置与对话历史
```

应用 ID 为 `com.dd.autoglm`，`minSdk` 29，`targetSdk` 36。
