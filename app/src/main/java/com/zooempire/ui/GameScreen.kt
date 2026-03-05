package com.zooempire.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zooempire.engine.ZooGameEngine
import com.zooempire.model.PlacementMode
import com.zooempire.ui.components.*
import com.zooempire.ui.shop.*
import com.zooempire.ui.zoo.ZooMapView

@Composable
fun GameScreen(
    engine: ZooGameEngine = viewModel()
) {
    val zooState by engine.zooState.collectAsStateWithLifecycle()
    val grid by engine.grid.collectAsStateWithLifecycle()
    val placementMode by engine.placementMode.collectAsStateWithLifecycle()
    val events by engine.events.collectAsStateWithLifecycle()
    val isRunning by engine.isRunning.collectAsStateWithLifecycle()
    val showShop by engine.showShop.collectAsStateWithLifecycle()
    val selectedEnclosure by engine.selectedEnclosure.collectAsStateWithLifecycle()
    val speed by engine.speed.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            GameTopBar(
                state = zooState,
                isRunning = isRunning,
                speed = speed,
                onTogglePlay = {
                    if (isRunning) engine.pauseSimulation() else engine.startSimulation()
                },
                onSetSpeed = { engine.setSpeed(it) }
            )

            // Event log
            EventLog(events = events)

            // Zoo map - fills remaining space
            Box(modifier = Modifier.weight(1f)) {
                ZooMapView(
                    grid = grid,
                    enclosures = zooState.enclosures,
                    facilities = zooState.facilities,
                    placementMode = placementMode,
                    onGridTap = { x, y -> engine.handleGridTap(x, y) }
                )
            }

            // Bottom bar
            GameBottomBar(
                placementMode = placementMode,
                onShopClick = { engine.toggleShop() },
                onCancelPlacement = { engine.cancelPlacement() }
            )
        }

        // Shop overlay
        if (showShop) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                ShopPanel(
                    money = zooState.money,
                    onBuyEnclosure = { type ->
                        engine.setPlacementMode(PlacementMode.PlacingEnclosure(type))
                    },
                    onBuyFacility = { type ->
                        engine.setPlacementMode(PlacementMode.PlacingFacility(type))
                    },
                    onPlacePath = {
                        engine.setPlacementMode(PlacementMode.PlacingPath)
                    },
                    onClose = { engine.toggleShop() }
                )
            }
        }

        // Enclosure detail overlay
        if (selectedEnclosure != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                EnclosureDetailPanel(
                    enclosure = selectedEnclosure!!,
                    money = zooState.money,
                    onBuyAnimal = { type, enc -> engine.buyAnimal(type, enc) },
                    onClose = { engine.selectEnclosure(null) }
                )
            }
        }
    }
}
