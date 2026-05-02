package com.example.markettracker.ui.screens.dashboard

import android.R.attr.title
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.InsertInvitation
import androidx.compose.material.icons.filled.Moving
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.markettracker.ui.theme.BlueAccent
import com.example.markettracker.ui.theme.CardBackground
import com.example.markettracker.ui.theme.NeonGreen
import com.example.markettracker.ui.theme.OrangeAccent
import com.example.markettracker.ui.theme.PurpleAccent
import com.example.markettracker.ui.theme.TextPrimary
import com.example.markettracker.ui.theme.TextSecondary

@Composable
fun QuickActionsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        QuickActionItem(Icons.Filled.InsertInvitation, "Sip", NeonGreen)
        QuickActionItem(Icons.Filled.Moving, "Earn", BlueAccent)
        QuickActionItem(Icons.Filled.SwapHoriz, "Exchange", OrangeAccent)
        QuickActionItem(Icons.Filled.InsertChart, "Market", PurpleAccent)
    }

}

@Composable
private fun QuickActionItem(icon: ImageVector, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}