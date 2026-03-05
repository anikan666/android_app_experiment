package com.zooempire.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zooempire.model.GameEvent
import com.zooempire.ui.theme.*

@Composable
fun EventLog(
    events: List<GameEvent>,
    modifier: Modifier = Modifier
) {
    if (events.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        events.take(3).forEach { event ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (event.isPositive) Color(0xCCE8F5E9) else Color(0xCCFFEBEE)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (event.isPositive) "\u2705" else "\u26A0\uFE0F",
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Day ${event.day}: ${event.message}",
                    fontSize = 11.sp,
                    color = if (event.isPositive) ZooGreenDark else ZooRed,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
