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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zooempire.model.PlacementMode
import com.zooempire.ui.theme.*

@Composable
fun GameBottomBar(
    placementMode: PlacementMode,
    onShopClick: () -> Unit,
    onCancelPlacement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ZooGreenDark)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (placementMode !is PlacementMode.None) {
            Text(
                when (placementMode) {
                    is PlacementMode.PlacingEnclosure -> "Tap to place ${placementMode.type.displayName}"
                    is PlacementMode.PlacingFacility -> "Tap to place ${placementMode.type.displayName}"
                    is PlacementMode.PlacingPath -> "Tap to place paths"
                    else -> ""
                },
                color = ZooGold,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onCancelPlacement,
                colors = ButtonDefaults.buttonColors(containerColor = ZooRed)
            ) {
                Text("Cancel")
            }
        } else {
            BottomBarButton(emoji = "\uD83D\uDED2", label = "Shop", onClick = onShopClick)
        }
    }
}

@Composable
private fun BottomBarButton(emoji: String, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}
