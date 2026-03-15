# 架构与实现

## 系统架构

```
┌─────────────────────┐         WebSocket (ws://IP:9876)         ┌─────────────────────┐
│   Android 客户端     │ ──────────────────────────────────────▶ │   Mac 服务端         │
│                     │                                          │                     │
│  文字输入 / 快捷键   │   JSON 消息                               │  CGEvent 模拟键鼠    │
│  触控板 / 剪贴板     │ ◀────────────────────────────────────── │  剪贴板操作          │
│                     │         (剪贴板内容回传)                    │                     │
└─────────────────────┘                                          └─────────────────────┘
        │                                                                 │
        │  mDNS (NsdManager)          Bonjour (_typesync._tcp.)           │
        └─────────────────── 自动发现 ──────────────────────────────────────┘
```

## 通信协议

Android 与 Mac 之间通过 WebSocket 传输 JSON 消息。所有消息共享统一结构：

```json
{
  "type": "text | key | macro | clipboard | mouse",
  // 以下字段按 type 可选
  "content": "string",
  "key": "string",
  "modifiers": ["cmd", "ctrl", "alt", "shift"],
  "name": "string",
  "action": "string",
  "dx": 0.0,
  "dy": 0.0,
  "button": "left | right"
}
```

### 消息类型

#### `text` — 文本输入
```json
{"type": "text", "content": "你好世界"}
```
服务端通过 CGEvent 逐字符发送 Unicode 键盘事件。

#### `key` — 按键
```json
{"type": "key", "key": "c", "modifiers": ["cmd"]}
```
支持的按键：a-z、0-9、F1-F12、方向键、Enter、Tab、Esc、Backspace、Delete、Space、Home、End、PgUp、PgDn 及常用符号。

修饰键：`cmd`、`ctrl`、`alt`/`option`、`shift`。

#### `macro` — 宏操作
```json
{"type": "macro", "name": "clear_input"}
```
预定义宏：

| 名称 | 操作 |
|------|------|
| `clear_input` | ⌘A → ⌫ |
| `select_all_copy` | ⌘A → ⌘C |
| `select_all_cut` | ⌘A → ⌘X |
| `select_all_paste` | ⌘A → ⌘V |
| `undo` | ⌘Z |
| `redo` | ⇧⌘Z |
| `line_delete` | ⇧⌘K |
| `line_select` | Home → ⇧End |
| `word_delete_back` | ⌥⌫ |
| `word_delete_forward` | ⌥Delete |
| `screenshot` | ⇧⌘4 |
| `spotlight` | ⌘Space |

#### `clipboard` — 剪贴板同步
```json
{"type": "clipboard", "action": "push", "content": "要复制的内容"}
{"type": "clipboard", "action": "pull"}
{"type": "clipboard", "action": "content", "content": "Mac 剪贴板内容"}  // 服务端回传
```

#### `mouse` — 鼠标操作
```json
{"type": "mouse", "action": "move", "dx": 10.5, "dy": -3.2}
{"type": "mouse", "action": "click", "button": "left"}
{"type": "mouse", "action": "double_click"}
{"type": "mouse", "action": "scroll", "dy": 3.0}
{"type": "mouse", "action": "drag_start"}
{"type": "mouse", "action": "drag_move", "dx": 5.0, "dy": 2.0}
{"type": "mouse", "action": "drag_end"}
```

## Mac 服务端模块

| 文件 | 职责 |
|------|------|
| `main.swift` | 应用入口，NSApplication 菜单栏应用，组装各组件 |
| `WebSocketServer.swift` | Swift NIO WebSocket 服务端，监听 9876 端口，管理客户端连接 |
| `MessageHandler.swift` | JSON 消息解析与分发，处理键盘/鼠标/剪贴板操作 |
| `KeySimulator.swift` | CGEvent 键盘模拟，Unicode 文本输入，按键映射，宏执行 |
| `Models.swift` | 消息数据模型定义（`Message`、`MessageType`） |
| `StatusBarController.swift` | macOS 菜单栏图标与状态菜单（连接状态、IP 显示、复制 IP） |
| `BonjourService.swift` | Bonjour/mDNS 服务发布（`_typesync._tcp.`） |

## Android 客户端模块

| 文件 | 职责 |
|------|------|
| `MainActivity.kt` | Activity 入口，主题状态管理 |
| `ui/MainScreen.kt` | 主界面：连接管理、文本输入框、剪贴板同步、Tab 导航 |
| `ui/KeyboardPanel.kt` | 快捷键面板：修饰键、编辑键、常用快捷键、宏、终端快捷键、F 键 |
| `ui/TouchpadPanel.kt` | 触控板面板：鼠标移动、单击/双击/右键/拖拽手势、滚轮 |
| `ui/TextSnippetsPanel.kt` | 文本片段面板：预设文本快速发送 |
| `ui/theme/Theme.kt` | Material 3 主题系统（亮/暗/跟随系统，Android 12+ 动态取色） |
| `model/Message.kt` | 消息数据模型与 JSON 序列化 |
| `network/WebSocketClient.kt` | OkHttp WebSocket 客户端，自动重连 |
| `network/NsdDiscovery.kt` | mDNS 服务发现（NsdManager） |

## 关键实现细节

### 文本输入
Android 端监听 TextField 变化，计算增量（新增字符 / 删除字符），发送对应的 `text` 或 `key(backspace)` 消息。Mac 端通过 `CGEvent.keyboardSetUnicodeString` 实现 Unicode 字符输入。

### 触控板拖拽手势
单一触控区域通过 `awaitPointerEventScope` 手动处理指针事件，实现：
- 普通滑动 → `mouse.move`
- 快速双击并拖动 → `drag_start` → `drag_move` → `drag_end`（300ms 内双击判定）
- 单击 → `mouse.click`
- 双击 → `mouse.double_click`
- 长按 → `mouse.click(right)`

Mac 端拖拽使用 `CGEventType.leftMouseDragged` 事件类型，区别于普通的 `mouseMoved`。

### 自动发现
Mac 服务端通过 Bonjour 发布 `_typesync._tcp.` 服务，Android 端通过 NsdManager 发现并显示为可点击的芯片，免去手动输入 IP。

### 主题系统
支持三种模式（亮色 / 暗色 / 跟随系统），TopAppBar 右侧按钮循环切换。Android 12+ 设备自动使用 Material You 动态取色，低版本使用自定义 Teal + Blue 配色方案。
