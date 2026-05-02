package com.example.markettracker.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.markettracker.domain.model.Coin
import com.example.markettracker.ui.theme.CardBackground
import com.example.markettracker.ui.theme.DarkBackground
import com.example.markettracker.ui.theme.PrimaryGreen
import com.example.markettracker.ui.theme.TextPrimary
import java.text.NumberFormat
import java.util.Locale

/**
 * Dashboard section showing coins with the highest 24h price increase.
 *
 * WHAT CHANGED:
 *   Old: Hardcoded "Bitcoin +3.43%" and "Doge +3.28%" — never updated.
 *   New: Receives [coins] from [DashboardViewModel] which reads real data from Room.
 *
 * @param coins The top gainer [Coin]s from [GetTopGainersUseCase].
 *              Empty list = no data yet (API not fetched or no gainers today).
 */
@Composable
fun TopGainersSection(coins: List<Coin>) {
    Column {
        Text(
            text  = "Top Gainers",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (coins.isEmpty()) {
            // Show placeholder while data hasn't loaded yet
            Text(
                text  = "Loading market data...",
                color = TextPrimary.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        } else {
            // Show real coins from the API
            coins.forEach { coin ->
                DashboardCoinItem(coin = coin)
            }
        }
    }
}

/**
 * A single coin row used in both the Top Gainers and Top Losers sections.
 *
 * Shows: coin logo, name, current price, and 24h price change percentage.
 * Green = positive change, Red = negative change.
 *
 * @param coin The [Coin] domain model to display.
 */
@Composable
fun DashboardCoinItem(coin: Coin) {
    // Determine if the coin went up or down
    val isPositive = coin.priceChangePercent24h >= 0
    val changeColor = if (isPositive) PrimaryGreen else com.example.markettracker.ui.theme.PrimaryRed

    // Format price in Indian locale (e.g. ₹6,56,985)
    val formattedPrice = NumberFormat
        .getNumberInstance(Locale("en", "IN"))
        .format(coin.currentPrice)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Coin logo from CoinGecko image URL
                Box(
                    modifier          = Modifier.size(40.dp).background(DarkBackground, CircleShape),
                    contentAlignment  = Alignment.Center
                ) {
                    AsyncImage(
                        model              = coin.imageUrl,
                        contentDescription = "${coin.name} logo",
                        modifier           = Modifier.size(36.dp).clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))

                // Coin name — truncated with "..." if too long to prevent price column wrapping
                Text(
                    text     = coin.name,
                    color    = TextPrimary,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Price + change percentage
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "₹$formattedPrice", color = TextPrimary, fontSize = 16.sp)
                Text(
                    text  = "${"%.2f".format(coin.priceChangePercent24h)}%",
                    color = changeColor
                )
            }
        }
    }
}