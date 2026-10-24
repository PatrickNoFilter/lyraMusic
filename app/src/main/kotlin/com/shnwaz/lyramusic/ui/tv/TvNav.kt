/*
 * Lyra Music Project (2026)
 * Shnwaz (github.com/shnwazdeveloper)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * TV navigation: hosts Lyra's ORIGINAL screens (via navigationBuilder) with
 * only TV layout patches — top tab bar for D-pad, overscan-safe padding,
 * mini now-playing bar. No reimplementation of Lyra UI.
 */

package com.shnwaz.lyramusic.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import coil3.compose.AsyncImage
import com.shnwaz.lyramusic.BuildConfig
import com.shnwaz.lyramusic.LocalPlayerConnection
import com.shnwaz.lyramusic.ui.screens.Screens
import com.shnwaz.lyramusic.ui.screens.navigationBuilder

private val TvOrange = Color(0xFFE8A33D)
private val TvBackground = Color(0xFF1A120B)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = TvBackground,
            surface = Color(0xFF2A2018),
            primary = TvOrange,
            onPrimary = TvBackground,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = TvOrange,
        ),
        content = content,
    )
}

private enum class TvTab(val label: String, val route: String) {
    Home("Home", Screens.Home.route),
    Search("Search", Screens.Search.route),
    Library("Library", Screens.Library.route),
    Player("Player", "tv_player"),
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun TvNav() {
    val navController = rememberNavController()
    var tab by rememberSaveable { mutableStateOf(TvTab.Home) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    fun selectTab(entry: TvTab) {
        tab = entry
        navController.navigate(entry.route) {
            popUpTo(Screens.Home.route) { inclusive = false }
            launchSingleTop = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // TV overscan-safe padding
            .padding(horizontal = 48.dp),
    ) {
        // Top tab bar — the TV layout patch for D-pad navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Lyra",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TvOrange,
                modifier = Modifier.padding(end = 16.dp),
            )
            TvTab.entries.forEach { entry ->
                val selected = tab == entry
                Button(
                    onClick = { selectTab(entry) },
                    colors = ButtonDefaults.colors(
                        containerColor = if (selected) TvOrange else Color(0xFF2A2018),
                        contentColor = if (selected) TvBackground else Color.White,
                    ),
                ) {
                    Text(entry.label)
                }
            }
        }

        // Original Lyra screens, hosted as-is
        Box(modifier = Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = Screens.Home.route,
            ) {
                navigationBuilder(
                    navController = navController,
                    scrollBehavior = scrollBehavior,
                    latestVersionName = BuildConfig.VERSION_NAME,
                )
                composable("tv_player") {
                    TvPlayerScreen()
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Mini now-playing bar (hidden on the Player tab itself)
        if (tab != TvTab.Player) {
            TvMiniPlayerBar(onOpenPlayer = { selectTab(TvTab.Player) })
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvMiniPlayerBar(onOpenPlayer: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val song by playerConnection.currentSong.collectAsState(initial = null)
    val current = song ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState(initial = false)

    Surface(
        onClick = onOpenPlayer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = current.song.thumbnailUrl,
                contentDescription = current.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = current.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = current.artists.joinToString { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = TvOrange,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                onClick = {
                    val player = playerConnection.player
                    if (isPlaying) player.pause() else player.play()
                },
                colors = ButtonDefaults.colors(
                    containerColor = TvOrange,
                    contentColor = TvBackground,
                ),
            ) {
                Text(if (isPlaying) "Pause" else "Play")
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvEmptyState(message: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.6f),
        )
    }
}
