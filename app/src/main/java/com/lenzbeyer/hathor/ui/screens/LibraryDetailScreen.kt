package com.lenzbeyer.hathor.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lenzbeyer.hathor.data.PlaylistEntity
import com.lenzbeyer.hathor.data.TrackEntity
import com.lenzbeyer.hathor.domain.TrackStatus
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
    val resyncError by vm.resyncError.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val p = playlist ?: return

    LaunchedEffect(resyncError) {
        resyncError?.let { Toast.makeText(ctx, it, Toast.LENGTH_LONG).show() }
    }

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
            SecondaryButton(
                "OPEN FOLDER",
                onClick = { openFolder(ctx, p) },
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                "RE-SYNC",
                onClick = { vm.resync() },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.size(16.dp))
        SolidDivider()
        SectionHeader("TRACKS")

        LazyColumn(Modifier.fillMaxSize()) {
            items(tracks, key = { it.id }) { t ->
                TrackPlayRow(t, onPlay = { playTrack(ctx, t) })
                FaintDivider(Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

private fun openFolder(ctx: android.content.Context, p: PlaylistEntity) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(p.playlistFolderUri), "vnd.android.document/directory")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
    }
    val launched = runCatching { ctx.startActivity(intent) }.isSuccess
    if (!launched) {
        Toast.makeText(ctx, "No app available to open this folder", Toast.LENGTH_SHORT).show()
    }
}

private fun playTrack(ctx: android.content.Context, t: TrackEntity) {
    val uri = t.mp3Uri
    if (uri.isNullOrBlank() || t.status != TrackStatus.Done) {
        Toast.makeText(ctx, "Not available yet", Toast.LENGTH_SHORT).show()
        return
    }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(uri), "audio/mpeg")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
    }
    val launched = runCatching { ctx.startActivity(intent) }.isSuccess
    if (!launched) {
        Toast.makeText(ctx, "No music player available", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun TrackPlayRow(t: TrackEntity, onPlay: () -> Unit) {
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
        IconButton(onClick = onPlay) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play")
        }
    }
}
