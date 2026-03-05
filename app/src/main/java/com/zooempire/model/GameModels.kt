package com.zooempire.model

import androidx.compose.ui.graphics.Color

enum class AnimalType(
    val displayName: String,
    val emoji: String,
    val cost: Int,
    val dailyUpkeep: Int,
    val visitorAttraction: Int,
    val requiredEnclosure: EnclosureType,
    val baseHappiness: Float
) {
    LION("Lion", "\uD83E\uDD81", 5000, 200, 50, EnclosureType.SAVANNA, 0.7f),
    ELEPHANT("Elephant", "\uD83D\uDC18", 8000, 350, 70, EnclosureType.SAVANNA, 0.6f),
    PENGUIN("Penguin", "\uD83D\uDC27", 3000, 150, 40, EnclosureType.ARCTIC, 0.8f),
    POLAR_BEAR("Polar Bear", "\uD83D\uDC3B\u200D❄\uFE0F", 7000, 300, 60, EnclosureType.ARCTIC, 0.65f),
    MONKEY("Monkey", "\uD83D\uDC12", 2000, 100, 35, EnclosureType.TROPICAL, 0.85f),
    PARROT("Parrot", "\uD83E\uDD9C", 1500, 80, 25, EnclosureType.TROPICAL, 0.9f),
    CROCODILE("Crocodile", "\uD83D\uDC0A", 4000, 180, 45, EnclosureType.WETLAND, 0.75f),
    FLAMINGO("Flamingo", "\uD83E\uDDA9", 2500, 120, 30, EnclosureType.WETLAND, 0.8f),
    PANDA("Panda", "\uD83D\uDC3C", 10000, 400, 90, EnclosureType.FOREST, 0.5f),
    WOLF("Wolf", "\uD83D\uDC3A", 3500, 160, 40, EnclosureType.FOREST, 0.7f),
    GIRAFFE("Giraffe", "\uD83E\uDD92", 6000, 250, 55, EnclosureType.SAVANNA, 0.65f),
    TIGER("Tiger", "\uD83D\uDC2F", 6500, 280, 65, EnclosureType.TROPICAL, 0.6f)
}

enum class EnclosureType(
    val displayName: String,
    val color: Long,
    val cost: Int,
    val capacity: Int
) {
    SAVANNA("Savanna", 0xFFE8D44D, 3000, 4),
    ARCTIC("Arctic", 0xFFB3E5FC, 4000, 3),
    TROPICAL("Tropical", 0xFF66BB6A, 3500, 4),
    WETLAND("Wetland", 0xFF4DB6AC, 3000, 3),
    FOREST("Forest", 0xFF2E7D32, 4500, 3)
}

enum class FacilityType(
    val displayName: String,
    val emoji: String,
    val cost: Int,
    val dailyUpkeep: Int,
    val visitorBonus: Int,
    val happinessBonus: Float
) {
    FOOD_STAND("Food Stand", "\uD83C\uDF54", 1000, 50, 15, 0.0f),
    GIFT_SHOP("Gift Shop", "\uD83C\uDF81", 1500, 60, 20, 0.0f),
    RESTROOM("Restroom", "\uD83D\uDEBB", 800, 30, 10, 0.0f),
    BENCH("Bench", "\uD83E\uDE91", 200, 0, 5, 0.0f),
    FOUNTAIN("Fountain", "⛲", 2000, 40, 25, 0.05f),
    PLAYGROUND("Playground", "\uD83C\uDFA0", 3000, 80, 30, 0.0f),
    VETERINARY("Veterinary", "\uD83C\uDFE5", 5000, 200, 0, 0.15f)
}

data class Animal(
    val id: Int,
    val type: AnimalType,
    var happiness: Float = type.baseHappiness,
    var health: Float = 1.0f,
    val enclosureId: Int
)

data class Enclosure(
    val id: Int,
    val type: EnclosureType,
    val gridX: Int,
    val gridY: Int,
    val animals: MutableList<Animal> = mutableListOf()
) {
    val isFull: Boolean get() = animals.size >= type.capacity
}

data class Facility(
    val id: Int,
    val type: FacilityType,
    val gridX: Int,
    val gridY: Int
)

data class ZooState(
    val money: Int = 25000,
    val day: Int = 1,
    val enclosures: MutableList<Enclosure> = mutableListOf(),
    val facilities: MutableList<Facility> = mutableListOf(),
    val visitors: Int = 0,
    val dailyRevenue: Int = 0,
    val dailyExpenses: Int = 0,
    val zooRating: Float = 0f,
    val ticketPrice: Int = 20,
    val zooName: String = "My Zoo"
)

data class GridCell(
    val x: Int,
    val y: Int,
    val content: CellContent = CellContent.Empty
)

sealed class CellContent {
    data object Empty : CellContent()
    data class EnclosureCell(val enclosureId: Int) : CellContent()
    data class FacilityCell(val facilityId: Int) : CellContent()
    data object Path : CellContent()
    data object Entrance : CellContent()
}

sealed class PlacementMode {
    data object None : PlacementMode()
    data class PlacingEnclosure(val type: EnclosureType) : PlacementMode()
    data class PlacingFacility(val type: FacilityType) : PlacementMode()
    data object PlacingPath : PlacementMode()
}

data class GameEvent(
    val message: String,
    val isPositive: Boolean,
    val day: Int
)
