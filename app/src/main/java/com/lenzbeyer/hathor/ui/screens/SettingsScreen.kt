package com.lenzbeyer.hathor.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lenzbeyer.hathor.ui.components.AngularTopBar
import com.lenzbeyer.hathor.ui.components.PrimaryButton
import com.lenzbeyer.hathor.ui.components.SectionHeader
import com.lenzbeyer.hathor.ui.components.SolidDivider
import com.lenzbeyer.hathor.ui.components.SquareSwitch

@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit,
) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        ctx.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        vm.setOutputFolder(uri.toString())
    }

    Column(Modifier.fillMaxSize()) {
        AngularTopBar(
            title = "SETTINGS",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader("STORAGE")
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Output folder", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        s.outputFolderUri ?: "Not set — pick one to begin",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                com.lenzbeyer.hathor.ui.components.SecondaryButton(
                    text = "PICK",
                    onClick = { folderPicker.launch(null) },
                )
            }
            SolidDivider()

            SectionHeader("DOWNLOAD")
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Bitrate", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    "320 kbps CBR",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Parallel downloads", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Currently ${s.maxParallel} of max 3",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(1, 2, 3).forEach { n ->
                        com.lenzbeyer.hathor.ui.components.SecondaryButton(
                            text = n.toString(),
                            onClick = { vm.setMaxParallel(n) },
                        )
                    }
                }
            }
            SolidDivider()

            SectionHeader("TAGGING")
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Strip cosmetic suffixes", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "e.g. (Official Video), [HD], [Remastered]",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SquareSwitch(checked = s.stripCosmetic, onCheckedChange = vm::setStripCosmetic)
            }
            SolidDivider()

            SectionHeader("MAINTENANCE")
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("yt-dlp version", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    s.ytDlpVersion ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PrimaryButton(
                text = "CHECK FOR UPDATE",
                onClick = vm::updateYtDlp,
                modifier = Modifier.padding(16.dp),
            )

            Spacer(Modifier.size(24.dp))
            Text(
                "v0.1.0  ·  sideload build",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
