package com.typesync.client.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.*
import java.util.concurrent.TimeUnit

private const val TAG = "TypeSyncWS"

class WebSocketClient {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var lastHost: String? = null
    private var lastPort: Int? = null
    private var autoReconnect = true
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 10
    private val handler = Handler(Looper.getMainLooper())
    private var isManualDisconnect = false

    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onMessage: ((String) -> Unit)? = null

    val isConnected: Boolean
        get() = webSocket != null && !isManualDisconnect

    fun connect(host: String, port: Int) {
        // Silently close old connection without triggering reconnect
        handler.removeCallbacksAndMessages(null)
        webSocket?.close(1000, "Reconnecting")
        webSocket = null

        isManualDisconnect = false
        lastHost = host
        lastPort = port
        reconnectAttempts = 0
        doConnect(host, port)
    }

    private fun doConnect(host: String, port: Int) {
        val url = "ws://$host:$port/"
        Log.d(TAG, "Connecting to $url")
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Connected! response=$response")
                reconnectAttempts = 0
                handler.post { onConnected?.invoke() }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Message: $text")
                handler.post { onMessage?.invoke(text) }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Closed: code=$code reason=$reason")
                handler.post { onDisconnected?.invoke() }
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Failure: ${t.message}", t)
                handler.post {
                    onError?.invoke(t.message ?: "Connection failed")
                    onDisconnected?.invoke()
                }
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (isManualDisconnect || !autoReconnect) return
        val host = lastHost ?: return
        val port = lastPort ?: return
        if (reconnectAttempts >= maxReconnectAttempts) {
            handler.post { onError?.invoke("达到最大重连次数") }
            return
        }
        reconnectAttempts++
        val delay = minOf(reconnectAttempts * 2000L, 10000L) // 2s, 4s, 6s... max 10s
        handler.postDelayed({
            if (!isManualDisconnect) {
                handler.post { onError?.invoke("正在重连 ($reconnectAttempts)...") }
                doConnect(host, port)
            }
        }, delay)
    }

    fun send(text: String) {
        webSocket?.send(text)
    }

    fun disconnect() {
        isManualDisconnect = true
        handler.removeCallbacksAndMessages(null)
        webSocket?.close(1000, "User disconnect")
        webSocket = null
    }
}
