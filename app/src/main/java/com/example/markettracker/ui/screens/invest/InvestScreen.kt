package com.example.markettracker.ui.screens.invest

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.room.util.TableInfo
import coil.compose.AsyncImage
import com.example.markettracker.domain.model.Coin
import com.example.markettracker.ui.components.StarfieldBackground
import com.example.markettracker.ui.screens.market.CoinList
import com.example.markettracker.ui.screens.market.MarketCoinItem
import com.example.markettracker.ui.theme.Background
import com.example.markettracker.ui.theme.CardBackground
import com.example.markettracker.ui.theme.DarkBackground
import com.example.markettracker.ui.theme.NeonGreen
import com.example.markettracker.ui.theme.NeonGreenDark
import com.example.markettracker.ui.theme.PrimaryGreen
import com.example.markettracker.ui.theme.PrimaryRed
import com.example.markettracker.ui.theme.TextPrimary
import com.example.markettracker.ui.theme.TextSecondary

@Composable
fun InvestScreen(
    viewModel: InvestViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    if (state.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = NeonGreen)
        }
        return
    }
    Box(modifier = Modifier.fillMaxSize()) {
        StarfieldBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text(
                text = "Invest",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.height(16.dp))

            InvestFiltersRow()

            Spacer(Modifier.height(20.dp))

            TrendingSection(state.trendingCoins)

            Spacer(Modifier.height(20.dp))

            MostTradedSection(state.mostTraded)

        }
    }
}

@Composable
fun MostTradedSection(coin: List<Coin>) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Most Traded",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
    }
    LazyColumn(modifier = Modifier
        .fillMaxSize()) {
        items(coin) { coin ->
            InvestCoinItem(coin)
        }
    }
}

@Composable
    fun InvestFiltersRow() {
        val filters = listOf("Discover", "Themes", "Sectors", "New")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            filters.forEach { filter ->
                Box(
                    modifier = Modifier
                        .background(
                            CardBackground, RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = filter,
                        color = TextSecondary
                    )
                }
            }
        }
    }

    @Composable
    fun TrendingSection(coins: List<Coin>) {
        Column {
            Text(
                text = "Trending Now",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(coins) { coin ->
                    TrendingCoin(coin)
                }
            }
        }
    }

    @Composable
    fun TrendingCoin(coin: Coin) {
        val isPositive = coin.priceChangePercent24h >= 0
        val changeColor = if (isPositive) PrimaryGreen else PrimaryRed
        val changePrefix = if (isPositive) "+" else ""

        Card(
            modifier = Modifier
                .width(140.dp)
                .height(170.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = CardBackground
            )
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                // Top section
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = coin.imageUrl,
                            contentDescription = coin.name,
                            modifier = Modifier
                                .size(36.dp)
                                .background(DarkBackground, CircleShape)
                        )

                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.height(40.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            coin.name,
                            color = TextPrimary,
                        )
                        Text(coin.symbol.uppercase(), color = TextSecondary, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Price section
                Column {
                    Text("₹${"%.2f".format(coin.currentPrice)}", color = TextPrimary)
                    Text(
                        "$changePrefix${"%.2f".format(coin.priceChangePercent24h)}%",
                        color = changeColor,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                // Button
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreenDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Invest",
                        color = DarkBackground
                    )
                }
            }
        }
    }

    @Composable
    fun InvestCoinItem(coin: Coin) {
        val isPositive = coin.priceChangePercent24h >= 0
        val changeColor = if (isPositive) PrimaryGreen else PrimaryRed
        val changePrefix = if (isPositive) "+" else ""

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = CardBackground
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = coin.imageUrl,
                            contentDescription = coin.name,
                            modifier = Modifier
                                .size(36.dp)
                                .background(DarkBackground, CircleShape)
                        )

                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(coin.name, color = TextPrimary)
                            Text(coin.symbol.uppercase(), color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("₹${"%.2f".format(coin.currentPrice)}", color = changeColor)
                        Text(
                            "$changePrefix${"%.2f".format(coin.priceChangePercent24h)}%",
                            color = changeColor,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
