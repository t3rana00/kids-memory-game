package com.example.kidsmemorygame

import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KidsMemoryGameApp()
        }
    }
}

@Composable
fun KidsMemoryGameApp() {
    var gridSize by remember { mutableStateOf(4) }
    var resetTrigger by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF6DD5FA), Color(0xFF2980B9))
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Kids' Memory Game",
                fontSize = 24.sp,
                color = Color.White,
                modifier = Modifier.padding(16.dp)
            )

            Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { gridSize = 2; resetTrigger++ }) { Text("Easy (2x2)") }
                Button(onClick = { gridSize = 4; resetTrigger++ }) { Text("Medium (4x4)") }
                Button(onClick = { gridSize = 6; resetTrigger++ }) { Text("Hard (6x6)") }
            }

            Button(
                onClick = { resetTrigger++ },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Reset Game")
            }

            MemoryGameGrid(gridSize, resetTrigger)
        }
    }
}

@Composable
fun MemoryGameGrid(gridSize: Int, resetTrigger: Int) {
    val context = LocalContext.current
    val totalCards = gridSize * gridSize

    val imageResources = listOf(
        R.drawable.giraffe,
        R.drawable.rabbit,
        R.drawable.penguine,
        R.drawable.zibra,
        R.drawable.lion,
        R.drawable.polar
    )

    val uniquePairsNeeded = totalCards / 2
    var cards by remember(resetTrigger) {
        mutableStateOf(
            (0 until uniquePairsNeeded).map { imageResources[it % imageResources.size] }
                .flatMap { listOf(it, it) }
                .shuffled()
        )
    }

    val flippedCards = remember { mutableStateListOf<Int>() }
    val matchedCards = remember { mutableStateListOf<Int>() }
    val coroutineScope = rememberCoroutineScope()
    var showGameOverDialog by remember { mutableStateOf(false) }

    LaunchedEffect(resetTrigger) {
        flippedCards.clear()
        matchedCards.clear()
    }

    LaunchedEffect(matchedCards.size) {
        if (matchedCards.size == totalCards) {
            showGameOverDialog = true
        }
    }

    if (showGameOverDialog) {
        AlertDialog(
            onDismissRequest = { showGameOverDialog = false },
            title = { Text("Game Over!") },
            text = { Text("Congratulations! You found all the pairs.") },
            confirmButton = {
                Button(onClick = { showGameOverDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    Column {
        for (row in 0 until gridSize) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                for (col in 0 until gridSize) {
                    val cardIndex = row * gridSize + col
                    if (cardIndex < cards.size) {
                        Card(
                            cardValue = cards[cardIndex],
                            isFlipped = flippedCards.contains(cardIndex) || matchedCards.contains(cardIndex),
                            gridSize = gridSize,
                            onClick = {
                                if (!matchedCards.contains(cardIndex) && flippedCards.size < 2) {
                                    flippedCards.add(cardIndex)
                                    if (flippedCards.size == 2) {
                                        coroutineScope.launch {
                                            delay(1000)
                                            checkForMatch(flippedCards, matchedCards, cards, context)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun checkForMatch(
    flippedCards: MutableList<Int>,
    matchedCards: MutableList<Int>,
    cards: List<Int>,
    context: android.content.Context
) {
    val matchSound = MediaPlayer.create(context, R.raw.match)

    if (flippedCards.size == 2) {
        val first = flippedCards[0]
        val second = flippedCards[1]
        if (cards[first] == cards[second]) {
            matchedCards.addAll(flippedCards)
            matchSound?.start()
        }
    }
    flippedCards.clear()
}

@Composable
fun Card(cardValue: Int, isFlipped: Boolean, gridSize: Int, onClick: () -> Unit) {
    val context = LocalContext.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val cardSize = (screenWidth / gridSize) - 16.dp

    val flipSound = remember { MediaPlayer.create(context, R.raw.flip) }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing)
    )

    val showFront = rotation <= 90f

    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(cardSize)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .background(
                color = if (showFront) Color.Gray else Color.White,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable {
                onClick()
                try {
                    flipSound?.start()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (showFront) {
            Text(
                text = "?",
                fontSize = 32.sp,
                color = Color.White
            )
        } else {
            Image(
                painter = painterResource(id = cardValue),
                contentDescription = "Card Image",
                modifier = Modifier.size(cardSize * 0.8f)
            )
        }
    }
}
