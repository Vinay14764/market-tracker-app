package com.example.markettracker.ui.screens.login

/**
 * UI state for the Login screen.
 *
 * CHANGE: Removed [isLoginSuccessful] boolean.
 *   Old code used [isLoginSuccessful = true] in state to trigger navigation.
 *   This caused a bug: rotating the screen re-triggered the LaunchedEffect
 *   because the state still held true even after navigation.
 *
 *   Navigation is now sent as a [LoginEffect.NavigateToDashboard] via a Channel,
 *   which is delivered exactly once — screen rotation safe.
 */
data class LoginState(

    /** Current text in the username input field. */
    val userName: String = "",

    /** Current text in the password input field. */
    val password: String = "",

    /**
     * True while a login network request is in progress.
     * Show a loading spinner on the button when this is true.
     * Currently unused (no real auth yet), but ready for when auth is added.
     */
    val isLoading: Boolean = false,

    /**
     * Inline error message shown below the login button.
     * Null = no error. Non-null = show this text in red.
     * Cleared automatically when the user starts typing again.
     */
    val error: String? = null
)