package com.example.qrorderkitchen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.qrorderkitchen.ui.navigation.AppNavigation
import com.example.qrorderkitchen.ui.theme.QROrderKitchenTheme
import com.example.qrorderkitchen.ui.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        com.example.qrorderkitchen.utils.NetworkTimeProvider.sync()

        authViewModel = AuthViewModel(this)

        setContent {
            QROrderKitchenTheme {
                AppNavigation(authViewModel = authViewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing && ::authViewModel.isInitialized) {
            authViewModel.performExitCleanup()
        }
    }
}
