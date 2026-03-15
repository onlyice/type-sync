# TypeSync

TypeSync 是一个将 Android 手机变成 Mac 远程键盘/触控板的工具，特别适合利用 Android 的语音输入能力在 Mac 上高效输入文字。

## 项目组成

| 组件 | 目录 | 技术栈 |
|------|------|--------|
| Mac 服务端 | `mac-server/` | Swift, Swift NIO (WebSocket), CGEvent, Bonjour/mDNS |
| Android 客户端 | `android-client/` | Kotlin, Jetpack Compose, OkHttp WebSocket, NsdManager |

## 文档索引

- [架构与实现](./architecture.md) — 系统架构、通信协议、模块说明
- [构建指南](./build.md) — 环境要求、构建与安装步骤
- [使用指南](./usage.md) — 功能说明与操作方法
