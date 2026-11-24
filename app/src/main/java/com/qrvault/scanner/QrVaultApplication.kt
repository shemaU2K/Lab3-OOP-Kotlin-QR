package com.qrvault.scanner

import android.app.Application
import com.qrvault.scanner.data.local.AppDatabase
import com.qrvault.scanner.data.repository.ScanRepository
import com.qrvault.scanner.logging.QrDebugTree
import com.qrvault.scanner.logging.QrReleaseTree
import timber.log.Timber

class QrVaultApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var scanRepository: ScanRepository
        private set

    override fun onCreate() {
        super.onCreate()
        configureLogging()

        Timber.i("Initializing QRVault local Room database")
        database = AppDatabase.getInstance(this)
        scanRepository = ScanRepository(database.scanHistoryDao())
        Timber.d("QRVaultApplication ready (buildType=%s)", if (BuildConfig.DEBUG) "debug" else "release")
    }

    private fun configureLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(QrDebugTree())
        } else {
            Timber.plant(QrReleaseTree())
        }
    }
}
