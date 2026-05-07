package com.lenzbeyer.hathor.ui.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lenzbeyer.hathor.domain.TrackDraft
import com.lenzbeyer.hathor.ui.components.AngularTopBar
import com.lenzbeyer.hathor.ui.components.FaintDivider
import com.lenzbeyer.hathor.ui.components.PrimaryButton
import com.lenzbeyer.hathor.ui.components.SectionHeader
import com.lenzbeyer.hathor.ui.components.SolidDivider
import com.lenzbeyer.hathor.ui.components.SquareCheckbox

@Composable
fun TagPreviewScreen(
    vm: TagPreviewViewModel,
    onBack: () -> Unit,
    onStartDownload: (String) -> Unit,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val draft = ui.draft ?: run {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text("No playlist loaded.", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        AngularTopBar(
            title = "TAG PREVIEW",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                Text(
                    "${draft.tracks.count { !it.isSkipped }} TRACKS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 16.dp),
                )
            },
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            item { SectionHeader("PLAYLIST") }
            item { LabeledField("FOLDER ARTIST", draft.folderArtist, vm::setFolderArtist) }
            item { LabeledField("TITLE",          draft.title,         vm::setTitle) }
            item { LabeledField("ALBUM",          draft.album,         vm::setAlbum) }
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LabeledField("YEAR",  draft.year ?: "",  vm::setYear,  Modifier.weight(1f))
                    LabeledField("GENRE", draft.genre ?: "", vm::setGenre, Modifier.weight(1f))
                }
            }
            item { Spacer(Modifier.height(8.dp)); SolidDivider() }
            item { SectionHeader("TRACKS") }

            itemsIndexed(draft.tracks, key = { _, t -> t.videoId }) { idx, t ->
                TrackEditRow(
                    track = t,
                    onArtistChange = { vm.setTrackArtist(idx, it) },
                    onTitleChange  = { vm.setTrackTitle(idx, it) },
                    onSkipToggle   = { vm.toggleSkip(idx) },
                )
                FaintDivider(Modifier.padding(horizontal = 16.dp))
            }
        }

        PrimaryButton(
            text = "START DOWNLOAD",
            onClick = { onStartDownload(draft.youtubePlaylistId) },
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        singleLine = true,
        shape = RectangleShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.outline,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun TrackEditRow(
    track: TrackDraft,
    onArtistChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onSkipToggle: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SquareCheckbox(
            checked = track.isSkipped,
            onCheckedChange = { onSkipToggle() },
        )
        Spacer(Modifier.size(12.dp))
        Text(
            "%02d".format(track.originalIndex),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(8.dp))
        Box(
            Modifier
                .weight(1f)
                .clickable { /* TODO: open per-track edit sheet */ },
        ) {
            Column {
                Text(
                    track.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
