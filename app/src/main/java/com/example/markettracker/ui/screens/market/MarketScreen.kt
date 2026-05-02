package com.example.markettracker.ui.screens.market

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.markettracker.R
import com.example.markettracker.domain.model.Coin
import com.example.markettracker.domain.model.MarketFilter
import com.example.markettracker.ui.theme.Background
import com.example.markettracker.ui.theme.CardBackground
import com.example.markettracker.ui.theme.DarkBackground
import com.example.markettracker.ui.theme.DarkCard
import com.example.markettracker.ui.theme.NeonGreen
import com.example.markettracker.ui.theme.PrimaryGreen
import com.example.markettracker.ui.theme.PrimaryRed
import com.example.markettracker.ui.theme.TextPrimary
import com.example.markettracker.ui.theme.TextSecondary
import com.example.markettracker.ui.viewmodel.MarketViewModel
import java.text.NumberFormat
import java.util.Locale

/**
 * Root composable for the Market screen.
 *
 * Uses [hiltViewModel] to get [MarketViewModel] — no manual factory needed.
 * Collects the unified [MarketState] and dispatches [MarketIntent]s back to the ViewModel.
 *
 * Effect collection: listens for one-shot navigation events from [MarketViewModel.effect].
 * Navigation is handled here (not in the ViewModel) because navigation requires [NavController].
 */
@Composable
fun MarketScreen(navController: NavController) {

    // hiltViewModel() replaces viewModel(factory = MarketViewModel.factory(context))
    // Hilt automatically wires the dependencies (use cases, repository, etc.)
    val viewModel: MarketViewModel = hiltViewModel()

    // Collect the full UI state as a Compose State object
    // The screen redraws automatically whenever state changes
    val state by viewModel.state.collectAsState()

    // Collect one-shot navigation effects
    // LaunchedEffect(Unit) runs once and keeps collecting until the composable leaves the composition
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                // Navigate to the coin detail screen with the selected coin's id
                is MarketEffect.NavigateToCoinDetail -> {
                    navController.navigate("coin_detail/${effect.coinId}")
                }
                // Show error — for now just logged (TODO: show Snackbar)
                is MarketEffect.ShowError -> {
                    // TODO: show snackbar here
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(8.dp)
    ) {
        Text(
            text = "Markets",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Search bar — sends SearchChanged intent when the user types
        MarketSearchBar(
            query = state.searchQuery,
            onQueryChange = { viewModel.onIntent(MarketIntent.SearchChanged(it)) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (state.rateLimitCountdown > 0) {
            RateLimitBanner(secondsRemaining = state.rateLimitCountdown)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Filter chips (ALL / FAVORITES)
        MarketFilterRow(
            coins = state.coins,
            currentFilter = state.filter,
            onFilterSelected = { viewModel.onIntent(MarketIntent.FilterChanged(it)) }
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Coin list — loading, error, or the actual list
        MarketCoinsList(
            coins = state.coins,
            isLoading = state.isLoading,
            error = state.error,
            currentFilter = state.filter,
            onFavoriteClick = { coin ->
                viewModel.onIntent(MarketIntent.FavoriteClicked(coin.id, coin.isFavorite))
            },
            onCoinClick = { coin ->
                viewModel.onIntent(MarketIntent.CoinClicked(coin.id))
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composables (stateless — they receive data and callbacks, no ViewModel access)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The search text field at the top of the Market screen.
 *
 * Stateless composable — the current [query] value comes from [MarketState]
 * and [onQueryChange] sends a [MarketIntent.SearchChanged] back up.
 */
@Composable
fun MarketSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(text = "Search", color = TextSecondary) },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = "search")
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor   = CardBackground,
            unfocusedContainerColor = CardBackground,
            focusedIndicatorColor   = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

/**
 * A horizontal row of filter chips (ALL and FAVORITES).
 *
 * Shows a "No favorites yet" message when [MarketFilter.FAVORITES] is selected
 * but the [coins] list is empty (user hasn't favorited anything).
 *
 * @param coins          The currently displayed coins (after filter is applied).
 * @param currentFilter  The currently selected filter chip.
 * @param onFilterSelected Callback when the user taps a filter chip.
 */
@Composable
fun MarketFilterRow(
    coins: List<Coin>,
    currentFilter: MarketFilter,
    onFilterSelected: (MarketFilter) -> Unit
) {
    val filters = listOf(MarketFilter.ALL, MarketFilter.FAVORITES)

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(filters) { filter ->
            val isSelected = currentFilter == filter
            Box(
                modifier = Modifier
                    .then(
                        if (isSelected)
                            Modifier.background(PrimaryGreen, RoundedCornerShape(20.dp))
                        else
                            Modifier.background(
                                Brush.verticalGradient(
                                    colors = listOf(DarkCard, DarkCard, NeonGreen.copy(alpha = 0.05f))
                                ),
                                RoundedCornerShape(20.dp)
                            )
                    )
                    .clickable { onFilterSelected(filter) }
                    .wrapContentWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = filter.toDisplayName(),
                    color = TextPrimary
                )
            }
        }
    }

    // Show a message if the user is on FAVORITES but hasn't starred anything
    if (coins.isEmpty() && currentFilter == MarketFilter.FAVORITES) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "No favorites yet ⭐", color = TextSecondary)
        }
    }
}

/**
 * The main scrollable list of coins.
 * Shows a loading message, an error, or the actual coin list depending on the state.
 *
 * FIX: The old code had `var isPositive = false` as a file-level mutable variable.
 * This caused a race condition in Compose — writing a global var inside LazyColumn items
 * and reading it in sub-composables is not safe. Now [isPositive] is a LOCAL val
 * computed per coin inside the lambda, which is always correct.
 */
@Composable
fun MarketCoinsList(
    coins: List<Coin>,
    isLoading: Boolean,
    error: String?,
    currentFilter: MarketFilter,
    onFavoriteClick: (Coin) -> Unit,
    onCoinClick: (Coin) -> Unit
) {
    when {
        isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Loading...", color = TextSecondary)
            }
        }
        error != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Error: $error", color = PrimaryRed)
            }
        }
        else -> {
            LazyColumn {
                items(coins) { coin ->
                    // ✅ LOCAL val — computed fresh for each coin, no global mutation
                    val isPositive = coin.priceChangePercent24h >= 0

                    // Format price in Indian locale (e.g. "6,56,985")
                    val formattedPrice = NumberFormat
                        .getNumberInstance(Locale("en", "IN"))
                        .format(coin.currentPrice)

                    MarketCoinItem(
                        coin       = coin,
                        price      = "₹$formattedPrice",
                        change     = "${"%.2f".format(coin.priceChangePercent24h)}%",
                        isPositive = isPositive,     // Passed as a parameter — correct per coin
                        onFavoriteClick = { onFavoriteClick(coin) },
                        onCoinClick     = { onCoinClick(coin) }
                    )
                }
            }
        }
    }
}

/**
 * A single row in the coin list.
 *
 * Stateless composable — receives all data as parameters and calls lambdas for user actions.
 * Does NOT hold a reference to the ViewModel (correct pattern for reusable composables).
 *
 * @param coin           The domain [Coin] model to display.
 * @param price          Pre-formatted price string, e.g. "₹6,56,985".
 * @param change         Pre-formatted change string, e.g. "+2.34%".
 * @param isPositive     True if the price went up — used to pick green vs red color.
 * @param onFavoriteClick Called when the heart icon is tapped.
 * @param onCoinClick     Called when the whole row is tapped (navigate to detail).
 */
@Composable
fun MarketCoinItem(
    coin: Coin,
    price: String,
    change: String,
    isPositive: Boolean,
    onFavoriteClick: () -> Unit,
    onCoinClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onCoinClick() },     // Tap the row → go to detail screen
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Coin logo image (loaded by Coil from URL)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(DarkBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model             = coin.imageUrl,
                    contentDescription = coin.name,
                    modifier          = Modifier.size(40.dp).clip(CircleShape),
                    placeholder       = painterResource(R.drawable.placeholder),
                    error             = painterResource(R.drawable.placeholder)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Coin name + symbol
            Column(modifier = Modifier.weight(1f)) {
                Text(text = coin.name, color = TextPrimary)
                Text(text = coin.symbol.uppercase(), color = TextSecondary, fontSize = 12.sp)
            }

            // Mini price chart (sparkline) — uses the isPositive passed as a parameter
            SparklineChart(
                values     = coin.sparkline.takeLast(20),
                isPositive = isPositive
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Price + percentage change
            Column(horizontalAlignment = Alignment.End,
                modifier = Modifier.width(80.dp)) {
                Text(text = price, color = TextPrimary)
                Text(
                    text  = change,
                    color = if (isPositive) PrimaryGreen else PrimaryRed  // Green for up, red for down
                )
            }

            // Favorite / unfavorite button
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector  = if (coin.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint         = if (coin.isFavorite) PrimaryRed else TextSecondary
                )
            }
        }
    }
}

/**
 * A small canvas-drawn line chart showing recent price trend.
 *
 * Draws a line path and a shaded fill area beneath it.
 * Color is green if the price went up, red if it went down.
 *
 * @param values     List of price data points (relative values, not absolute prices).
 * @param isPositive True = draw in green, False = draw in red.
 */
@Composable
fun SparklineChart(
    values: List<Float>,
    isPositive: Boolean
) {
    // Pick the chart color based on whether the coin is up or down
    val lineColor = if (isPositive) PrimaryGreen else PrimaryRed

    Canvas(
        modifier = Modifier.width(70.dp).height(25.dp)
    ) {
        if (values.isEmpty()) return@Canvas

        val maxVal  = values.maxOrNull() ?: 0f
        val minVal  = values.minOrNull() ?: 0f
        val range   = maxVal - minVal
        val stepX   = size.width / (values.size - 1)

        val linePath = Path()
        val fillPath = Path()

        values.forEachIndexed { index, value ->
            val x = index * stepX

            // Normalize value to 0..1 range, then invert Y (screen Y goes top-down)
            val normalized = if (range == 0f) 0f else (value - minVal) / range
            val y = size.height - normalized * size.height

            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, size.height)  // Start fill from bottom-left
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            if (index == values.lastIndex) {
                fillPath.lineTo(x, size.height)  // Close fill back to bottom-right
                fillPath.close()
            }
        }

        // Draw the semi-transparent fill area under the line
        drawPath(
            path  = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent)
            )
        )

        // Draw the line itself
        drawPath(
            path  = linePath,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

/**
 * Returns a human-readable display name for each [MarketFilter] value.
 * Keeps display strings out of the enum itself (separation of presentation from domain).
 */
fun MarketFilter.toDisplayName(): String = when (this) {
    MarketFilter.ALL       -> "All Crypto"
    MarketFilter.FAVORITES -> "Favorites"
}
@Composable
fun RateLimitBanner(secondsRemaining: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.placeholder), // swap with a clock/warning icon if you have one
                contentDescription = null,
                tint = PrimaryRed,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "API limit reached. Retry in ${secondsRemaining}s",
                color = PrimaryRed,
                fontSize = 13.sp
            )
        }
    }
}