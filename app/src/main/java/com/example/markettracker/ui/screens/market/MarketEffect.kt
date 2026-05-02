package com.example.markettracker.ui.screens.market

/**
 * One-shot side effects that the Market screen needs to perform exactly once.
 *
 * This is the "Effect" part of MVI (Model-View-Intent).
 *
 * WHY effects instead of putting navigation in the State?
 *   If you stored `navigateTo: String?` in [MarketState] and the user rotated the screen,
 *   [LaunchedEffect] would see the same non-null value and navigate again — a nasty bug.
 *
 *   A [kotlinx.coroutines.channels.Channel] delivers each effect EXACTLY ONCE and doesn't
 *   replay it on recomposition or screen rotation. This fixes the double-navigation bug
 *   that existed in the old [LoginState.isLoginSuccessful] approach.
 *
 * In the ViewModel:
 *   private val _effect = Channel<MarketEffect>(Channel.BUFFERED)
 *   val effect: Flow<MarketEffect> = _effect.receiveAsFlow()
 *
 * In the Composable:
 *   LaunchedEffect(Unit) {
 *       viewModel.effect.collect { effect ->
 *           when (effect) {
 *               is MarketEffect.NavigateToCoinDetail -> navController.navigate("coin_detail/${effect.coinId}")
 *               is MarketEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
 *           }
 *       }
 *   }
 */
sealed class MarketEffect {

    /**
     * Navigate to the coin detail screen for the given coin.
     * @param coinId The CoinGecko id to pass as a navigation argument.
     */
    data class NavigateToCoinDetail(val coinId: String) : MarketEffect()

    /**
     * Show a transient error message (e.g. in a Snackbar or Toast).
     * @param message The error text to display.
     */
    data class ShowError(val message: String) : MarketEffect()
}