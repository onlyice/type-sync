package com.typesync.client.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.typesync.client.model.Message
import com.typesync.client.network.NsdDiscovery
import com.typesync.client.network.WebSocketClient
import com.typesync.client.ui.theme.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class DiscoveredService(val name: String, val host: String, val port: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    wsClient: WebSocketClient,
    nsdDiscovery: NsdDiscovery?,
    themeMode: ThemeMode = ThemeMode.AUTO,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var ipAddress by remember { mutableStateOf("10.0.2.2") }
    var port by remember { mutableStateOf("9876") }
    var isConnected by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("未连接") }
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var discoveredServices by remember { mutableStateOf(listOf<DiscoveredService>()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var macClipboard by remember { mutableStateOf<String?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("typesync_settings", Context.MODE_PRIVATE) }
    var autoClearSeconds by remember { mutableIntStateOf(prefs.getInt("auto_clear_seconds", 0)) }

    // Auto-clear input after idle
    val coroutineScope = rememberCoroutineScope()
    var autoClearJob by remember { mutableStateOf<Job?>(null) }

    // Setup WebSocket callbacks
    LaunchedEffect(wsClient) {
        wsClient.onConnected = {
            isConnected = true
            statusText = "已连接 $ipAddress"
        }
        wsClient.onDisconnected = {
            isConnected = false
            statusText = "已断开"
        }
        wsClient.onError = { error ->
            statusText = error
        }
        wsClient.onMessage = { text ->
            val msg = Message.fromJson(text)
            if (msg?.type == "clipboard" && msg.action == "content") {
                macClipboard = msg.content
            }
        }
    }

    // Setup mDNS discovery
    LaunchedEffect(nsdDiscovery) {
        nsdDiscovery?.onServiceFound = { name, host, p ->
            val service = DiscoveredService(name, host, p)
            discoveredServices = (discoveredServices + service).distinctBy { it.host }
        }
        nsdDiscovery?.onServiceLost = { name ->
            discoveredServices = discoveredServices.filter { it.name != name }
        }
        nsdDiscovery?.startDiscovery()
    }

    DisposableEffect(nsdDiscovery) {
        onDispose {
            nsdDiscovery?.stopDiscovery()
        }
    }

    val sendMessage: (Message) -> Unit = { msg ->
        wsClient.send(msg.toJson())
    }

    // Settings dialog
    if (showSettingsDialog) {
        var tempSeconds by remember { mutableStateOf(autoClearSeconds.toString()) }
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("设置") },
            text = {
                Column {
                    Text("停止输入后自动清空输入框", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "设为 0 表示关闭此功能",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempSeconds,
                        onValueChange = { tempSeconds = it.filter { c -> c.isDigit() } },
                        label = { Text("秒数") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = tempSeconds.toIntOrNull() ?: 0
                    autoClearSeconds = value
                    prefs.edit().putInt("auto_clear_seconds", value).apply()
                    showSettingsDialog = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TypeSync") },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Text("⚙️")
                    }
                    IconButton(onClick = {
                        val next = when (themeMode) {
                            ThemeMode.AUTO -> ThemeMode.LIGHT
                            ThemeMode.LIGHT -> ThemeMode.DARK
                            ThemeMode.DARK -> ThemeMode.AUTO
                        }
                        onThemeModeChange(next)
                    }) {
                        Text(
                            when (themeMode) {
                                ThemeMode.AUTO -> "🌗"
                                ThemeMode.LIGHT -> "☀️"
                                ThemeMode.DARK -> "🌙"
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Connection section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = if (isConnected) "🟢" else "🔴")
                        Spacer(Modifier.width(8.dp))
                        Text(statusText)
                    }

                    if (!isConnected && discoveredServices.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("发现的设备：", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            discoveredServices.forEach { service ->
                                AssistChip(
                                    onClick = {
                                        ipAddress = service.host
                                        port = service.port.toString()
                                        wsClient.connect(service.host, service.port)
                                        statusText = "连接中..."
                                    },
                                    label = { Text(service.name) }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ipAddress,
                            onValueChange = { ipAddress = it },
                            label = { Text("IP 地址") },
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text("端口") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (isConnected) {
                                    wsClient.disconnect()
                                } else {
                                    val p = port.toIntOrNull() ?: 9876
                                    wsClient.connect(ipAddress, p)
                                    statusText = "连接中..."
                                }
                            }
                        ) {
                            Text(if (isConnected) "断开" else "连接")
                        }
                    }
                }
            }

            // Text input area
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    val oldText = textFieldValue.text
                    val newText = newValue.text
                    textFieldValue = newValue

                    if (isConnected && newText != oldText) {
                        if (newText.startsWith(oldText)) {
                            val delta = newText.substring(oldText.length)
                            if (delta.isNotEmpty()) {
                                sendMessage(Message.text(delta))
                            }
                        } else if (oldText.startsWith(newText)) {
                            val deleted = oldText.length - newText.length
                            repeat(deleted) {
                                sendMessage(Message.key("backspace"))
                            }
                        } else {
                            val commonPrefix = oldText.commonPrefixWith(newText)
                            val deleteCount = oldText.length - commonPrefix.length
                            val insertText = newText.substring(commonPrefix.length)
                            repeat(deleteCount) {
                                sendMessage(Message.key("backspace"))
                            }
                            if (insertText.isNotEmpty()) {
                                sendMessage(Message.text(insertText))
                            }
                        }
                    }

                    // Auto-clear after idle
                    if (autoClearSeconds > 0 && newText.isNotEmpty()) {
                        autoClearJob?.cancel()
                        autoClearJob = coroutineScope.launch {
                            delay(autoClearSeconds * 1000L)
                            textFieldValue = TextFieldValue("")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(vertical = 8.dp),
                label = { Text("在此输入文字（支持语音输入）") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.None),
            )

            // Action row: clear + clipboard sync
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        // Push phone clipboard to Mac
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = cm.primaryClip?.getItemAt(0)?.text?.toString()
                        if (!clip.isNullOrEmpty()) {
                            sendMessage(Message.clipboardPush(clip))
                        }
                    }) {
                        Text("📋→Mac")
                    }
                    TextButton(onClick = {
                        // Pull Mac clipboard to phone
                        sendMessage(Message.clipboardPull())
                    }) {
                        Text("Mac→📋")
                    }
                }
                TextButton(onClick = {
                    textFieldValue = TextFieldValue("")
                }) {
                    Text("清空输入框")
                }
            }

            // Show Mac clipboard content if pulled
            if (macClipboard != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Mac 剪贴板:", style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            macClipboard?.take(200) ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 4
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = {
                                macClipboard?.let { content ->
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("TypeSync", content)
                                    cm.setPrimaryClip(clip)
                                }
                            }) {
                                Text("复制到手机")
                            }
                            TextButton(onClick = { macClipboard = null }) {
                                Text("关闭")
                            }
                        }
                    }
                }
            }

            // Tab row for panels
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("快捷键", modifier = Modifier.padding(vertical = 12.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("触控板", modifier = Modifier.padding(vertical = 12.dp))
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Text("片段", modifier = Modifier.padding(vertical = 12.dp))
                }
            }

            // Tab content
            when (selectedTab) {
                0 -> KeyboardPanel(onSend = sendMessage)
                1 -> TouchpadPanel(onSend = sendMessage)
                2 -> TextSnippetsPanel(onSend = sendMessage)
            }
        }
    }
}

private fun String.commonPrefixWith(other: String): String {
    val minLen = minOf(this.length, other.length)
    for (i in 0 until minLen) {
        if (this[i] != other[i]) return this.substring(0, i)
    }
    return this.substring(0, minLen)
}
