package com.zooempire.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zooempire.model.ZooState
import com.zooempire.ui.theme.*

@Composable
fun GameTopBar(
    state: ZooState,
    isRunning: Boolean,
    speed: Int,
    onTogglePlay: () -> Unit,
    onSetSpeed: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ZooGreenDark)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Top row: Zoo name + Day
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                state.zooName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                "Day ${state.day}",
                color = ZooGold,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bottom row: Money, visitors, rating, controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Money
            StatChip(
                label = "$${state.money}",
                emoji = "\uD83D\uDCB0",
                color = if (state.money >= 0) ZooGold else ZooRed
            )
            // Visitors
            StatChip(
                label = "${state.visitors}",
                emoji = "\uD83D\uDC65"
            )
            // Rating
            StatChip(
                label = "${"%.1f".format(state.zooRating)}/5",
                emoji = "\u2B50"
            )
            // Play controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isRunning) ZooRed else ZooGreen)
                        .clickable(onClick = onTogglePlay)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (isRunning) "||" else "\u25B6",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                (1..3).forEach { s ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (speed == s) ZooGold else Color(0x44FFFFFF))
                            .clickable { onSetSpeed(s) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "${s}x",
                            color = if (speed == s) ZooGreenDark else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (s < 3) Spacer(modifier = Modifier.width(2.dp))
                }
            }
        }

        // Revenue/Expense row
        if (state.day > 1) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "\u2B06 +$${state.dailyRevenue}",
                    color = Color(0xFF81C784),
                    fontSize = 12.sp
                )
                Text(
                    "\u2B07 -$${state.dailyExpenses}",
                    color = Color(0xFFEF9A9A),
                    fontSize = 12.sp
                )
                Text(
                    "Net: $${state.dailyRevenue - state.dailyExpenses}",
                    color = if (state.dailyRevenue >= state.dailyExpenses) Color(0xFF81C784) else ZooRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatChip(label: String, emoji: String, color: Color = Color.White) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(2.dp))
        Text(label, color = color, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}
