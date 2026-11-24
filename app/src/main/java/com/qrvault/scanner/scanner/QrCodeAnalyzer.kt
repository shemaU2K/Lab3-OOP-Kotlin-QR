package com.qrvault.scanner.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import timber.log.Timber

class QrCodeAnalyzer(
    private val debounceMs: Long = 2500L,
    private val onQrDecoded: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    private var lastDecodedValue: String? = null
    private var lastDecodedTimestamp: Long = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val rawValue = barcodes.firstOrNull()?.rawValue?.takeIf { it.isNotBlank() }
                if (rawValue != null) {
                    val now = System.currentTimeMillis()
                    val isDuplicate = rawValue == lastDecodedValue && (now - lastDecodedTimestamp) < debounceMs
                    if (!isDuplicate) {
                        lastDecodedValue = rawValue
                        lastDecodedTimestamp = now
                        Timber.i("MlKitDecoder decoded QR (%d chars)", rawValue.length)
                        onQrDecoded(rawValue)
                    }
                }
            }
            .addOnFailureListener { error ->
                Timber.e(error, "MlKitDecoder frame processing failed")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
