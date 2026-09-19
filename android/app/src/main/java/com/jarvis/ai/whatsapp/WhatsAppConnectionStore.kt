package com.jarvis.ai.whatsapp

import android.content.Context

class WhatsAppConnectionStore(context: Context) {
    private val prefs = context.getSharedPreferences("jarvis_whatsapp", Context.MODE_PRIVATE)

    fun savePhone(phone: String) {
        prefs.edit().putString("phone", phone.filter(Char::isDigit)).apply()
    }

    fun phone(): String? = prefs.getString("phone", null)

    fun clear() {
        prefs.edit().clear().apply()
    }
}
