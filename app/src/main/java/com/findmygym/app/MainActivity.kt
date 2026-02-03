package com.findmygym.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.findmygym.app.nav.AppNav
import com.findmygym.app.ui.theme.FindMyGymTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FindMyGymTheme {
                AppNav()
            }
        }
    }
}