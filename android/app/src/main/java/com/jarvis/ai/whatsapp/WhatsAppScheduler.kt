package com.jarvis.ai.whatsapp

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class WhatsAppScheduler(private val context: Context) {
    fun schedule(request: WhatsAppSendRequest, delayMinutes: Long): UUID {
        val data = Data.Builder()
            .putString("contact_id", request.contactId)
            .putString("message", request.message)
            .build()
        val work = OneTimeWorkRequestBuilder<WhatsAppSendWorker>()
            .setInputData(data)
            .setInitialDelay(delayMinutes.coerceAtLeast(1), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueue(work)
        return work.id
    }
}

class WhatsAppSendWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return Result.failure(
            workDataOf("error" to "Ponte local do WhatsApp não configurada; nenhuma mensagem foi enviada.")
        )
    }
}
