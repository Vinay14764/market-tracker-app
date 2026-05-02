package com.example.markettracker.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.markettracker.ui.components.StarfieldBackground

/**
 * Dashboard (Home) screen composable.
 *
 * WHAT CHANGED:
 *   Old: All sections (TopGainersSection, TopLosersSection) had hardcoded data.
 *   New: [DashboardViewModel] provides live coin data from Room.
 *        TopGainersSection and TopLosersSection receive real [Coin] lists.
 *
 * Data flows automatically:
 *   CoinGecko API → MarketViewModel refresh → Room DB → DashboardViewModel
 *   → DashboardState → DashboardScreen → TopGainersSection
 *
 * No extra network call is made by this screen — it reads the data that
 * the Market screen's ViewModel already fetched and stored in Room.
 */
@Composable
fun DashboardScreen() {

    // hiltViewModel() creates DashboardViewModel with injected use cases
    val viewModel: DashboardViewModel = hiltViewModel()

    // Collect the full dashboard state
    val state by viewModel.state.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        StarfieldBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            TopBar()

            PortfolioCard()

            QuickActionsRow()
            LazyColumn {
                // Pass real data from state to the section composables
                item { TopGainersSection(coins = state.topGainers) }

                item { TopLosersSection(coins = state.topLosers) }
            }
        }
    }
}