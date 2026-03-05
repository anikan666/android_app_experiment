package com.zooempire.ui.zoo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.zooempire.engine.ZooGameEngine
import com.zooempire.model.*
import com.zooempire.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun ZooMapView(
    grid: Array<Array<GridCell>>,
    enclosures: List<Enclosure>,
    facilities: List<Facility>,
    placementMode: PlacementMode,
    onGridTap: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 3f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
            .pointerInput(placementMode) {
                detectTapGestures { offset ->
                    val cellSize = (size.width.toFloat() / ZooGameEngine.GRID_SIZE) * scale
                    val gridX = ((offset.x - offsetX) / cellSize).toInt()
                    val gridY = ((offset.y - offsetY) / cellSize).toInt()
                    if (gridX in 0 until ZooGameEngine.GRID_SIZE && gridY in 0 until ZooGameEngine.GRID_SIZE) {
                        onGridTap(gridX, gridY)
                    }
                }
            }
    ) {
        val cellSize = (size.width / ZooGameEngine.GRID_SIZE) * scale

        translate(left = offsetX, top = offsetY) {
            // Draw grass background
            drawRect(
                color = Color(0xFF8BC34A),
                topLeft = Offset.Zero,
                size = Size(cellSize * ZooGameEngine.GRID_SIZE, cellSize * ZooGameEngine.GRID_SIZE)
            )

            // Draw grid lines
            for (i in 0..ZooGameEngine.GRID_SIZE) {
                drawLine(
                    color = Color(0x337CB342),
                    start = Offset(i * cellSize, 0f),
                    end = Offset(i * cellSize, cellSize * ZooGameEngine.GRID_SIZE),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color(0x337CB342),
                    start = Offset(0f, i * cellSize),
                    end = Offset(cellSize * ZooGameEngine.GRID_SIZE, i * cellSize),
                    strokeWidth = 1f
                )
            }

            // Draw cells
            for (y in grid.indices) {
                for (x in grid[y].indices) {
                    val cell = grid[y][x]
                    val topLeft = Offset(x * cellSize, y * cellSize)

                    when (cell.content) {
                        is CellContent.EnclosureCell -> {
                            val enc = enclosures.find { it.id == (cell.content as CellContent.EnclosureCell).enclosureId }
                            if (enc != null) {
                                drawRect(
                                    color = Color(enc.type.color),
                                    topLeft = topLeft,
                                    size = Size(cellSize, cellSize)
                                )
                                // Draw border
                                drawRect(
                                    color = Color(0xFF5D4037),
                                    topLeft = topLeft,
                                    size = Size(cellSize, cellSize),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                                )
                                // Draw animal emojis in the top-left cell of each enclosure
                                if (x == enc.gridX && y == enc.gridY && enc.animals.isNotEmpty()) {
                                    drawAnimalEmojis(enc, topLeft, cellSize)
                                }
                            }
                        }
                        is CellContent.FacilityCell -> {
                            drawRect(
                                color = Color(0xFFE0E0E0),
                                topLeft = topLeft,
                                size = Size(cellSize, cellSize)
                            )
                            drawRect(
                                color = Color(0xFF9E9E9E),
                                topLeft = topLeft,
                                size = Size(cellSize, cellSize),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                            )
                            val fac = facilities.find { it.id == (cell.content as CellContent.FacilityCell).facilityId }
                            if (fac != null) {
                                drawContext.canvas.nativeCanvas.drawText(
                                    fac.type.emoji,
                                    topLeft.x + cellSize * 0.2f,
                                    topLeft.y + cellSize * 0.75f,
                                    android.graphics.Paint().apply {
                                        textSize = cellSize * 0.55f
                                    }
                                )
                            }
                        }
                        is CellContent.Path -> {
                            drawRect(
                                color = Color(0xFFD7CCC8),
                                topLeft = topLeft,
                                size = Size(cellSize, cellSize)
                            )
                        }
                        is CellContent.Entrance -> {
                            drawRect(
                                color = Color(0xFF8D6E63),
                                topLeft = topLeft,
                                size = Size(cellSize, cellSize)
                            )
                            drawContext.canvas.nativeCanvas.drawText(
                                "\uD83C\uDFDF\uFE0F",
                                topLeft.x + cellSize * 0.15f,
                                topLeft.y + cellSize * 0.75f,
                                android.graphics.Paint().apply {
                                    textSize = cellSize * 0.55f
                                }
                            )
                        }
                        CellContent.Empty -> {
                            // Highlight if in placement mode
                            if (placementMode !is PlacementMode.None) {
                                drawRect(
                                    color = Color(0x2200E676),
                                    topLeft = topLeft,
                                    size = Size(cellSize, cellSize)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawAnimalEmojis(enclosure: Enclosure, topLeft: Offset, cellSize: Float) {
    val animals = enclosure.animals
    val emojiSize = cellSize * 0.4f
    animals.forEachIndexed { index, animal ->
        val col = index % 2
        val row = index / 2
        drawContext.canvas.nativeCanvas.drawText(
            animal.type.emoji,
            topLeft.x + col * cellSize + cellSize * 0.1f,
            topLeft.y + row * cellSize + emojiSize + cellSize * 0.1f,
            android.graphics.Paint().apply {
                textSize = emojiSize
            }
        )
    }
}
