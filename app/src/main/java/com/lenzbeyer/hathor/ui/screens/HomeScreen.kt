package com.lenzbeyer.hathor.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lenzbeyer.hathor.data.PlaylistEntity
import com.lenzbeyer.hathor.ui.components.AngularTopBar
import com.lenzbeyer.hathor.ui.components.FaintDivider
import com.lenzbeyer.hathor.ui.components.PrimaryButton
import com.lenzbeyer.hathor.ui.components.SecondaryButton
import com.lenzbeyer.hathor.ui.components.SectionHeader
import com.lenzbeyer.hathor.ui.components.SolidDivider

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onContinue: () -> Unit,
    onLibraryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRecentClick: (String) -> Unit,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()

    LaunchedEffect(ui.draftReady) { if (ui.draftReady) onContinue() }

    Column(Modifier.fillMaxSize()) {
        AngularTopBar(title = "YT TO MP3")

        Column(Modifier.weight(1f)) {
            SectionHeader("PLAYLIST URL", Modifier.padding(top = 8.dp))

            OutlinedTextField(
                value = ui.url,
                onValueChange = vm::onUrlChange,
                placeholder = { Text("paste youtube playlist link") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
                singleLine = true,
                shape = RectangleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                isError = ui.error != null,
                supportingText = { ui.error?.let { Text(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(12.dp))

            PrimaryButton(
                text = if (ui.isWorking) "WORKING…" else "CONTINUE",
                onClick = vm::onContinue,
                enabled = !ui.isWorking && ui.url.isNotBlank(),
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(24.dp))
            SolidDivider()
            SectionHeader("RECENT")

            LazyColumn(Modifier.fillMaxWidth()) {
                items(recent, key = { it.id }) { p -> RecentRow(p, onRecentClick) }
            }
        }

        // Bottom action row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SecondaryButton(
                "≡  LIBRARY",
                onClick = onLibraryClick,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                "⚙  SETTINGS",
                onClick = onSettingsClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RecentRow(p: PlaylistEntity, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { onClick(p.id) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("•", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.size(12.dp))
        Text(
            "${p.folderArtist} — ${p.title}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    FaintDivider(Modifier.padding(horizontal = 16.dp))
}
