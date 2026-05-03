package ai.tour.guide.ui.components.permissions

import ai.tour.guide.R
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private val LocationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

@Composable
fun LocationPermissionGate(
    modifier: Modifier = Modifier,
    requestOnFirstComposition: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasLocationPermission by remember {
        mutableStateOf(context.hasLocationPermission())
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasLocationPermission = context.hasLocationPermission()
    }

    LaunchedEffect(requestOnFirstComposition) {
        hasLocationPermission = context.hasLocationPermission()
        if (requestOnFirstComposition && !hasLocationPermission) {
            locationPermissionLauncher.launch(LocationPermissions)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasLocationPermission = context.hasLocationPermission()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (hasLocationPermission) {
            content()
        } else {
            LocationPermissionRequiredModal(
                onRequestPermission = {
                    locationPermissionLauncher.launch(LocationPermissions)
                },
                onOpenSettings = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
private fun LocationPermissionRequiredModal(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.location_permission_required_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 24.dp)
                )
                Text(
                    text = stringResource(R.string.location_permission_required_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 24.dp)
                        .fillMaxWidth()
                )
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.location_permission_grant_button))
                }
                FilledTonalButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.location_permission_settings_button))
                }
            }
        }
    }
}

private fun Context.hasLocationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}
