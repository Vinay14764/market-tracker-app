package com.example.markettracker.ui.screens.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.markettracker.domain.model.Coin
import com.example.markettracker.ui.components.DashboardSectionShimmer
import com.example.markettracker.ui.theme.TextPrimary

/**
 * Dashboard section showing coins with the biggest 24h price decrease.
 *
 */
@Composable
fun TopLosersSection(coins: List<Coin>) {
    Column {
        Text(
            text  = "Top Losers",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (coins.isEmpty()) {
            DashboardSectionShimmer()
        } else {
            // Show real coins — [DashboardCoinItem] handles color (red for negative change)
            coins.forEach { coin ->
                DashboardCoinItem(coin = coin)
            }
        }
    }
}