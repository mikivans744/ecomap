package com.ecomap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ecomap.app.presentation.navigation.EcoMapNavHost
import com.ecomap.app.presentation.theme.EcoMapTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
        setContent {
            EcoMapTheme {
                EcoMapNavHost()
            }
        }
    }
}
