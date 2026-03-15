package com.typesync.client.model

import org.json.JSONObject

data class Message(
    val type: String,
    val content: String? = null,
    val key: String? = null,
    val modifiers: List<String>? = null,
    val name: String? = null,
    val action: String? = null,
    val dx: Double? = null,
    val dy: Double? = null,
    val button: String? = null,
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("type", type)
        content?.let { json.put("content", it) }
        key?.let { json.put("key", it) }
        modifiers?.let { json.put("modifiers", org.json.JSONArray(it)) }
        name?.let { json.put("name", it) }
        action?.let { json.put("action", it) }
        dx?.let { json.put("dx", it) }
        dy?.let { json.put("dy", it) }
        button?.let { json.put("button", it) }
        return json.toString()
    }

    companion object {
        fun text(content: String) = Message(type = "text", content = content)
        fun key(key: String, modifiers: List<String> = emptyList()) = Message(type = "key", key = key, modifiers = modifiers)
        fun macro(name: String) = Message(type = "macro", name = name)
        fun clipboardPush(content: String) = Message(type = "clipboard", action = "push", content = content)
        fun clipboardPull() = Message(type = "clipboard", action = "pull")
        fun mouseMove(dx: Double, dy: Double) = Message(type = "mouse", action = "move", dx = dx, dy = dy)
        fun mouseClick(button: String = "left") = Message(type = "mouse", action = "click", button = button)
        fun mouseDoubleClick() = Message(type = "mouse", action = "double_click")
        fun mouseScroll(dy: Double) = Message(type = "mouse", action = "scroll", dy = dy)
        fun mouseDragStart() = Message(type = "mouse", action = "drag_start")
        fun mouseDragMove(dx: Double, dy: Double) = Message(type = "mouse", action = "drag_move", dx = dx, dy = dy)
        fun mouseDragEnd() = Message(type = "mouse", action = "drag_end")

        fun fromJson(json: String): Message? {
            return try {
                val obj = JSONObject(json)
                Message(
                    type = obj.getString("type"),
                    content = obj.optString("content", null),
                    action = obj.optString("action", null),
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
