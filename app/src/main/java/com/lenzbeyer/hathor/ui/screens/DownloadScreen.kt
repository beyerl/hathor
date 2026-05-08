package com.lenzbeyer.hathor.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lenzbeyer.hathor.data.TrackEntity
import com.lenzbeyer.hathor.domain.TrackStatus
import com.lenzbeyer.hathor.ui.components.AngularTopBar
import com.lenzbeyer.hathor.ui.components.FaintDivider
import com.lenzbeyer.hathor.ui.components.SecondaryButton
import com.lenzbeyer.hathor.ui.components.SolidDivider

@Composable
fun DownloadScreen(
    vm: DownloadViewModel,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val tracks by vm.tracks.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        AngularTopBar(
            title = "DOWNLOADING",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                Text(
                    "${state.done} / ${state.total}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 16.dp),
                )
            },
        )

        Column(Modifier.padding(16.dp)) {
            Text(
                "Job",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                "${state.percent}%  ·  ${state.done} of ${state.total} done",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(12.dp))
            LinearProgressIndicator(
                progress = { state.percent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SecondaryButton(
                text = if (state.isPaused) "RESUME" else "PAUSE",
                onClick = { if (state.isPaused) vm.resume() else vm.pause() },
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = "CANCEL",
                onClick = vm::cancel,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.size(16.dp))
        SolidDivider()

        LazyColumn(Modifier.fillMaxSize()) {
            items(tracks, key = { it.id }) { t ->
                TrackProgressRow(t, onRetry = { vm.retry(t.id) })
                FaintDivider(Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun TrackProgressRow(t: TrackEntity, onRetry: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            statusGlyph(t.status),
            style = MaterialTheme.typography.bodyLarge,
            color = if (t.status == TrackStatus.Failed) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(12.dp))
        Text(
            "%02d".format(t.index),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(12.dp))
        Text(
            t.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            t.status.name.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (t.status == TrackStatus.Failed) {
            Spacer(Modifier.size(8.dp))
            IconButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = "Retry")
            }
        }
    }
}

private fun statusGlyph(s: TrackStatus): String = when (s) {
    TrackStatus.Done        -> "✓"
    TrackStatus.Failed      -> "✗"
    TrackStatus.Skipped     -> "⊘"
    TrackStatus.Pending     -> "·"
    TrackStatus.Resolving,
    TrackStatus.Downloading,
    TrackStatus.Transcoding,
    TrackStatus.Tagging     -> "⟳"
}
