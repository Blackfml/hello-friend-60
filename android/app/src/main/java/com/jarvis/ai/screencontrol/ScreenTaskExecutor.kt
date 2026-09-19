package com.jarvis.ai.screencontrol

import kotlinx.coroutines.delay

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

    suspend fun waitForUiChange(delayMs: Long = 350L) {
        if (cancelled) return
        delay(delayMs)
    }

    fun isCancelled(): Boolean = cancelled

    fun observe(): ScreenSnapshot? = snapshotProvider.capture()
}
