package com.novafinance.feature.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novafinance.core.designsystem.Nova
import com.novafinance.core.designsystem.component.NovaCard
import com.novafinance.core.designsystem.component.NovaChipOption
import com.novafinance.core.designsystem.component.NovaChipRow
import com.novafinance.core.designsystem.component.NovaSectionHeader
import com.novafinance.core.domain.model.SoundMode
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun ProfileRoute(
    onOpenPermissions: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.backupEvents.collect { event ->
            statusMessage = when (event) {
                BackupEvent.ImportSucceeded -> "Backup restored."
                is BackupEvent.Failure -> event.message
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.exportBackup { json ->
            context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            statusMessage = "Backup saved."
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val json = context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream)).readText()
        }
        if (json != null) viewModel.importBackup(json)
    }

    ProfileScreen(
        uiState = uiState,
        statusMessage = statusMessage,
        onBiometricLockToggle = viewModel::onBiometricLockToggle,
        onNotificationsToggle = viewModel::onNotificationsToggle,
        onSpendingAlertsToggle = viewModel::onSpendingAlertsToggle,
        onSoundModeSelect = viewModel::onSoundModeSelect,
        onDashboardInsightsToggle = viewModel::onDashboardInsightsToggle,
        onOpenPermissions = onOpenPermissions,
        onExportClick = { exportLauncher.launch("nova-backup.json") },
        onImportClick = { importLauncher.launch(arrayOf("application/json")) }
    )
}

@Composable
private fun ProfileScreen(
    uiState: ProfileUiState,
    statusMessage: String?,
    onBiometricLockToggle: (Boolean) -> Unit,
    onNotificationsToggle: (Boolean) -> Unit,
    onSpendingAlertsToggle: (Boolean) -> Unit,
    onSoundModeSelect: (SoundMode) -> Unit,
    onDashboardInsightsToggle: (Boolean) -> Unit,
    onOpenPermissions: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    val settings = uiState.settings

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Nova.colors.background),
        contentPadding = PaddingValues(
            horizontal = Nova.spacing.screenHorizontal,
            vertical = Nova.spacing.screenVertical
        ),
        verticalArrangement = Arrangement.spacedBy(Nova.spacing.xl)
    ) {
        item {
            Text(text = "Profile", style = Nova.typography.headlineLarge, color = Nova.colors.textPrimary)
        }

        if (statusMessage != null) {
            item {
                Text(text = statusMessage, style = Nova.typography.bodySmall, color = Nova.colors.primary)
            }
        }

        item {
            SettingsSection(title = "Security") {
                SettingsToggleRow(
                    label = "Biometric lock",
                    description = "Require Face/Fingerprint unlock to open Nova",
                    checked = settings.isBiometricLockEnabled,
                    onCheckedChange = onBiometricLockToggle
                )
                SettingsNavRow(
                    label = "Permissions",
                    description = "Manage what Nova can access",
                    onClick = onOpenPermissions
                )
            }
        }

        item {
            SettingsSection(title = "Notifications") {
                SettingsToggleRow(
                    label = "Push notifications",
                    description = "Account activity and reminders",
                    checked = settings.areNotificationsEnabled,
                    onCheckedChange = onNotificationsToggle
                )
                SettingsToggleRow(
                    label = "Spending alerts",
                    description = "Notify me when I'm close to a budget limit",
                    checked = settings.areSpendingAlertsEnabled,
                    onCheckedChange = onSpendingAlertsToggle
                )
            }
        }

        item {
            SettingsSection(title = "Sound & haptics") {
                Text(text = "Mode", style = Nova.typography.labelLarge, color = Nova.colors.textSecondary)
                NovaChipRow(
                    options = SoundMode.entries.map { NovaChipOption(it, it.name.lowercase().replaceFirstChar(Char::uppercase)) },
                    selected = settings.soundMode,
                    onSelect = onSoundModeSelect
                )
            }
        }

        item {
            SettingsSection(title = "Personalization") {
                Text(
                    text = "Nova is dark-first — a light theme isn't available yet.",
                    style = Nova.typography.bodyMedium,
                    color = Nova.colors.textSecondary
                )
                SettingsToggleRow(
                    label = "Dashboard insights",
                    description = "Show the spending insight banner on your dashboard",
                    checked = settings.showDashboardInsights,
                    onCheckedChange = onDashboardInsightsToggle
                )
            }
        }

        item {
            SettingsSection(title = "Your data") {
                SettingsNavRow(
                    label = "Export backup",
                    description = "Save everything as a JSON file you control",
                    onClick = onExportClick
                )
                SettingsNavRow(
                    label = "Restore from backup",
                    description = "Replaces all current data with the backup file's contents",
                    onClick = onImportClick
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Nova.spacing.sm)) {
        NovaSectionHeader(title = title)
        NovaCard(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = Nova.typography.bodyLarge, color = Nova.colors.textPrimary)
            Text(text = description, style = Nova.typography.bodySmall, color = Nova.colors.textSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Nova.colors.onPrimary,
                checkedTrackColor = Nova.colors.primary,
                uncheckedThumbColor = Nova.colors.textSecondary,
                uncheckedTrackColor = Nova.colors.surface
            )
        )
    }
}

@Composable
private fun SettingsNavRow(label: String, description: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = Nova.typography.bodyLarge, color = Nova.colors.textPrimary)
            Text(text = description, style = Nova.typography.bodySmall, color = Nova.colors.textSecondary)
        }
        Text(text = "›", style = Nova.typography.titleLarge, color = Nova.colors.textTertiary)
    }
}
