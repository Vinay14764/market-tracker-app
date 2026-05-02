package com.example.markettracker.ui.screens.portfolio

import android.webkit.WebSettings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import coil.compose.AsyncImage
import com.example.markettracker.ui.theme.Background
import com.example.markettracker.ui.theme.CardBackground
import com.example.markettracker.ui.theme.DarkBackground
import com.example.markettracker.ui.theme.DarkCard
import com.example.markettracker.ui.theme.DepositColor
import com.example.markettracker.ui.theme.NeonGreen
import com.example.markettracker.ui.theme.PrimaryGreen
import com.example.markettracker.ui.theme.PrimaryRed
import com.example.markettracker.ui.theme.TextPrimary
import com.example.markettracker.ui.theme.TextSecondary
import com.example.markettracker.ui.theme.WithdrawColor

@Composable
fun PortfolioScreen(
    viewModel: PortfolioViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    when {
        state.isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NeonGreen)
            }
        }

        state.isEmpty -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No holdings yet",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Star coins on the Market screen\nto add them to your portfolio",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        }else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
                    .padding(20.dp)

            ) {
                Text(
                    text = "Portfolio",
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                PortfolioSummaryCard(
                    totalInvestment = state.totalInvested,
                    totalCurrentValue =  state.totalCurrentValue,
                    overallPnlPercent = state.overallPnl
                )
                Spacer(Modifier.height(16.dp))
                ActionButtonsRow()
                Spacer(Modifier.height(20.dp))
                Text(
                    "My Holdings",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.holding) { holding ->
                        HoldingItem(holding)
                    }
                }
            }
        }
    }
}

@Composable
fun HoldingItem( holding: PortfolioHolding) {
    val isProfit = holding.priceChangePercentage > 0
    val changeColor = if(isProfit) PrimaryGreen else PrimaryRed
    val changePrefix = if(isProfit) "+" else "-"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = holding.imageUri,
                    contentDescription = holding.name,
                    modifier = Modifier
                        .size(40.dp)
                        .background(DarkBackground, CircleShape)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    holding.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    holding.symbol.uppercase(),
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    "Invested: ₹${"%.0f".format(holding.investmentAmount)}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "₹${"%.2f".format(holding.currentValue)}",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .background(changeColor, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "$changePrefix${"%.2f".format(holding.priceChangePercentage)}%",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}


@Composable
fun ActionButtonsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = {},
            modifier = Modifier.weight(1f),
            border = BorderStroke(1.dp, DepositColor)
        ) {
            Text(
                text = "Deposit",
                color = DepositColor
            )
        }
        OutlinedButton(
            onClick = {},
            modifier = Modifier.weight(1f),
            border = BorderStroke(1.dp, WithdrawColor)

        ) {
            Text(
                text = "Withdraw",
                color = WithdrawColor
            )
        }
    }
}

@Composable
fun PortfolioSummaryCard(
totalInvestment : Double,
totalCurrentValue : Double,
overallPnlPercent : Double
) {
    val isProfit = overallPnlPercent >= 0
    val pnlColor = if (isProfit) PrimaryGreen else Color(0xFFE57373)
    val pnlPrefix = if (isProfit) "+" else ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        )
    )
    {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkCard,
                            DarkCard,
                            NeonGreen.copy(alpha = 0.05f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text("Total Investment Amount", color = TextSecondary)
                Text(
                    "₹${"%.2f".format(totalInvestment)}",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))

                Text("Current Investment Value", color = TextSecondary)
                Text(
                    "₹${"%.2f".format(totalCurrentValue)}",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                Row {
                    Text("Overall Profit/Loss  ", color = TextSecondary)
                    Text(
                        "$pnlPrefix${"%.2f".format(overallPnlPercent)}%",
                        color = pnlColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

        }
    }
}