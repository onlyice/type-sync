package com.typesync.client.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typesync.client.model.Message

@Composable
fun KeyboardPanel(
    onSend: (Message) -> Unit,
    modifier: Modifier = Modifier
) {
    var ctrlOn by remember { mutableStateOf(false) }
    var altOn by remember { mutableStateOf(false) }
    var cmdOn by remember { mutableStateOf(false) }
    var shiftOn by remember { mutableStateOf(false) }
    var showFKeys by remember { mutableStateOf(false) }

    fun currentModifiers(): List<String> {
        val mods = mutableListOf<String>()
        if (ctrlOn) mods.add("ctrl")
        if (altOn) mods.add("alt")
        if (cmdOn) mods.add("cmd")
        if (shiftOn) mods.add("shift")
        return mods
    }

    fun sendKeyAndReset(key: String) {
        val mods = currentModifiers()
        onSend(Message.key(key, mods))
        ctrlOn = false
        altOn = false
        cmdOn = false
        shiftOn = false
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Modifier toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ModifierToggle("Ctrl", "⌃", ctrlOn, Modifier.weight(1f)) { ctrlOn = !ctrlOn }
            ModifierToggle("Alt", "⌥", altOn, Modifier.weight(1f)) { altOn = !altOn }
            ModifierToggle("Cmd", "⌘", cmdOn, Modifier.weight(1f)) { cmdOn = !cmdOn }
            ModifierToggle("Shift", "⇧", shiftOn, Modifier.weight(1f)) { shiftOn = !shiftOn }
        }

        // Special keys
        SectionLabel("编辑键")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            KeyButton("Esc", null, Modifier.weight(1f)) { sendKeyAndReset("escape") }
            KeyButton("Tab", "⇥", Modifier.weight(1f)) { sendKeyAndReset("tab") }
            KeyButton("退格", "⌫", Modifier.weight(1f)) { sendKeyAndReset("backspace") }
            KeyButton("Del", "⌦", Modifier.weight(1f)) { sendKeyAndReset("delete") }
            KeyButton("空格", "␣", Modifier.weight(1f)) { sendKeyAndReset("space") }
        }

        // Arrow keys + Home/End
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            KeyButton("Home", "↖", Modifier.weight(1f)) { sendKeyAndReset("home") }
            KeyButton("←", null, Modifier.weight(1f)) { sendKeyAndReset("left") }
            KeyButton("↑", null, Modifier.weight(1f)) { sendKeyAndReset("up") }
            KeyButton("↓", null, Modifier.weight(1f)) { sendKeyAndReset("down") }
            KeyButton("→", null, Modifier.weight(1f)) { sendKeyAndReset("right") }
            KeyButton("End", "↘", Modifier.weight(1f)) { sendKeyAndReset("end") }
        }

        // Enter + PgUp/PgDn
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            KeyButton("Enter", "↵", Modifier.weight(2f)) { sendKeyAndReset("return") }
            KeyButton("PgUp", "⇞", Modifier.weight(1f)) { sendKeyAndReset("pageup") }
            KeyButton("PgDn", "⇟", Modifier.weight(1f)) { sendKeyAndReset("pagedown") }
        }

        Divider(modifier = Modifier.padding(vertical = 2.dp))

        // Common shortcuts
        SectionLabel("常用快捷键")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ShortcutButton("撤销", "⌘Z", Modifier.weight(1f)) { onSend(Message.key("z", listOf("cmd"))) }
            ShortcutButton("重做", "⇧⌘Z", Modifier.weight(1f)) { onSend(Message.key("z", listOf("cmd", "shift"))) }
            ShortcutButton("剪切", "⌘X", Modifier.weight(1f)) { onSend(Message.key("x", listOf("cmd"))) }
            ShortcutButton("复制", "⌘C", Modifier.weight(1f)) { onSend(Message.key("c", listOf("cmd"))) }
            ShortcutButton("粘贴", "⌘V", Modifier.weight(1f)) { onSend(Message.key("v", listOf("cmd"))) }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ShortcutButton("保存", "⌘S", Modifier.weight(1f)) { onSend(Message.key("s", listOf("cmd"))) }
            ShortcutButton("搜索", "⌘F", Modifier.weight(1f)) { onSend(Message.key("f", listOf("cmd"))) }
            ShortcutButton("新建", "⌘N", Modifier.weight(1f)) { onSend(Message.key("n", listOf("cmd"))) }
            ShortcutButton("关闭", "⌘W", Modifier.weight(1f)) { onSend(Message.key("w", listOf("cmd"))) }
        }

        // Macros
        SectionLabel("宏操作")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MacroButton("清空重输", "⌘A ⌫", Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ) { onSend(Message.macro("clear_input")) }
            MacroButton("全选复制", "⌘A ⌘C", Modifier.weight(1f)) { onSend(Message.macro("select_all_copy")) }
            MacroButton("全选粘贴", "⌘A ⌘V", Modifier.weight(1f)) { onSend(Message.macro("select_all_paste")) }
        }

        Divider(modifier = Modifier.padding(vertical = 2.dp))

        // Terminal shortcuts
        SectionLabel("终端快捷键")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TerminalButton("^C", "中断", Modifier.weight(1f)) { onSend(Message.key("c", listOf("ctrl"))) }
            TerminalButton("^D", "EOF", Modifier.weight(1f)) { onSend(Message.key("d", listOf("ctrl"))) }
            TerminalButton("^Z", "挂起", Modifier.weight(1f)) { onSend(Message.key("z", listOf("ctrl"))) }
            TerminalButton("^L", "清屏", Modifier.weight(1f)) { onSend(Message.key("l", listOf("ctrl"))) }
            TerminalButton("^R", "搜索", Modifier.weight(1f)) { onSend(Message.key("r", listOf("ctrl"))) }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TerminalButton("^A", "行首", Modifier.weight(1f)) { onSend(Message.key("a", listOf("ctrl"))) }
            TerminalButton("^E", "行尾", Modifier.weight(1f)) { onSend(Message.key("e", listOf("ctrl"))) }
            TerminalButton("^U", "删至首", Modifier.weight(1f)) { onSend(Message.key("u", listOf("ctrl"))) }
            TerminalButton("^K", "删至尾", Modifier.weight(1f)) { onSend(Message.key("k", listOf("ctrl"))) }
            TerminalButton("^W", "删词", Modifier.weight(1f)) { onSend(Message.key("w", listOf("ctrl"))) }
        }

        // F-keys expandable section
        TextButton(onClick = { showFKeys = !showFKeys }) {
            Text(if (showFKeys) "▲ 收起功能键" else "▼ 功能键 F1–F12")
        }

        if (showFKeys) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                (1..6).forEach { n ->
                    KeyButton("F$n", null, Modifier.weight(1f)) { sendKeyAndReset("f$n") }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                (7..12).forEach { n ->
                    KeyButton("F$n", null, Modifier.weight(1f)) { sendKeyAndReset("f$n") }
                }
            }
        }
    }
}

// -- Section label --

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
    )
}

// -- Modifier toggle button --

@Composable
private fun ModifierToggle(
    label: String,
    symbol: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(4.dp)
    ) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text(symbol, fontSize = 16.sp)
            Text(label, fontSize = 10.sp)
        }
    }
}

// -- Key button (navigation/editing keys) --

@Composable
private fun KeyButton(
    label: String,
    symbol: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(2.dp)
    ) {
        if (symbol != null) {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text(symbol, fontSize = 16.sp)
                Text(label, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        } else {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

// -- Shortcut button (⌘ combos) --

@Composable
private fun ShortcutButton(
    label: String,
    shortcut: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        contentPadding = PaddingValues(2.dp)
    ) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(
                shortcut,
                fontSize = 9.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

// -- Macro button --

@Composable
private fun MacroButton(
    label: String,
    steps: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onTertiaryContainer,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(
                steps,
                fontSize = 9.sp,
                maxLines = 1,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}

// -- Terminal button --

@Composable
private fun TerminalButton(
    combo: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ),
        contentPadding = PaddingValues(2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text(
                combo,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Text(
                description,
                fontSize = 9.sp,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
