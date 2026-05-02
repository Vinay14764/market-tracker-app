package com.example.markettracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class Star(
    val x: Float,       // 0..1 relative to screen width
    val y: Float,       // 0..1 relative to screen height
    val radius: Float,  // px
    val phase: Float    // twinkle phase offset (radians)
)

@Composable
fun StarfieldBackground(
    modifier: Modifier = Modifier,
    starCount: Int = 120,
    backgroundColor: Color = Color(0xFF04080F)
) {
    val infinite = rememberInfiniteTransition(label = "starfield")

    // Single float drives all star brightness via sin(time + phase)
    val twinkle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        ),
        label = "twinkle"
    )

    // Generated once — never recreated on recomposition
    val stars = remember(starCount) {
        List(starCount) {
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                radius = Random.nextFloat() * 2.2f + 0.4f,
                phase = Random.nextFloat() * 2f * PI.toFloat()
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize().background(backgroundColor)) {
        stars.forEach { star ->
            val alpha = (sin(twinkle + star.phase) * 0.4f + 0.6f).coerceIn(0.1f, 1f)
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = star.radius,
                center = Offset(star.x * size.width, star.y * size.height)
            )
        }
    }
}