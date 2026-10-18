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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tilawa.app.domain.model.CorrectionState
import com.tilawa.app.domain.model.WordStatus
import com.tilawa.app.ui.theme.CorrectGreen
import com.tilawa.app.ui.theme.CorrectGreenBg
import com.tilawa.app.ui.theme.GreenPrimary
import com.tilawa.app.ui.theme.SkippedRed
import com.tilawa.app.ui.theme.SkippedRedBg
import com.tilawa.app.ui.theme.SlipAmber
import com.tilawa.app.ui.theme.SlipAmberBg

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CorrectionScreen(
    state: CorrectionState?,
    onResolveWord: (Int, String) -> Unit,
    onStartPractice: () -> Unit,
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    if (state == null) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Select a verse to begin correction practice.")
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("correction_summary_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Accuracy", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${state.accuracy}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Slips Detected", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${state.slipsCount}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SlipAmber
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Progress", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${state.currentWordIndex}/${state.verdicts.size}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Word Verdicts Grid
        Text(
            text = "Recitation Word Feedback",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            state.verdicts.forEach { verdict ->
                val (bgColor, textColor, badge) = when (verdict.status) {
                    WordStatus.CORRECT -> Triple(CorrectGreenBg, CorrectGreen, "✓ CORRECT")
                    WordStatus.SLIP -> Triple(SlipAmberBg, SlipAmber, "⚠ SLIP")
                    WordStatus.SKIPPED -> Triple(SkippedRedBg, SkippedRed, "✕ SKIPPED")
                    WordStatus.PENDING -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurface, "PENDING")
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = verdict.expected,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = badge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        if (verdict.status == WordStatus.SLIP) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row {
                                OutlinedButton(
                                    onClick = { onResolveWord(verdict.index, "accept") },
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Accept", fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Button(
                                    onClick = { onResolveWord(verdict.index, "retry") },
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Retry", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onStartPractice,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .testTag("practice_recitation_button"),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(if (isListening) "Reciting..." else "Start Correction Session")
        }
    }
}
