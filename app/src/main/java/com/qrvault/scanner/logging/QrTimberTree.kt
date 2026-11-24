package com.qrvault.scanner.logging

import android.util.Log
import timber.log.Timber

/**
 * Development logger tree adding file name, line number, and method tag
 * for fast Logcat navigation in Android Studio.
 */
class QrDebugTree : Timber.DebugTree() {
    override fun createStackElementTag(element: StackTraceElement): String {
        return "QRVault:${element.fileName}:${element.lineNumber}#${element.methodName}"
    }
}

/**
 * Production logger tree:
 * - Suppresses VERBOSE and DEBUG noise
 * - Automatically redacts Wi-Fi passwords (P:...;) and secrets before logging
 */
class QrReleaseTree : Timber.Tree() {
    private val wifiPasswordRegex = Regex("(P:)([^;]*)(;)", RegexOption.IGNORE_CASE)

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= Log.INFO
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (!isLoggable(tag, priority)) return

        val sanitized = wifiPasswordRegex.replace(message, "$1[REDACTED]$3")
        val resolvedTag = tag ?: "QRVault"

        when (priority) {
            Log.INFO -> Log.i(resolvedTag, sanitized)
            Log.WARN -> Log.w(resolvedTag, sanitized, t)
            Log.ERROR, Log.ASSERT -> Log.e(resolvedTag, sanitized, t)
        }
    }
}
