package com.typesync.client.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.typesync.client.model.Message

@Composable
fun TouchpadPanel(
    onSend: (Message) -> Unit,
    modifier: Modifier = Modifier
) {
    val sensitivity = 1.5f
    // Track whether we're in a drag (double-tap-and-hold then move)
    var isDragging by remember { mutableStateOf(false) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "触控板",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 4.dp)
        )

        // Touchpad area — single surface handling both move and drag
        // Double-tap then hold+drag = drag gesture
        // Single drag = mouse move
        // Single tap = left click
        // Long press = right click
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pointerInput(Unit) {
                    val doubleTapThreshold = 300L // ms
                    val tapSlopPx = 20f

                    awaitPointerEventScope {
                        while (true) {
                            // Wait for first press
                            val downEvent = awaitPointerEvent()
                            if (downEvent.type != PointerEventType.Press) continue
                            val downChange = downEvent.changes.firstOrNull() ?: continue
                            val downTime = downChange.uptimeMillis
                            val downPos = downChange.position

                            val timeSinceLastTap = downTime - lastTapTime
                            val isDoubleTapStart = timeSinceLastTap < doubleTapThreshold

                            var totalDragX = 0f
                            var totalDragY = 0f
                            var hasMoved = false
                            var dragStarted = false

                            // Track movement until release
                            var prevPos = downPos
                            var released = false
                            var longPressHandled = false

                            while (!released) {
                                val event = withTimeoutOrNull(500L) {
                                    awaitPointerEvent()
                                }

                                if (event == null) {
                                    // Timeout = long press (only if no movement)
                                    if (!hasMoved && !longPressHandled) {
                                        longPressHandled = true
                                        onSend(Message.mouseClick("right"))
                                    }
                                    continue
                                }

                                val change = event.changes.firstOrNull() ?: continue

                                if (event.type == PointerEventType.Release) {
                                    released = true
                                    if (dragStarted) {
                                        onSend(Message.mouseDragEnd())
                                        isDragging = false
                                    }
                                    change.consume()
                                    break
                                }

                                if (event.type == PointerEventType.Move) {
                                    val dx = change.position.x - prevPos.x
                                    val dy = change.position.y - prevPos.y
                                    prevPos = change.position
                                    totalDragX += dx
                                    totalDragY += dy

                                    if (!hasMoved) {
                                        val totalDist = kotlin.math.sqrt(totalDragX * totalDragX + totalDragY * totalDragY)
                                        if (totalDist > tapSlopPx) {
                                            hasMoved = true
                                            if (isDoubleTapStart) {
                                                dragStarted = true
                                                isDragging = true
                                                onSend(Message.mouseDragStart())
                                            }
                                        }
                                    }

                                    if (hasMoved) {
                                        val sdx = (dx * sensitivity).toDouble()
                                        val sdy = (dy * sensitivity).toDouble()
                                        if (dragStarted) {
                                            onSend(Message.mouseDragMove(sdx, sdy))
                                        } else {
                                            onSend(Message.mouseMove(sdx, sdy))
                                        }
                                    }

                                    change.consume()
                                }
                            }

                            if (!hasMoved && !longPressHandled) {
                                // It's a tap
                                lastTapTime = downTime
                                // Check if this is the second tap of a double-tap (no drag)
                                if (isDoubleTapStart) {
                                    onSend(Message.mouseDoubleClick())
                                    lastTapTime = 0L // reset so next tap isn't triple
                                } else {
                                    onSend(Message.mouseClick("left"))
                                }
                            } else if (hasMoved) {
                                lastTapTime = 0L
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "滑动 = 移动鼠标\n单击 = 左键　双击 = 双击\n长按 = 右键　双击拖动 = 拖拽",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        // Mouse buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onSend(Message.mouseClick("left")) },
                modifier = Modifier.weight(1f).height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text("左键", fontSize = 13.sp, maxLines = 1)
            }
            Button(
                onClick = { onSend(Message.mouseClick("right")) },
                modifier = Modifier.weight(1f).height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text("右键", fontSize = 13.sp, maxLines = 1)
            }
            Button(
                onClick = { onSend(Message.mouseScroll(-3.0)) },
                modifier = Modifier.weight(1f).height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text("↑滚", fontSize = 13.sp, maxLines = 1)
            }
            Button(
                onClick = { onSend(Message.mouseScroll(3.0)) },
                modifier = Modifier.weight(1f).height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text("↓滚", fontSize = 13.sp, maxLines = 1)
            }
        }
    }
}
