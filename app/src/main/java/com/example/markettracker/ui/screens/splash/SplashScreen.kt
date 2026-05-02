package com.example.markettracker.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.markettracker.R

/**
 * Splash screen that plays a Lottie animation once and navigates to Login
 * only after the animation has fully completed.
 *
 * HOW IT WORKS:
 *   [animateLottieCompositionAsState] drives the animation and exposes [progress] (0f → 1f).
 *   [LaunchedEffect(progress)] watches that value — when it reaches 1f (100%), we navigate.
 *
 * WHY NOT delay(2500)?
 *   A hardcoded delay is a guess. If the animation is shorter, the user waits unnecessarily.
 *   If it's longer, we navigate before it finishes. [animateLottieCompositionAsState] is exact.
 *
 * @param onNavigationToLogin Called once when the animation finishes — triggers navigation.
 */
@Composable
fun SplashScreen(
    onNavigationToLogin: () -> Unit
) {
    // Load the Lottie animation composition from the raw resource file
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.savingthemoney)
    )

    // Animate the composition exactly once (iterations = 1).
    // [progress] goes from 0f (start) to 1f (end) as the animation plays.
    // When [composition] is null (still loading), [progress] stays at 0f — no navigation yet.
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations  = 1
    )

    // Navigate to Login exactly when the animation reaches 100% completion.
    // [LaunchedEffect(progress)] re-runs whenever [progress] changes.
    // The check [progress == 1f] ensures we only navigate once, at the very end.
    LaunchedEffect(progress) {
        if (progress == 1f) {
            onNavigationToLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress    = { progress },   // Drive the animation with the same progress value
            modifier    = Modifier.size(220.dp)
        )
    }
}