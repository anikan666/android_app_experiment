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
fun EnclosureDetailPanel(
    enclosure: Enclosure,
    money: Int,
    onBuyAnimal: (AnimalType, Enclosure) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f),
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
                Column {
                    Text(
                        "${enclosure.type.displayName} Enclosure",
                        style = MaterialTheme.typography.titleLarge,
                        color = ZooGreen
                    )
                    Text(
                        "${enclosure.animals.size}/${enclosure.type.capacity} animals",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
                TextButton(onClick = onClose) {
                    Text("Close")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Current animals
            if (enclosure.animals.isNotEmpty()) {
                Text("Current Animals:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                enclosure.animals.forEach { animal ->
                    AnimalRow(animal)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Buy animals section
            if (!enclosure.isFull) {
                Text("Add Animal:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                val compatibleAnimals = AnimalType.entries.filter { it.requiredEnclosure == enclosure.type }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(compatibleAnimals) { type ->
                        val canAfford = money >= type.cost
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = canAfford) { onBuyAnimal(type, enclosure) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (canAfford) Color(0xFFF5F5F5) else Color(0xFFE0E0E0)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(type.emoji, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(type.displayName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(
                                        "Attraction: +${type.visitorAttraction} | Upkeep: $${type.dailyUpkeep}/day",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                Text(
                                    "$${type.cost}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (canAfford) ZooGreen else ZooRed,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    "Enclosure is full!",
                    color = ZooRed,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun AnimalRow(animal: Animal) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(animal.type.emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(animal.type.displayName, fontSize = 13.sp, modifier = Modifier.weight(1f))
        // Happiness bar
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Happy", fontSize = 10.sp, color = Color.Gray)
            LinearProgressIndicator(
                progress = { animal.happiness },
                modifier = Modifier
                    .width(50.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when {
                    animal.happiness > 0.7f -> ZooGreen
                    animal.happiness > 0.4f -> ZooGold
                    else -> ZooRed
                },
                trackColor = Color(0xFFE0E0E0)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        // Health bar
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Health", fontSize = 10.sp, color = Color.Gray)
            LinearProgressIndicator(
                progress = { animal.health },
                modifier = Modifier
                    .width(50.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when {
                    animal.health > 0.7f -> ZooBlue
                    animal.health > 0.4f -> ZooGold
                    else -> ZooRed
                },
                trackColor = Color(0xFFE0E0E0)
            )
        }
    }
}
