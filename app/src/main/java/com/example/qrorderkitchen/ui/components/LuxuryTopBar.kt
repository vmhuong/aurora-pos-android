package com.example.qrorderkitchen.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrorderkitchen.ui.theme.AuroraGold
import com.example.qrorderkitchen.ui.theme.AuroraGoldHover
import com.example.qrorderkitchen.ui.theme.AuroraGoldSoft
import com.example.qrorderkitchen.ui.theme.LightBorder
import com.example.qrorderkitchen.ui.theme.LightSurface
import com.example.qrorderkitchen.ui.theme.TextPrimary
import com.example.qrorderkitchen.ui.theme.TextSecondary
import com.example.qrorderkitchen.ui.viewmodel.AppMode

@Composable
fun LuxuryTopBar(
    appMode: AppMode,
    currentTableId: String,
    onTableClick: () -> Unit,
    onModeSwitchClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightSurface)
            .statusBarsPadding() // Shift top header down below system clock/status bar!
            .border(0.5.dp, LightBorder, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Logo / Title
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AURORA",
                    color = AuroraGold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "RESTAURANT & LOUNGE",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Active Table Tag (Clickable to change table)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(AuroraGoldSoft)
                    .border(1.dp, AuroraGold.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .clickable { onTableClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulsing Green Live Dot
                Box(
                    modifier = Modifier
                        .scale(scale)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF, 0x10, 0xB9, 0x81))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bàn $currentTableId · Active",
                    color = AuroraGoldHover,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Role Switcher Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (appMode == AppMode.STAFF) AuroraGold else LightBorder)
                    .clickable { onModeSwitchClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (appMode == AppMode.STAFF) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                        contentDescription = "Mode",
                        tint = if (appMode == AppMode.STAFF) LightSurface else TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (appMode == AppMode.STAFF) "POS Staff" else "Khách",
                        color = if (appMode == AppMode.STAFF) LightSurface else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
