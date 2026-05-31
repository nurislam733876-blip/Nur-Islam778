package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.Modifier
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.VaultScreen
import com.example.viewmodel.VaultViewModel
import com.example.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: VaultViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Instantiate ViewModel with Custom Factory for Room context
        viewModel = ViewModelProvider(this, ViewModelFactory(applicationContext))[VaultViewModel::class.java]
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) { // Always force premium dark theme
                val currentScreen by viewModel.currentScreen.collectAsState()
                
                // Block/Intercept System Back Key when inside the secure Vault to maintain isolation
                BackHandler(enabled = currentScreen != VaultScreen.Calculator) {
                    val popped = viewModel.navigateBack()
                    if (!popped) {
                        viewModel.lockVault()
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        when (currentScreen) {
                            VaultScreen.Calculator -> {
                                CalculatorScreen(viewModel = viewModel)
                            }
                            VaultScreen.VaultDashboard -> {
                                VaultDashboardScreen(viewModel = viewModel)
                            }
                            VaultScreen.PhotoViewer -> {
                                PhotoViewerScreen(viewModel = viewModel)
                            }
                            VaultScreen.PhotoEditor -> {
                                PhotoEditorScreen(viewModel = viewModel)
                            }
                            VaultScreen.VideoPlayer -> {
                                VideoPlayerScreen(viewModel = viewModel)
                            }
                            VaultScreen.HiddenAppManager -> {
                                HiddenAppManagerScreen(viewModel = viewModel)
                            }
                            else -> {
                                CalculatorScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
