package com.zooempire.engine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zooempire.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

class ZooGameEngine : ViewModel() {
    companion object {
        const val GRID_SIZE = 20
        const val ENCLOSURE_SIZE = 2
    }

    private val _zooState = MutableStateFlow(ZooState())
    val zooState: StateFlow<ZooState> = _zooState.asStateFlow()

    private val _grid = MutableStateFlow(Array(GRID_SIZE) { y -> Array(GRID_SIZE) { x -> GridCell(x, y) } })
    val grid: StateFlow<Array<Array<GridCell>>> = _grid.asStateFlow()

    private val _placementMode = MutableStateFlow<PlacementMode>(PlacementMode.None)
    val placementMode: StateFlow<PlacementMode> = _placementMode.asStateFlow()

    private val _events = MutableStateFlow<List<GameEvent>>(emptyList())
    val events: StateFlow<List<GameEvent>> = _events.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _showShop = MutableStateFlow(false)
    val showShop: StateFlow<Boolean> = _showShop.asStateFlow()

    private val _selectedEnclosure = MutableStateFlow<Enclosure?>(null)
    val selectedEnclosure: StateFlow<Enclosure?> = _selectedEnclosure.asStateFlow()

    private val _speed = MutableStateFlow(1)
    val speed: StateFlow<Int> = _speed.asStateFlow()

    private var nextEnclosureId = 1
    private var nextAnimalId = 1
    private var nextFacilityId = 1

    init {
        // Place entrance
        val g = _grid.value
        g[GRID_SIZE - 1][GRID_SIZE / 2] = GridCell(GRID_SIZE / 2, GRID_SIZE - 1, CellContent.Entrance)
        // Place initial path from entrance
        for (y in (GRID_SIZE - 2) downTo (GRID_SIZE - 4)) {
            g[y][GRID_SIZE / 2] = GridCell(GRID_SIZE / 2, y, CellContent.Path)
        }
        _grid.value = g
    }

    fun startSimulation() {
        if (_isRunning.value) return
        _isRunning.value = true
        viewModelScope.launch {
            while (_isRunning.value) {
                val delayMs = when (_speed.value) {
                    1 -> 3000L
                    2 -> 1500L
                    3 -> 750L
                    else -> 3000L
                }
                delay(delayMs)
                simulateDay()
            }
        }
    }

    fun pauseSimulation() {
        _isRunning.value = false
    }

    fun setSpeed(speed: Int) {
        _speed.value = speed.coerceIn(1, 3)
    }

    fun toggleShop() {
        _showShop.value = !_showShop.value
        _placementMode.value = PlacementMode.None
        _selectedEnclosure.value = null
    }

    fun selectEnclosure(enclosure: Enclosure?) {
        _selectedEnclosure.value = enclosure
        _showShop.value = false
    }

    fun setPlacementMode(mode: PlacementMode) {
        _placementMode.value = mode
        _showShop.value = false
        _selectedEnclosure.value = null
    }

    fun cancelPlacement() {
        _placementMode.value = PlacementMode.None
    }

    fun handleGridTap(gridX: Int, gridY: Int) {
        when (val mode = _placementMode.value) {
            is PlacementMode.PlacingEnclosure -> placeEnclosure(mode.type, gridX, gridY)
            is PlacementMode.PlacingFacility -> placeFacility(mode.type, gridX, gridY)
            is PlacementMode.PlacingPath -> placePath(gridX, gridY)
            PlacementMode.None -> {
                // Check if tapped on an enclosure
                val cell = _grid.value.getOrNull(gridY)?.getOrNull(gridX) ?: return
                when (val content = cell.content) {
                    is CellContent.EnclosureCell -> {
                        val enc = _zooState.value.enclosures.find { it.id == content.enclosureId }
                        selectEnclosure(enc)
                    }
                    else -> selectEnclosure(null)
                }
            }
        }
    }

    private fun placeEnclosure(type: EnclosureType, gridX: Int, gridY: Int) {
        val state = _zooState.value
        if (state.money < type.cost) {
            addEvent("Not enough money for ${type.displayName}!", false)
            return
        }

        // Check if 2x2 area is free
        for (dy in 0 until ENCLOSURE_SIZE) {
            for (dx in 0 until ENCLOSURE_SIZE) {
                val cx = gridX + dx
                val cy = gridY + dy
                if (cx >= GRID_SIZE || cy >= GRID_SIZE) {
                    addEvent("Can't place here - out of bounds!", false)
                    return
                }
                if (_grid.value[cy][cx].content !is CellContent.Empty) {
                    addEvent("Can't place here - space occupied!", false)
                    return
                }
            }
        }

        val enclosure = Enclosure(nextEnclosureId++, type, gridX, gridY)
        val g = _grid.value
        for (dy in 0 until ENCLOSURE_SIZE) {
            for (dx in 0 until ENCLOSURE_SIZE) {
                g[gridY + dy][gridX + dx] = GridCell(gridX + dx, gridY + dy, CellContent.EnclosureCell(enclosure.id))
            }
        }
        _grid.value = g

        val newState = state.copy(money = state.money - type.cost)
        newState.enclosures.addAll(state.enclosures)
        newState.enclosures.add(enclosure)
        newState.facilities.addAll(state.facilities)
        _zooState.value = newState

        addEvent("Built ${type.displayName} enclosure! (-$${type.cost})", true)
        _placementMode.value = PlacementMode.None
    }

    private fun placeFacility(type: FacilityType, gridX: Int, gridY: Int) {
        val state = _zooState.value
        if (state.money < type.cost) {
            addEvent("Not enough money for ${type.displayName}!", false)
            return
        }

        if (gridX >= GRID_SIZE || gridY >= GRID_SIZE) return
        if (_grid.value[gridY][gridX].content !is CellContent.Empty) {
            addEvent("Can't place here - space occupied!", false)
            return
        }

        val facility = Facility(nextFacilityId++, type, gridX, gridY)
        val g = _grid.value
        g[gridY][gridX] = GridCell(gridX, gridY, CellContent.FacilityCell(facility.id))
        _grid.value = g

        val newState = state.copy(money = state.money - type.cost)
        newState.enclosures.addAll(state.enclosures)
        newState.facilities.addAll(state.facilities)
        newState.facilities.add(facility)
        _zooState.value = newState

        addEvent("Built ${type.displayName}! (-$${type.cost})", true)
        _placementMode.value = PlacementMode.None
    }

    private fun placePath(gridX: Int, gridY: Int) {
        if (gridX >= GRID_SIZE || gridY >= GRID_SIZE) return
        if (_grid.value[gridY][gridX].content !is CellContent.Empty) return

        val g = _grid.value
        g[gridY][gridX] = GridCell(gridX, gridY, CellContent.Path)
        _grid.value = g
    }

    fun buyAnimal(type: AnimalType, enclosure: Enclosure) {
        val state = _zooState.value
        if (state.money < type.cost) {
            addEvent("Not enough money for a ${type.displayName}!", false)
            return
        }
        if (enclosure.isFull) {
            addEvent("Enclosure is full!", false)
            return
        }
        if (type.requiredEnclosure != enclosure.type) {
            addEvent("${type.displayName} needs a ${type.requiredEnclosure.displayName} enclosure!", false)
            return
        }

        val animal = Animal(nextAnimalId++, type, enclosureId = enclosure.id)
        enclosure.animals.add(animal)

        _zooState.value = state.copy(money = state.money - type.cost)
        // preserve mutable lists
        _zooState.value.enclosures.addAll(state.enclosures)
        _zooState.value.facilities.addAll(state.facilities)

        addEvent("Bought a ${type.displayName}! (-$${type.cost})", true)
        _selectedEnclosure.value = enclosure
    }

    fun setTicketPrice(price: Int) {
        val state = _zooState.value
        _zooState.value = state.copy(ticketPrice = price.coerceIn(5, 100))
        _zooState.value.enclosures.addAll(state.enclosures)
        _zooState.value.facilities.addAll(state.facilities)
    }

    fun setZooName(name: String) {
        val state = _zooState.value
        _zooState.value = state.copy(zooName = name)
        _zooState.value.enclosures.addAll(state.enclosures)
        _zooState.value.facilities.addAll(state.facilities)
    }

    private fun simulateDay() {
        val state = _zooState.value

        // Calculate total animal attraction
        val totalAttraction = state.enclosures.sumOf { enc ->
            enc.animals.sumOf { animal ->
                (animal.type.visitorAttraction * animal.happiness * animal.health).roundToInt()
            }
        }

        // Facility bonuses
        val facilityVisitorBonus = state.facilities.sumOf { it.type.visitorBonus }
        val facilityHappinessBonus = state.facilities.sumOf { (it.type.happinessBonus * 100).toInt() } / 100f

        // Calculate visitors based on attraction and ticket price
        val priceMultiplier = when {
            state.ticketPrice <= 10 -> 1.5f
            state.ticketPrice <= 20 -> 1.2f
            state.ticketPrice <= 30 -> 1.0f
            state.ticketPrice <= 50 -> 0.7f
            else -> 0.4f
        }
        val baseVisitors = ((totalAttraction + facilityVisitorBonus) * priceMultiplier).roundToInt()
        val visitors = max(0, baseVisitors + Random.nextInt(-5, 10))

        // Revenue
        val ticketRevenue = visitors * state.ticketPrice
        val facilityRevenue = (visitors * state.facilities.size * 2)
        val totalRevenue = ticketRevenue + facilityRevenue

        // Expenses
        val animalUpkeep = state.enclosures.sumOf { enc ->
            enc.animals.sumOf { it.type.dailyUpkeep }
        }
        val facilityUpkeep = state.facilities.sumOf { it.type.dailyUpkeep }
        val totalExpenses = animalUpkeep + facilityUpkeep

        // Update animal happiness
        state.enclosures.forEach { enc ->
            enc.animals.forEach { animal ->
                val crowdingPenalty = if (enc.animals.size > enc.type.capacity / 2) -0.02f else 0.01f
                val randomChange = Random.nextFloat() * 0.06f - 0.03f
                animal.happiness = (animal.happiness + crowdingPenalty + randomChange + facilityHappinessBonus * 0.1f)
                    .coerceIn(0.1f, 1.0f)
                // Health can degrade if happiness is very low
                if (animal.happiness < 0.3f) {
                    animal.health = (animal.health - 0.05f).coerceIn(0.1f, 1.0f)
                } else {
                    animal.health = (animal.health + 0.02f).coerceIn(0.1f, 1.0f)
                }
            }
        }

        // Zoo rating
        val totalAnimals = state.enclosures.sumOf { it.animals.size }
        val avgHappiness = if (totalAnimals > 0) {
            state.enclosures.flatMap { it.animals }.map { it.happiness }.average().toFloat()
        } else 0f
        val animalVariety = state.enclosures.flatMap { it.animals }.map { it.type }.distinct().size
        val rating = min(5f, (avgHappiness * 2f + animalVariety * 0.3f + state.facilities.size * 0.1f))

        // Random events
        if (Random.nextFloat() < 0.1f && totalAnimals > 0) {
            val eventType = Random.nextInt(5)
            when (eventType) {
                0 -> {
                    val bonus = Random.nextInt(500, 2000)
                    addEvent("A donor gave your zoo $${bonus}!", true)
                    _zooState.value = state.copy(
                        money = state.money + totalRevenue - totalExpenses + bonus,
                        day = state.day + 1,
                        visitors = visitors,
                        dailyRevenue = totalRevenue,
                        dailyExpenses = totalExpenses,
                        zooRating = rating
                    )
                    _zooState.value.enclosures.addAll(state.enclosures)
                    _zooState.value.facilities.addAll(state.facilities)
                    return
                }
                1 -> addEvent("A school group visited - great publicity!", true)
                2 -> {
                    val randomAnimal = state.enclosures.flatMap { it.animals }.randomOrNull()
                    if (randomAnimal != null) {
                        randomAnimal.happiness = min(1f, randomAnimal.happiness + 0.1f)
                        addEvent("${randomAnimal.type.displayName} is feeling extra happy today!", true)
                    }
                }
                3 -> addEvent("Local news featured your zoo!", true)
                4 -> {
                    val randomAnimal = state.enclosures.flatMap { it.animals }.randomOrNull()
                    if (randomAnimal != null) {
                        randomAnimal.happiness = max(0.1f, randomAnimal.happiness - 0.15f)
                        addEvent("${randomAnimal.type.displayName} is feeling stressed.", false)
                    }
                }
            }
        }

        val newMoney = state.money + totalRevenue - totalExpenses
        _zooState.value = state.copy(
            money = newMoney,
            day = state.day + 1,
            visitors = visitors,
            dailyRevenue = totalRevenue,
            dailyExpenses = totalExpenses,
            zooRating = rating
        )
        _zooState.value.enclosures.addAll(state.enclosures)
        _zooState.value.facilities.addAll(state.facilities)

        if (newMoney < 0) {
            addEvent("WARNING: Your zoo is losing money!", false)
        }
    }

    private fun addEvent(message: String, isPositive: Boolean) {
        val day = _zooState.value.day
        val newEvents = _events.value.toMutableList()
        newEvents.add(0, GameEvent(message, isPositive, day))
        if (newEvents.size > 20) newEvents.removeAt(newEvents.size - 1)
        _events.value = newEvents
    }
}
