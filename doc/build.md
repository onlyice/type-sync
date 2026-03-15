# 构建指南

## Mac 服务端

### 环境要求

- macOS 13+ (Ventura 及以上)
- Swift 5.9+ (Xcode 15+ 或独立 Swift 工具链)

### 构建

```bash
cd mac-server
swift build
```

构建产物位于 `mac-server/.build/debug/TypeSyncServer`。

Release 构建：

```bash
swift build -c release
# 产物: mac-server/.build/release/TypeSyncServer
```

### 运行

```bash
# 前台运行
.build/debug/TypeSyncServer

# 后台运行
.build/debug/TypeSyncServer > /tmp/typesync-server.log 2>&1 &
```

### macOS 权限

首次运行需要授予 **辅助功能权限**（Accessibility），否则无法模拟键鼠输入：

**系统设置 → 隐私与安全性 → 辅助功能** → 添加 `TypeSyncServer`（或终端应用）

---

## Android 客户端

### 环境要求

- JDK 17+（推荐 JDK 25，路径示例：`/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home`）
- Android SDK（路径示例：`~/Library/Android/sdk`）
- Android SDK Platform 35 (API 35)
- 构建工具：AGP 9.0.1、Gradle 9.1.0、Kotlin Compose Plugin 2.2.10

### 构建 APK

```bash
cd android-client

# 设置环境变量（如果未在系统级配置）
export JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=~/Library/Android/sdk

# Debug APK
./gradlew assembleDebug
# 产物: app/build/outputs/apk/debug/app-debug.apk
```

### 安装到设备

#### 通过 USB

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

#### 通过无线调试 (Android 11+)

1. 手机开启 **设置 → 开发者选项 → 无线调试**
2. 点击 **使用配对码配对设备**，获取配对端口和配对码
3. 配对：
   ```bash
   adb pair <IP>:<配对端口> <配对码>
   # 例: adb pair 192.168.1.17:33647 901588
   ```
4. 获取无线调试页面顶部显示的**连接端口**（和配对端口不同）
5. 连接：
   ```bash
   adb connect <IP>:<连接端口>
   # 例: adb connect 192.168.1.17:44487
   ```
6. 安装：
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

#### 构建并直接安装（一步完成）

```bash
./gradlew installDebug
```

### 模拟器测试

使用 Android Studio 模拟器时，模拟器通过 `10.0.2.2` 访问宿主 Mac。应用默认 IP 地址已设为此值。

```bash
# 启动模拟器
emulator -avd Medium_Phone_API_36

# 构建并安装
./gradlew installDebug
```

### 注意事项

- Android 9+ 默认阻止明文 HTTP/WebSocket 连接，项目已在 `res/xml/network_security_config.xml` 中配置 `cleartextTrafficPermitted=true`
- 如果 `adb` 不在 PATH 中，使用完整路径：`~/Library/Android/sdk/platform-tools/adb`
