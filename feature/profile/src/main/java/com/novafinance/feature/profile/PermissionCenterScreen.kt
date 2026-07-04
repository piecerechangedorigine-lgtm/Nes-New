package com.novafinance.feature.profile

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novafinance.core.designsystem.Nova
import com.novafinance.core.designsystem.component.NovaCard
import com.novafinance.core.designsystem.icons.NovaIcons
import com.novafinance.core.domain.model.PermissionInfo
import com.novafinance.core.domain.model.PermissionStatus
import com.novafinance.core.domain.model.PermissionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionCenterRoute(
    onBack: () -> Unit,
    viewModel: PermissionCenterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Re-checks status whenever this screen resumes — the only reliable
    // moment a real OS grant could have changed (returning from either
    // the system permission dialog or the app's Settings page).
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentRefresh by rememberUpdatedState(viewModel::refresh)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) currentRefresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onOsPermissionResult(PermissionType.NOTIFICATIONS, granted) }

    PermissionCenterScreen(
        uiState = uiState,
        onBack = onBack,
        onPermissionClick = { type ->
            if (type == PermissionType.NOTIFICATIONS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onAcknowledge(type)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionCenterScreen(
    uiState: PermissionCenterUiState,
    onBack: () -> Unit,
    onPermissionClick: (PermissionType) -> Unit
) {
    Scaffold(
        containerColor = Nova.colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Permissions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = NovaIcons.Close, contentDescription = "Back", tint = Nova.colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Nova.colors.background,
                    titleContentColor = Nova.colors.textPrimary
                )
            )
        }
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(Nova.colors.background),
            contentPadding = PaddingValues(
                horizontal = Nova.spacing.screenHorizontal,
                vertical = scaffoldPadding.calculateTopPadding() + Nova.spacing.screenVertical
            ),
            verticalArrangement = Arrangement.spacedBy(Nova.spacing.md)
        ) {
            items(uiState.permissions, key = { it.type }) { info ->
                PermissionRow(info = info, onClick = { onPermissionClick(info.type) })
            }
        }
    }
}

@Composable
private fun PermissionRow(info: PermissionInfo, onClick: () -> Unit) {
    NovaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = info.type.displayName, style = Nova.typography.bodyLarge, color = Nova.colors.textPrimary)
                Text(text = info.type.description, style = Nova.typography.bodySmall, color = Nova.colors.textSecondary)
                Text(text = statusLabel(info.status), style = Nova.typography.labelSmall, color = statusColor(info.status))
            }
            if (info.status != PermissionStatus.GRANTED) {
                TextButton(onClick = onClick) {
                    Text(
                        text = if (info.type.isRealOsPermission) "Enable" else "Learn more",
                        color = Nova.colors.primary
                    )
                }
            }
        }
    }
}

private fun statusLabel(status: PermissionStatus): String = when (status) {
    PermissionStatus.GRANTED -> "Enabled"
    PermissionStatus.DENIED -> "Denied"
    PermissionStatus.NOT_REQUESTED -> "Not enabled"
    PermissionStatus.PERMANENTLY_DENIED -> "Denied — enable in system Settings"
}

@Composable
private fun statusColor(status: PermissionStatus) = when (status) {
    PermissionStatus.GRANTED -> Nova.colors.success
    PermissionStatus.DENIED, PermissionStatus.PERMANENTLY_DENIED -> Nova.colors.error
    PermissionStatus.NOT_REQUESTED -> Nova.colors.textTertiary
}
