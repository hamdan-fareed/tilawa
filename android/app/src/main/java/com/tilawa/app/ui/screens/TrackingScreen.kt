package com.tilawa.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tilawa.app.domain.model.QuranVerse
import com.tilawa.app.domain.model.VerseCandidate
import com.tilawa.app.ui.theme.CorrectGreen
import com.tilawa.app.ui.theme.CorrectGreenBg
import com.tilawa.app.ui.theme.GreenPrimary
import com.tilawa.app.ui.theme.GreenPrimaryContainer

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrackingScreen(
    verse: QuranVerse,
    currentWordIndex: Int,
    matchedIndices: List<Int>,
    candidates: List<VerseCandidate>,
    isListening: Boolean,
    onStartMic: () -> Unit,
    onStopMic: () -> Unit,
    onReset: () -> Unit,
    onSelectSample: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val words = verse.textUthmani.split(" ").filter { it.isNotBlank() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main Display Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("verse_display_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Surah Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${verse.surahNameEn} (${verse.surah}:${verse.ayah})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = verse.surahName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Words Flow Layout (RTL Arabic style)
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quran_words_flow"),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    words.forEachIndexed { index, word ->
                        val isSpoken = matchedIndices.contains(index)
                        val isCurrent = index == currentWordIndex && isListening

                        val bgColor = when {
                            isCurrent -> GreenPrimaryContainer
                            isSpoken -> CorrectGreenBg
                            else -> Color.Transparent
                        }

                        val textColor = when {
                            isCurrent -> GreenPrimary
                            isSpoken -> CorrectGreen
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgColor)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = word,
                                fontSize = 24.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Waveform indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(36.dp)
                ) {
                    repeat(12) { i ->
                        val barHeight = if (isListening) (12 + (i * 7) % 24).dp else 8.dp
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(barHeight)
                                .clip(CircleShape)
                                .background(GreenPrimary)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isListening) {
                Button(
                    onClick = onStartMic,
                    modifier = Modifier.testTag("start_reciting_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Reciting")
                }
            } else {
                Button(
                    onClick = onStopMic,
                    modifier = Modifier.testTag("stop_reciting_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop")
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.testTag("reset_session_button"),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Corpus Samples Shelf
        Text(
            text = "Test Recitation Clips",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = verse.surah == 1 && verse.ayah == 1,
                onClick = { onSelectSample(1, 1) },
                label = { Text("Al-Fatihah 1:1") }
            )
            FilterChip(
                selected = verse.surah == 1 && verse.ayah == 2,
                onClick = { onSelectSample(1, 2) },
                label = { Text("Al-Fatihah 1:2") }
            )
            FilterChip(
                selected = verse.surah == 2 && verse.ayah == 255,
                onClick = { onSelectSample(2, 255) },
                label = { Text("Ayat Al-Kursi") }
            )
            FilterChip(
                selected = verse.surah == 112 && verse.ayah == 1,
                onClick = { onSelectSample(112, 1) },
                label = { Text("Al-Ikhlas 112:1") }
            )
        }
    }
}
