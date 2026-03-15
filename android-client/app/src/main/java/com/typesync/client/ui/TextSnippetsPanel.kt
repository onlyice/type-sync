package com.typesync.client.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typesync.client.model.Message

data class TextSnippet(val label: String, val content: String)

@Composable
fun TextSnippetsPanel(
    onSend: (Message) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var snippets by remember {
        mutableStateOf(
            listOf(
                TextSnippet("邮箱", "your@email.com"),
                TextSnippet("地址", "请替换为你的地址"),
                TextSnippet("签名", "Best regards,\nYour Name"),
                TextSnippet("分割线", "────────────────"),
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "文本片段",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
            TextButton(onClick = { showAddDialog = true }) {
                Text("+ 添加")
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(snippets) { snippet ->
                AssistChip(
                    onClick = { onSend(Message.text(snippet.content)) },
                    label = { Text(snippet.label, fontSize = 13.sp) }
                )
            }
        }
    }

    if (showAddDialog) {
        var label by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加文本片段") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("名称") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("内容") },
                        minLines = 2,
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (label.isNotBlank() && content.isNotBlank()) {
                            snippets = snippets + TextSnippet(label, content)
                            showAddDialog = false
                        }
                    }
                ) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("取消") }
            }
        )
    }
}
