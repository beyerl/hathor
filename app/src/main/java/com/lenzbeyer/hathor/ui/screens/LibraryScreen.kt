package com.lenzbeyer.hathor.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lenzbeyer.hathor.data.PlaylistEntity
import com.lenzbeyer.hathor.ui.components.AngularTopBar
import com.lenzbeyer.hathor.ui.components.SecondaryButton

@Composable
fun LibraryScreen(
    vm: LibraryViewModel,
    onPlaylistClick: (String) -> Unit,
    onBack: () -> Unit,
    onNew: () -> Unit,
) {
    val playlists by vm.playlists.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        AngularTopBar(
            title = "LIBRARY",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                Box(Modifier.padding(end = 8.dp)) {
                    SecondaryButton("+ NEW", onClick = onNew)
                }
            },
        )
        if (playlists.isEmpty()) {
            EmptyLibrary()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            ) {
                items(playlists, key = { it.id }) { p ->
                    PlaylistCard(p, onClick = { onPlaylistClick(p.id) })
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "No playlists yet",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            "Paste a playlist URL on the home screen to begin.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlaylistCard(p: PlaylistEntity, onClick: () -> Unit) {
    OutlinedCard(
        shape = RectangleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(88.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(88.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RectangleShape),
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    "${p.folderArtist} — ${p.title}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "Album: ${p.album}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
    }
}
