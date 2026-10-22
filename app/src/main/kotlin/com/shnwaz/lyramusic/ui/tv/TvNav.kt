/*
 * Lyra Music Project (2026)
 * Shnwaz (github.com/shnwazdeveloper)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * TV navigation: top tab bar (Home/Search/Library/Player) plus a mini
 * now-playing bar, in Lyra's dark visual language. D-pad navigable.
 */

package com.shnwaz.lyramusic.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.shnwaz.lyramusic.LocalPlayerConnection

private enum class TvTab(val label: String) {
    Home("Home"),
    Search("Search"),
    Library("Library"),
    Player("Player"),
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvNav() {
    var tab by rememberSaveable { mutableStateOf(TvTab.Home) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Top tab bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Lyra TV",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(end = 16.dp),
            )
            TvTab.entries.forEach { entry ->
                val selected = tab == entry
                Button(
                    onClick = { tab = entry },
                    colors = ButtonDefaults.colors(
                        containerColor = if (selected) LyraTvOrange
                        else Color(0xFF2A2018),
                        contentColor = if (selected) LyraTvBackground
                        else Color.White,
                    ),
                ) {
                    Text(entry.label)
                }
            }
        }

        // Content
        when (tab) {
            TvTab.Home -> TvHomeScreen(onOpenPlayer = { tab = TvTab.Player })
            TvTab.Search -> TvSearchScreen()
            TvTab.Library -> TvLibraryScreen()
            TvTab.Player -> TvPlayerScreen()
        }

        Spacer(Modifier.height(8.dp))

        // Mini now-playing bar (hidden on the Player tab itself)
        if (tab != TvTab.Player) {
            TvMiniPlayerBar(onOpenPlayer = { tab = TvTab.Player })
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
            .padding(horizontal = 48.dp, vertical = 8.dp),
        shape = androidx.tv.material3.ShapeDefaults.Large,
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
                    color = LyraTvOrange,
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
                    containerColor = LyraTvOrange,
                    contentColor = LyraTvBackground,
                ),
            ) {
                Text(if (isPlaying) "Pause" else "Play")
            }
        }
    }
}
