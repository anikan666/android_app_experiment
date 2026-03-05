package com.zooempire.ui.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.zooempire.model.*
import com.zooempire.ui.theme.*

@Composable
fun ShopPanel(
    money: Int,
    onBuyEnclosure: (EnclosureType) -> Unit,
    onBuyFacility: (FacilityType) -> Unit,
    onPlacePath: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Enclosures", "Facilities", "Paths")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.45f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Shop",
                    style = MaterialTheme.typography.titleLarge,
                    color = ZooGreen
                )
                Text(
                    "Budget: $$money",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (money > 0) ZooGreen else ZooRed
                )
                TextButton(onClick = onClose) {
                    Text("Close")
                }
            }

            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 13.sp) }
                    )
                }
            }

            when (selectedTab) {
                0 -> EnclosureList(money, onBuyEnclosure)
                1 -> FacilityList(money, onBuyFacility)
                2 -> PathOption(onPlacePath)
            }
        }
    }
}

@Composable
private fun EnclosureList(money: Int, onBuy: (EnclosureType) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(EnclosureType.entries) { type ->
            ShopItem(
                title = "${type.displayName} Enclosure",
                subtitle = "Capacity: ${type.capacity} animals",
                cost = type.cost,
                canAfford = money >= type.cost,
                color = Color(type.color),
                onClick = { onBuy(type) }
            )
        }
    }
}

@Composable
private fun FacilityList(money: Int, onBuy: (FacilityType) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(FacilityType.entries) { type ->
            ShopItem(
                title = "${type.emoji} ${type.displayName}",
                subtitle = "Upkeep: $${type.dailyUpkeep}/day | Visitor +${type.visitorBonus}",
                cost = type.cost,
                canAfford = money >= type.cost,
                onClick = { onBuy(type) }
            )
        }
    }
}

@Composable
private fun PathOption(onPlace: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Paths are free! Tap to place.", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onPlace,
            colors = ButtonDefaults.buttonColors(containerColor = ZooBrown)
        ) {
            Text("Place Paths")
        }
    }
}

@Composable
private fun ShopItem(
    title: String,
    subtitle: String,
    cost: Int,
    canAfford: Boolean,
    color: Color = Color.Transparent,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canAfford, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (canAfford) Color(0xFFF5F5F5) else Color(0xFFE0E0E0)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (color != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Text(
                "$$cost",
                fontWeight = FontWeight.Bold,
                color = if (canAfford) ZooGreen else ZooRed,
                fontSize = 15.sp
            )
        }
    }
}
