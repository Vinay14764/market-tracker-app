package com.example.markettracker.ui.screens.login

/**
 * One-shot side effects for the Login screen.
 *
 * Replaces the old [LoginState.isLoginSuccessful] boolean approach.
 *
 * WHY:
 *   The old code stored [isLoginSuccessful = true] in the state. When collected
 *   in a [LaunchedEffect(state.isLoginSuccessful)], rotating the screen caused
 *   the effect to re-trigger and navigate to Dashboard again (or crash).
 *
 *   A [Channel]-based effect is delivered exactly once. Even if the screen
 *   recomposes 10 times, navigation only happens once.
 *
 * See [MarketEffect] for a detailed explanation of the Channel pattern.
 */
sealed class LoginEffect {

    /**
     * Login validation passed — navigate to the Dashboard.
     * Triggered once when [LoginViewModel.login] succeeds.
     */
    data object NavigateToDashboard : LoginEffect()
}