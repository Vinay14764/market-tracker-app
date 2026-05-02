package com.example.markettracker.ui.screens.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.markettracker.domain.model.Coin
import com.example.markettracker.ui.theme.TextPrimary

/**
 * Dashboard section showing coins with the biggest 24h price decrease.
 *
 * WHAT CHANGED:
 *   Old: Hardcoded "Cardano -0.99%" and "Ripple -1.20%" — never updated.
 *   New: Receives real [coins] from [DashboardViewModel] via [GetTopLosersUseCase].
 *
 * Reuses [DashboardCoinItem] from [TopGainersSection.kt] — same card design,
 * but the change percentage will be negative (shown in red automatically).
 *
 * @param coins The top loser [Coin]s from [GetTopLosersUseCase].
 *              Empty list = no data yet.
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
            // Show placeholder while data hasn't loaded yet
            Text(
                text     = "Loading market data...",
                color    = TextPrimary.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        } else {
            // Show real coins — [DashboardCoinItem] handles color (red for negative change)
            coins.forEach { coin ->
                DashboardCoinItem(coin = coin)
            }
        }
    }
}