package com.qrvault.scanner

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrvault.scanner.scanner.QrCodeAnalyzer
import com.qrvault.scanner.ui.ScannerViewModel
import com.qrvault.scanner.ui.theme.QrVaultTheme
import timber.log.Timber
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val viewModel: ScannerViewModel by viewModels {
        val app = application as QrVaultApplication
        ScannerViewModel.Factory(app.scanRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.i("MainActivity created")

        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }

            QrVaultTheme(darkTheme = isDarkTheme) {
                QrVaultRootScreen(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = {
                        isDarkTheme = !isDarkTheme
                        Timber.d("Theme switched dark=%s", isDarkTheme)
                    },
                    onSelectLanguage = { tag ->
                        Timber.i("LocaleManager switching app locale to '%s'", tag)
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrVaultRootScreen(
    viewModel: ScannerViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onSelectLanguage: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val history by viewModel.history.collectAsStateWithLifecycle()
    val activeScan by viewModel.activeScan.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (selectedTab) {
                            0 -> stringResource(R.string.nav_scanner)
                            1 -> stringResource(R.string.nav_history)
                            else -> stringResource(R.string.nav_settings)
                        }
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_scanner)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_history)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_settings)) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> CameraScannerTab(onQrDecoded = viewModel::onQrDecoded)
                1 -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = pluralStringResource(R.plurals.scan_count, history.size, history.size),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    items(history, key = { it.id }) { item ->
                        ElevatedCard(
                            onClick = { viewModel.selectScan(item) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(item.displayTitle, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(item.rawValue, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            }
                        }
                    }
                }
                else -> Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(onClick = { onSelectLanguage("uk") }) { Text("Українська") }
                        FilledTonalButton(onClick = { onSelectLanguage("en") }) { Text("English") }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.settings_theme))
                        Switch(checked = isDarkTheme, onCheckedChange = { onToggleTheme() })
                    }
                    OutlinedButton(onClick = viewModel::clearHistory) {
                        Text(stringResource(R.string.settings_clear_history))
                    }
                }
            }
        }

        activeScan?.let { scan ->
            ModalBottomSheet(onDismissRequest = { viewModel.selectScan(null) }) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(scan.displayTitle, style = MaterialTheme.typography.titleLarge)
                    Text(scan.rawValue, style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("QR", scan.rawValue))
                            Timber.d("Copied scan id=%d to clipboard", scan.id)
                        }) {
                            Text(stringResource(R.string.action_copy))
                        }
                        if (scan.payloadType == "URL") {
                            OutlinedButton(onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(scan.rawValue)))
                            }) {
                                Text(stringResource(R.string.action_open_url))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraScannerTab(onQrDecoded: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        Timber.i("Camera permission result granted=%s", granted)
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.camera_permission_rationale))
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text(stringResource(R.string.nav_scanner))
                }
            }
        }
        return
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraExecutor = Executors.newSingleThreadExecutor()
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val provider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, QrCodeAnalyzer(onQrDecoded = onQrDecoded))
                    }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analyzer)
                Timber.i("CameraX bound to lifecycle with QrCodeAnalyzer")
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}
