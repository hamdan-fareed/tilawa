package com.tilawa.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tilawa.app.data.QuranRepository
import com.tilawa.app.domain.model.RecitationMode
import com.tilawa.app.ui.screens.CorrectionScreen
import com.tilawa.app.ui.screens.QuranExplorerScreen
import com.tilawa.app.ui.screens.TrackingScreen
import com.tilawa.app.ui.theme.GreenPrimary
import com.tilawa.app.ui.theme.GreenPrimaryContainer
import com.tilawa.app.ui.theme.TilawaTheme
import com.tilawa.app.ui.viewmodel.TilawaViewModel

enum class NavigationDestination {
    TRACKING,
    CORRECTION,
    EXPLORER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TilawaApp(
    viewModel: TilawaViewModel = viewModel()
) {
    TilawaTheme {
        var currentDest by remember { mutableStateOf(NavigationDestination.TRACKING) }

        val verse by viewModel.currentVerse.collectAsState()
        val wordIndex by viewModel.currentWordIndex.collectAsState()
        val matched by viewModel.matchedIndices.collectAsState()
        val candidates by viewModel.candidates.collectAsState()
        val isListening by viewModel.isListening.collectAsState()
        val correctionState by viewModel.correctionState.collectAsState()

        val repository = remember { QuranRepository() }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Tilawa تلاوة",
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = GreenPrimaryContainer
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                    NavigationBarItem(
                        selected = currentDest == NavigationDestination.TRACKING,
                        onClick = {
                            currentDest = NavigationDestination.TRACKING
                            viewModel.setMode(RecitationMode.TRACKING)
                        },
                        icon = { Icon(Icons.Default.Mic, contentDescription = "Tracking") },
                        label = { Text("Tracking") },
                        modifier = Modifier.testTag("nav_tracking")
                    )
                    NavigationBarItem(
                        selected = currentDest == NavigationDestination.CORRECTION,
                        onClick = {
                            currentDest = NavigationDestination.CORRECTION
                            viewModel.setMode(RecitationMode.CORRECTION)
                        },
                        icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Correction") },
                        label = { Text("Correction") },
                        modifier = Modifier.testTag("nav_correction")
                    )
                    NavigationBarItem(
                        selected = currentDest == NavigationDestination.EXPLORER,
                        onClick = { currentDest = NavigationDestination.EXPLORER },
                        icon = { Icon(Icons.Default.MenuBook, contentDescription = "Browse") },
                        label = { Text("Browse") },
                        modifier = Modifier.testTag("nav_explorer")
                    )
                }
            }
        ) { paddingValues ->
            when (currentDest) {
                NavigationDestination.TRACKING -> {
                    TrackingScreen(
                        verse = verse,
                        currentWordIndex = wordIndex,
                        matchedIndices = matched,
                        candidates = candidates,
                        isListening = isListening,
                        onStartMic = { viewModel.startListening() },
                        onStopMic = { viewModel.stopListening() },
                        onReset = { viewModel.reset() },
                        onSelectSample = { s, a -> viewModel.simulateRecitation(s, a) },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                NavigationDestination.CORRECTION -> {
                    CorrectionScreen(
                        state = correctionState,
                        onResolveWord = { idx, action -> viewModel.resolveWord(idx, action) },
                        onStartPractice = { viewModel.simulateRecitation(verse.surah, verse.ayah) },
                        isListening = isListening,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                NavigationDestination.EXPLORER -> {
                    QuranExplorerScreen(
                        verses = repository.getAllVerses(),
                        onSelectVerse = { s, a ->
                            viewModel.selectVerse(s, a)
                            currentDest = NavigationDestination.TRACKING
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}
