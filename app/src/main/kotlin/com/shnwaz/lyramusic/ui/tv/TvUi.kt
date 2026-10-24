/*
 * Lyra Music Project (2026)
 * Shnwaz (github.com/shnwazdeveloper)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * Shared TV theme, cards, rails and mini-player used by all TV screens.
 * Visual language mirrors the Lyra phone app: dark warm background,
 * orange/gold accent, white headlines.
 */

package com.shnwaz.lyramusic.ui.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import coil3.compose.AsyncImage

/** Lyra's orange/gold accent, as seen on the phone app. */
val LyraTvOrange = Color(0xFFE8A33D)

/** Lyra's dark warm background. */
val LyraTvBackground = Color(0xFF1A120B)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = LyraTvBackground,
            surface = Color(0xFF2A2018),
            surfaceVariant = Color(0xFF3A2C1C),
            primary = LyraTvOrange,
            onPrimary = LyraTvBackground,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = LyraTvOrange,
        ),
        content = content,
    )
}

/** Orange section heading, like "Favorit selamanya!" on the phone app. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = LyraTvOrange,
        modifier = Modifier.padding(start = 48.dp, top = 24.dp, bottom = 12.dp),
    )
}

/** Media card in Lyra's style: rounded image, white title, orange artist. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvMediaCard(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.width(180.dp),
    ) {
        Column {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = LyraTvOrange,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

/** Dark pill chip with white text, like the phone's mood chips. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvMoodChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.tv.material3.Button(
        onClick = onClick,
        modifier = modifier,
    ) {
        Text(
            text = label,
            color = Color.White,
        )
    }
}

@Composable
fun <T> TvRail(
    title: String,
    items: List<T>,
    key: (T) -> Any,
    card: @Composable (T) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TvSectionTitle(title = title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(items, key = key) { item ->
                card(item)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvEmptyState(message: String) {
    Row(
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
