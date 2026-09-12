package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.di.AppContainer
import com.example.ui.navigation.NocApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = AppContainer.getInstance(applicationContext)

        setContent {
            MyApplicationTheme {
                NocApp(
                    container = container,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

