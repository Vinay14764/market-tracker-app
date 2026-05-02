package com.example.markettracker.ui.screens.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun TopBar() {
    Text(
        text = "Home",
        style = MaterialTheme.typography.displayMedium,
        color = Color.White
    )
}
