package com.jarvis.ai.screencontrol

import kotlinx.coroutines.delay

data class ScreenVerification(
    val before: ScreenSnapshot?,
    val after: ScreenSnapshot?,
    val changed: Boolean
)

class ScreenTaskExecutor(
    private val snapshotProvider: ScreenSnapshotProvider = ScreenSnapshotProvider()
) {
    @Volatile
    private var cancelled = false

    fun cancel() {
        cancelled = true
    }

    fun reset() {
        cancelled = false
    }

    suspend fun waitForUiChange(delayMs: Long = 450L) {
        if (cancelled) return
        delay(delayMs)
    }

    suspend fun verifyChange(
        before: ScreenSnapshot?,
        timeoutMs: Long = 3_000L,
        pollMs: Long = 300L
    ): ScreenVerification {
        if (cancelled) return ScreenVerification(before, before, false)

        val deadline = System.currentTimeMillis() + timeoutMs
        var latest = snapshotProvider.capture()

        while (!cancelled && System.currentTimeMillis() < deadline) {
            if (fingerprint(before) != fingerprint(latest)) {
                return ScreenVerification(before, latest, true)
            }
            delay(pollMs)
            latest = snapshotProvider.capture()
        }

        return ScreenVerification(before, latest, fingerprint(before) != fingerprint(latest))
    }

    fun isCancelled(): Boolean = cancelled

    fun observe(): ScreenSnapshot? = snapshotProvider.capture()

    private fun fingerprint(snapshot: ScreenSnapshot?): String {
        if (snapshot == null) return "null"
        return buildString {
            append(snapshot.packageName).append('|')
            snapshot.elements.take(120).forEach {
                append(it.text.orEmpty()).append('|')
                append(it.contentDescription.orEmpty()).append('|')
                append(it.className.orEmpty()).append('|')
                append(it.clickable).append('|')
                append(it.enabled).append(';')
            }
        }
    }
}
