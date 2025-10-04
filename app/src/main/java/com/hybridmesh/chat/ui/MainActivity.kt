package com.hybridmesh.chat.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hybridmesh.chat.ui.screens.ChatScreen
import com.hybridmesh.chat.ui.screens.DeviceListScreen
import com.hybridmesh.chat.ui.theme.HybridMeshChatTheme
import com.hybridmesh.chat.ui.viewmodel.ChatViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) 
            // All permissions granted, start the app dhruvpatel
        } else {
            // Handle permission denial
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request permissions
        requestPermissions()
        
        setContent {
            HybridMeshChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HybridMeshApp()
                }
            }
        }
    }
    
    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
        )
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}

@Composable
fun HybridMeshApp() {
    val viewModel: ChatViewModel = koinViewModel()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.DeviceList) }
    
    when (currentScreen) {
        is Screen.DeviceList -> {
            DeviceListScreen(
                viewModel = viewModel,
                onDeviceClick = { device ->
                    viewModel.selectDevice(device)
                    currentScreen = Screen.Chat
                }
            )
        }
        is Screen.Chat -> {
            ChatScreen(
                viewModel = viewModel,
                onBackClick = {
                    currentScreen = Screen.DeviceList
                }
            )
        }
    }
}

sealed class Screen {
    object DeviceList : Screen()
    object Chat : Screen()
}
