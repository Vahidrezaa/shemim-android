package com.shemim.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shemim.app.ui.SkeletonScreen
import com.shemim.core.designsystem.theme.ShemimTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ShemimTheme {
                SkeletonScreen()
            }
        }
    }
}
