package com.example.markettracker.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Login screen.
 *
 * Handles login form validation and triggers navigation via [LoginEffect].
 *
 * KEY CHANGE from the old code:
 *   Old: [LoginState.isLoginSuccessful = true] triggered navigation via LaunchedEffect(state.isLoginSuccessful).
 *        BUG: rotating the screen re-triggered LaunchedEffect because the state still held true.
 *
 *   New: Navigation is sent as a [LoginEffect] through a [Channel].
 *        A Channel delivers each value EXACTLY ONCE — no re-trigger on rotation.
 *
 * NOTE: Currently uses basic validation (non-empty fields).
 *       TODO: Replace with real backend authentication (Firebase Auth, custom API) in a future sprint.
 *
 * @HiltViewModel — Hilt creates and injects this ViewModel. No companion object factory needed.
 * @Inject constructor() — No dependencies needed yet (real auth will add an AuthUseCase here).
 */
@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {

    // ─────────────────────────────────────────────────
    // State
    // ─────────────────────────────────────────────────

    /** Internal mutable state — only this ViewModel can write to it. */
    private val _state = MutableStateFlow(LoginState())

    /**
     * Public read-only state for the UI.
     * Collect in the Composable: val state by viewModel.state.collectAsState()
     */
    val state: StateFlow<LoginState> = _state.asStateFlow()

    // ─────────────────────────────────────────────────
    // Effect — one-shot events (navigation)
    // ─────────────────────────────────────────────────

    /** Internal Channel for one-shot effects like navigation. */
    private val _effect = Channel<LoginEffect>(Channel.BUFFERED)

    /**
     * Public effects stream. Collect in the Composable:
     *   LaunchedEffect(Unit) { viewModel.effect.collect { handle(it) } }
     */
    val effect = _effect.receiveAsFlow()

    // ─────────────────────────────────────────────────
    // Event Handlers — called by the UI when the user interacts
    // ─────────────────────────────────────────────────

    /**
     * Called every time the user types a character in the username field.
     * Clears any existing error message when the user starts typing again.
     */
    fun onUserNameChange(userName: String) {
        _state.update { it.copy(userName = userName, error = null) }
    }

    /**
     * Called every time the user types a character in the password field.
     * Clears any existing error message when the user starts typing again.
     */
    fun onPasswordChange(password: String) {
        _state.update { it.copy(password = password, error = null) }
    }

    /**
     * Called when the user taps the Login button.
     *
     * Validates the form:
     *   - If fields are empty → update state with an error message (shown below the button).
     *   - If valid → send [LoginEffect.NavigateToDashboard] via Channel (triggers navigation once).
     *
     * TODO: Replace the simple non-empty check with a real authentication call:
     *   _state.update { it.copy(isLoading = true) }
     *   when (val result = loginUseCase(state.userName, state.password)) {
     *       is AppResult.Success -> _effect.send(LoginEffect.NavigateToDashboard)
     *       is AppResult.Error   -> _state.update { it.copy(error = result.exception.message) }
     *   }
     *   _state.update { it.copy(isLoading = false) }
     */
    fun login() {
        val currentState = _state.value

        if (currentState.userName.isEmpty() || currentState.password.isEmpty()) {
            // Show inline error message below the button
            _state.update { it.copy(error = "Fields can't be empty") }
        } else {
            // Validation passed — navigate to Dashboard via Channel (delivered exactly once)
            viewModelScope.launch {
                _state.update { it.copy(isLoading = true) }
                _effect.send(LoginEffect.NavigateToDashboard)
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}