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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@OptIn(ExperimentalMaterial3Api::class)
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

    var sheetIndex by remember { mutableStateOf<Int?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                    onSkipToggle   = { vm.toggleInclude(idx) },
                    onRowClick     = { sheetIndex = idx },
                )
                FaintDivider(Modifier.padding(horizontal = 16.dp))
            }
        }

        ui.error?.let { msg ->
            Text(
                msg,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        PrimaryButton(
            text = "START DOWNLOAD",
            onClick = { vm.startDownload(onStartDownload) },
            modifier = Modifier.padding(16.dp),
        )
    }

    sheetIndex?.let { idx ->
        val track = draft.tracks.getOrNull(idx) ?: run {
            sheetIndex = null
            return@let
        }
        ModalBottomSheet(
            onDismissRequest = { sheetIndex = null },
            sheetState = sheetState,
            shape = RectangleShape,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Text(
                    "TRACK %02d".format(track.originalIndex),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                SolidDivider()
                Spacer(Modifier.height(8.dp))
                LabeledField(
                    label = "ARTIST",
                    value = track.artist,
                    onChange = { vm.setTrackArtist(idx, it) },
                )
                LabeledField(
                    label = "TITLE",
                    value = track.title,
                    onChange = { vm.setTrackTitle(idx, it) },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
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
    onSkipToggle: () -> Unit,
    onRowClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SquareCheckbox(
            checked = !track.isSkipped,
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
                .clickable(onClick = onRowClick),
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
