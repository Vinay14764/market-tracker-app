package com.example.markettracker.ui.screens.market

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.markettracker.ui.components.StarfieldBackground
import com.example.markettracker.ui.theme.Background
import com.example.markettracker.ui.theme.CardBackground
import com.example.markettracker.ui.theme.DepositColor
import com.example.markettracker.ui.theme.PrimaryGreen
import com.example.markettracker.ui.theme.PrimaryRed
import com.example.markettracker.ui.theme.TextPrimary
import com.example.markettracker.ui.theme.TextSecondary
import com.example.markettracker.ui.theme.WithdrawColor
import java.text.NumberFormat
import java.util.Locale

/**
 * Detail screen for a single cryptocurrency coin.
 *
 * WHAT CHANGED from the old version:
 *   Old: Hardcoded "Bitcoin" image, "63.743344" price, "+52%" change — always the same.
 *   New: Reads real data from Room via [CoinDetailViewModel].
 *        Shows the actual coin the user tapped, with its live price and change percentage.
 *
 * HOW COIN ID IS PASSED:
 *   The [coinId] parameter comes from the navigation route ("coin_detail/{coinId}").
 *   [hiltViewModel] creates [CoinDetailViewModel] which reads [coinId] from SavedStateHandle
 *   automatically — Jetpack Navigation Compose puts nav args into SavedStateHandle for us.
 *
 * @param coinId The CoinGecko ID of the coin to display (e.g. "bitcoin", "ethereum").
 *               Only used to navigate here — the ViewModel reads it from SavedStateHandle.
 */

@Composable
fun CoinDetailScreen(coinId: String) {

    // hiltViewModel() creates the ViewModel scoped to this NavBackStackEntry.
    // SavedStateHandle inside the ViewModel has "coinId" populated automatically.
    val viewModel: CoinDetailViewModel = hiltViewModel()

    // Collect the UI state
    val state by viewModel.state.collectAsState()

    // StarfieldBackground fills the screen first; the Column is layered on top.
    // The Column background is transparent so the starfield shows through.
    Box(modifier = Modifier.fillMaxSize()) {
        StarfieldBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(16.dp)
        ) {
        when {
            // ── Loading state ──────────────────────────────────────────────
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Loading...", color = TextSecondary)
                }
            }

            // ── Error state ────────────────────────────────────────────────
            state.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.error ?: "Unknown error", color = PrimaryRed)
                }
            }

            // ── Coin loaded — show real data ───────────────────────────────
            state.coin != null -> {
                val coin = state.coin!!  // Safe because we checked for null above

                // Whether the price went up or down — determines color
                val isPositive = coin.priceChangePercent24h >= 0
                val changeColor = if (isPositive) PrimaryGreen else PrimaryRed

                // Format the price in Indian locale (e.g. "6,56,985.23")
                val formattedPrice = NumberFormat
                    .getNumberInstance(Locale("en", "IN"))
                    .format(coin.currentPrice)

                    // ── Coin Header: Logo + Name + Symbol ──
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = coin.imageUrl,
                            contentDescription = "${coin.name} icon",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = coin.name,
                                color = TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = coin.symbol.uppercase(),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Price + Change ──
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "₹$formattedPrice",
                            color = changeColor,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${"%.2f".format(coin.priceChangePercent24h)}%",
                            color = changeColor,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(CardBackground, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val basePrices = viewModel.getChartData()

                        val isPositive = coin.priceChangePercent24h >= 0

                        // 🔥 Fix 1: Match chart direction with change
                        val prices = if (isPositive) {
                            basePrices
                        } else {
                            basePrices.reversed()
                        }
                        val progress by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = tween(800)
                        )
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(CardBackground, RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 16.dp)
                        ) {

                            val maxPrice = prices.maxOrNull() ?: 0f
                            val minPrice = prices.minOrNull() ?: 0f

                            val stepX = size.width / (prices.size - 1)

                            val padding = 20f

                            val points = prices.mapIndexed { index, price ->

                                val x = index * stepX * progress

                                val normalized =
                                    (price - minPrice) / (maxPrice - minPrice + 0.0001f)

                                val y = padding + (size.height - 2 * padding) -
                                        (normalized * (size.height - 2 * padding))

                                Offset(x, y)
                            }

                            // 🔥 Smooth Curve
                            val path = Path()

                            points.forEachIndexed { index, point ->
                                if (index == 0) {
                                    path.moveTo(point.x, point.y)
                                } else {
                                    val prev = points[index - 1]
                                    val midX = (prev.x + point.x) / 2

                                    path.cubicTo(
                                        midX, prev.y,
                                        midX, point.y,
                                        point.x, point.y
                                    )
                                }
                            }

                            val chartColor = if (isPositive) PrimaryGreen else PrimaryRed

                            // 🔥 Glow effect (draw behind)
                            drawPath(
                                path = path,
                                color = chartColor.copy(alpha = 0.2f),
                                style = Stroke(width = 8f)
                            )

                            // 🔥 Main line (reduced thickness)
                            drawPath(
                                path = path,
                                color = chartColor,
                                style = Stroke(width = 3f)
                            )

                            // 🔥 Gradient fill (FIXED)
                            val fillPath = Path().apply { addPath(path) }

                            fillPath.lineTo(size.width, size.height)
                            fillPath.lineTo(0f, size.height)
                            fillPath.close()

                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        chartColor.copy(alpha = 0.25f), // softer
                                        Color.Transparent
                                    )
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Timeframe Selector Buttons (15m, 30m, 1H, 4H, 1D) ──
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("15m", "30m", "1H", "4H", "1D").forEach { timeframe ->
                            val isSelected = state.selectedTimeframe == timeframe
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) PrimaryGreen else CardBackground,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.onTimeSelected(timeframe) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                // Tapping a timeframe updates the selected state
                                Text(
                                    text = timeframe,
                                    color = TextPrimary,
                                    modifier = Modifier.run {
                                        if (!isSelected) this else this
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "About ${coin.name}",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    when {
                        state.isDescriptionLoading -> {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = PrimaryGreen,
                                trackColor = CardBackground
                            )
                        }

                        state.description != null -> {
                            Text(
                                text = state.description!!,
                                color = TextSecondary,
                                fontSize = 14.sp,
                                lineHeight = 22.sp
                            )
                        }

                        else -> {
                            Text(
                                "No description available.",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }


                    Spacer(modifier = Modifier.height(40.dp))
                }

                // ── BUY / SELL buttons ──
                // TODO: Connect to real trading or portfolio logic
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* TODO: handle buy */ },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, DepositColor),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(text = "BUY", color = DepositColor)
                    }
                    OutlinedButton(
                        onClick = { /* TODO: handle sell */ },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, WithdrawColor),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(text = "SELL", color = WithdrawColor)
                    }
                }
            }
        }
        }   // Column
    }   // Box (StarfieldBackground wrapper)
}