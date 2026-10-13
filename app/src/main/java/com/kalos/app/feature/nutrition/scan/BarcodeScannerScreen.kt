package com.kalos.app.feature.nutrition.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.kalos.app.R
import com.kalos.app.core.ui.component.KalosButton
import com.kalos.app.core.ui.component.ScreenHeader
import com.kalos.app.core.ui.theme.KalosShape
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Result key written to the food-search screen's savedStateHandle when a barcode is
 * decoded. The screen observes it and decides what to do (local lookup / manual create).
 */
const val SCANNED_BARCODE_KEY = "scanned_barcode"

@Composable
fun BarcodeScannerScreen(navController: NavController) {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Fires exactly once: hands the decoded value back to the previous screen and pops.
    val handled = remember { AtomicBoolean(false) }
    val onDecoded: (String) -> Unit = remember {
        { raw ->
            if (handled.compareAndSet(false, true)) {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set(SCANNED_BARCODE_KEY, raw)
                navController.popBackStack()
            }
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader(
                title = stringResource(R.string.food_scan_action),
                small = true,
                modifier = Modifier.padding(horizontal = 16.dp),
                navigation = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (hasPermission) {
                    CameraPreview(onDecoded = onDecoded, modifier = Modifier.fillMaxSize())
                    Viewfinder(Modifier.fillMaxSize())
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shape = KalosShape.control,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(24.dp),
                    ) {
                        Text(
                            stringResource(R.string.food_scan_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp),
                    ) {
                        Text(
                            stringResource(R.string.food_scan_permission),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        KalosButton(
                            stringResource(R.string.food_scan_allow),
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Viseur gradué, statique (rien ne bouge pendant l'analyse) : voile autour de la fenêtre,
 * équerres aux coins, graduations sur les bords haut et bas comme une règle. Purement décoratif.
 */
@Composable
private fun Viewfinder(modifier: Modifier = Modifier) {
    // Sur l'image caméra, blanc et voile noir restent lisibles quel que soit le thème.
    val ink = Color.White
    val faint = ink.copy(alpha = 0.7f)
    val veil = Color.Black.copy(alpha = 0.45f)
    Canvas(modifier) {
        val w = size.width * 0.8f
        val h = (w * 0.45f).coerceAtMost(size.height * 0.5f)
        val left = (size.width - w) / 2f
        val top = (size.height - h) / 2f
        val right = left + w
        val bottom = top + h
        // Voile : quatre bandes autour de la fenêtre.
        drawRect(veil, Offset.Zero, Size(size.width, top))
        drawRect(veil, Offset(0f, bottom), Size(size.width, size.height - bottom))
        drawRect(veil, Offset(0f, top), Size(left, h))
        drawRect(veil, Offset(right, top), Size(size.width - right, h))

        // Équerres aux quatre coins.
        val stroke = 2.dp.toPx()
        val arm = 22.dp.toPx()
        for ((x, dx) in listOf(left to arm, right to -arm)) {
            for ((y, dy) in listOf(top to arm, bottom to -arm)) {
                drawLine(ink, Offset(x, y), Offset(x + dx, y), stroke)
                drawLine(ink, Offset(x, y), Offset(x, y + dy), stroke)
            }
        }

        // Graduations entre les équerres, longues tous les 5 pas.
        val tick = 1.dp.toPx()
        val step = 6.dp.toPx()
        val start = left + arm + step
        val count = ((w - 2 * (arm + step)) / step).toInt()
        val long = 9.dp.toPx()
        val short = 5.dp.toPx()
        for (i in 0..count) {
            val x = start + i * step
            val len = if (i % 5 == 0) long else short
            drawLine(faint, Offset(x, top), Offset(x, top + len), tick)
            drawLine(faint, Offset(x, bottom), Offset(x, bottom - len), tick)
        }
    }
}

@Composable
private fun CameraPreview(
    onDecoded: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val executor = ContextCompat.getMainExecutor(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(executor, BarcodeAnalyzer(onDecoded)) }
                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                } catch (_: Exception) {
                    // Camera bind can fail (no back camera, in use elsewhere) — the hint
                    // stays and the user can go back. Nothing actionable to surface here.
                }
            }, executor)
            previewView
        },
    )
}

/** ML Kit barcode analyzer restricted to retail formats. Fires [onBarcode] once. */
private class BarcodeAnalyzer(private val onBarcode: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
            )
            .build()
    )
    private var handled = false

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (handled) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val raw = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                if (raw != null && !handled) {
                    handled = true
                    onBarcode(raw)
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}
