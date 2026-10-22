/*
 * Lyra Music Project (2026)
 * Shnwaz (github.com/shnwazdeveloper)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * TV now-playing screen: artwork, title, progress and transport controls,
 * all D-pad navigable. Reads state from the shared PlayerConnection.
 */

package com.shnwaz.lyramusic.ui.tv

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.shnwaz.lyramusic.LocalPlayerConnection
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private fun formatMs(ms: Long): String {
    if (ms < 0) return "0:00"
    val totalSec = ms / 1000
    return "${totalSec / 60}:${(totalSec % 60).toString().padStart(2, '0')}"
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvPlayerScreen() {
    val playerConnection = LocalPlayerConnection.current ?: run {
        TvEmptyState("Nothing is playing yet.")
        return
    }

    val song by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle(initialValue = false)
    val player = playerConnection.player

    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(playerConnection) {
        while (isActive) {
            positionMs = player.currentPosition
            durationMs = player.duration.takeIf { it > 0 } ?: 0L
            delay(500)
        }
    }

    val current = song
    if (current == null) {
        TvEmptyState("Nothing is playing yet.")
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 64.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AsyncImage(
            model = current.song.thumbnailUrl,
            contentDescription = current.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(16.dp)),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = current.title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = current.artists.joinToString { it.name },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatMs(positionMs),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.width(12.dp))
            LinearProgressIndicator(
                progress = {
                    if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
                },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = formatMs(durationMs),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = { player.seekTo((positionMs - 10_000).coerceAtLeast(0)) }) {
                Text("-10s")
            }
            Button(onClick = { playerConnection.seekToPrevious() }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous")
            }
            Button(
                onClick = { if (isPlaying) player.pause() else player.play() },
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                )
            }
            Button(onClick = { playerConnection.seekToNext() }) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next")
            }
            Button(onClick = { player.seekTo(positionMs + 10_000) }) {
                Text("+10s")
            }
        }
    }
}
