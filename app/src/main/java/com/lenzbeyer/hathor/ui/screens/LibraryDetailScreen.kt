package com.lenzbeyer.hathor.ui.screens

import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lenzbeyer.hathor.data.TrackEntity
import com.lenzbeyer.hathor.ui.components.AngularTopBar
import com.lenzbeyer.hathor.ui.components.FaintDivider
import com.lenzbeyer.hathor.ui.components.SecondaryButton
import com.lenzbeyer.hathor.ui.components.SectionHeader
import com.lenzbeyer.hathor.ui.components.SolidDivider

@Composable
fun LibraryDetailScreen(
    vm: LibraryDetailViewModel,
    onBack: () -> Unit,
) {
    val playlist by vm.playlist.collectAsStateWithLifecycle()
    val tracks by vm.tracks.collectAsStateWithLifecycle()
    val p = playlist ?: return

    Column(Modifier.fillMaxSize()) {
        AngularTopBar(
            title = "${p.folderArtist} — ${p.title}",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Box(
                Modifier
                    .size(120.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RectangleShape),
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            ) {
                Text("PLAYLIST", style = MaterialTheme.typography.titleMedium)
                Text("${tracks.size} tracks", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "MP3 320 CBR",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    p.album,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SecondaryButton("OPEN FOLDER", onClick = { /* TODO: ACTION_VIEW SAF tree URI */ }, modifier = Modifier.weight(1f))
            SecondaryButton("RE-SYNC",     onClick = { /* TODO: enqueue re-sync */ },           modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.size(16.dp))
        SolidDivider()
        SectionHeader("TRACKS")

        LazyColumn(Modifier.fillMaxSize()) {
            items(tracks, key = { it.id }) { t ->
                TrackPlayRow(t)
                FaintDivider(Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun TrackPlayRow(t: TrackEntity) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "%02d".format(t.index),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(12.dp))
        Text(
            t.title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        t.durationMs?.let {
            val total = it / 1000
            Text(
                "%d:%02d".format(total / 60, total % 60),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { /* TODO: ACTION_VIEW the SAF MP3 URI */ }) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play")
        }
    }
}
