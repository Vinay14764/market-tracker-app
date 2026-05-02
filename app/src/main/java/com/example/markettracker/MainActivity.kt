package com.example.markettracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.markettracker.navigation.NavGraph
import com.example.markettracker.ui.theme.CryptoAppTheme

/**
 * The app's single Activity. All screens are Composables managed by NavGraph.
 *
 * @AndroidEntryPoint tells Hilt to inject dependencies into this Activity.
 * Without this annotation, hiltViewModel() in Composables inside this Activity won't work.
 */
@dagger.hilt.android.AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CryptoAppTheme {
                NavGraph()
            }
        }
    }
}
