/*
 * Lyra Music Project (2026)
 * Shnwaz (github.com/shnwazdeveloper)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * TV now-playing screen, horizontal layout: player (artwork, title,
 * progress, controls) on the left, up-next queue on the right.
 * D-pad LEFT/RIGHT moves between player and queue, UP/DOWN in the queue.
 */

package com.shnwaz.lyramusic.ui.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
import com.shnwaz.lyramusic.extensions.metadata
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvPlayerScreen() {
    val playerConnection = LocalPlayerConnection.current ?: run {
        TvEmptyState("Nothing is playing yet.")
        return
    }

    val song by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle(initialValue = false)
    val queueWindows by playerConnection.queueWindows.collectAsState()
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

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp),
    ) {
        // Left: player
        Column(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AsyncImage(
                model = current.song.thumbnailUrl,
                contentDescription = current.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = current.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = current.artists.joinToString { it.name },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
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
                    color = Color.White,
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
                    color = Color.White,
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

        Spacer(Modifier.width(32.dp))

        // Right: playlist queue
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight(),
        ) {
            if (queueWindows.isNotEmpty()) {
                Text(
                    text = "Up next (${queueWindows.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3),
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    itemsIndexed(queueWindows, key = { idx, _ -> idx }) { idx, window ->
                        val meta = window.mediaItem.metadata
                        Button(
                            onClick = { player.seekToDefaultPosition(idx) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "${idx + 1}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF2196F3),
                                    modifier = Modifier.width(36.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = meta?.title ?: "Unknown",
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = meta?.artists?.joinToString { it.name } ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "No queue",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val s = (ms / 1000).toInt()
    return "%d:%02d".format(s / 60, s % 60)
}
