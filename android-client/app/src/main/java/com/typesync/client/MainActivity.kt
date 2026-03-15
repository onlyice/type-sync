package com.typesync.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.typesync.client.network.NsdDiscovery
import com.typesync.client.network.WebSocketClient
import com.typesync.client.ui.MainScreen
import com.typesync.client.ui.theme.ThemeMode
import com.typesync.client.ui.theme.TypeSyncTheme

class MainActivity : ComponentActivity() {
    private val wsClient = WebSocketClient()
    private var nsdDiscovery: NsdDiscovery? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nsdDiscovery = NsdDiscovery(this)
        setContent {
            var themeMode by remember { mutableStateOf(ThemeMode.AUTO) }

            TypeSyncTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        wsClient = wsClient,
                        nsdDiscovery = nsdDiscovery,
                        themeMode = themeMode,
                        onThemeModeChange = { themeMode = it }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        nsdDiscovery?.stopDiscovery()
        wsClient.disconnect()
    }
}
