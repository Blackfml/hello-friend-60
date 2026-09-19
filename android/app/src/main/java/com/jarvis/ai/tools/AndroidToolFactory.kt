package com.jarvis.ai.tools

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.BatteryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AndroidToolFactory {
    fun registerDefaults(context: Context, registry: ToolRegistry) {
        registry.register(SimpleTool(ToolDefinition("get_time", "Obtém a hora e data atuais.")) {
            val text = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "BR")).format(Date())
            ToolResult.Success("Agora são $text.")
        })

        registry.register(SimpleTool(ToolDefinition("get_battery_status", "Consulta bateria e carregamento.")) {
            val manager = context.getSystemService(BatteryManager::class.java)
            val level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val charging = manager.isCharging
            ToolResult.Success(
                "Bateria em $level%. ${if (charging) "O aparelho está carregando." else "O aparelho não está carregando."}",
                mapOf("level" to level, "charging" to charging)
            )
        })

        registry.register(SimpleTool(ToolDefinition(
            "set_volume",
            "Define o volume de uma categoria de áudio.",
            parameters = mapOf("stream" to "media|ring|alarm|notification", "level" to "inteiro de 0 a 100"),
            requiredParameters = setOf("stream", "level")
        )) { args ->
            val stream = when (args["stream"]?.toString()?.lowercase()) {
                "ring" -> AudioManager.STREAM_RING
                "alarm" -> AudioManager.STREAM_ALARM
                "notification" -> AudioManager.STREAM_NOTIFICATION
                else -> AudioManager.STREAM_MUSIC
            }
            val level = args["level"]?.toString()?.toIntOrNull()?.coerceIn(0, 100)
                ?: return@SimpleTool ToolResult.Failure("O volume precisa ser um número de 0 a 100.")
            val audio = context.getSystemService(AudioManager::class.java)
            val max = audio.getStreamMaxVolume(stream)
            audio.setStreamVolume(stream, (max * level / 100.0).toInt(), 0)
            ToolResult.Success("Volume ajustado para $level%.")
        })

        registry.register(SimpleTool(ToolDefinition(
            "set_flashlight",
            "Liga ou desliga a lanterna.",
            parameters = mapOf("enabled" to "true ou false"),
            requiredParameters = setOf("enabled")
        )) { args ->
            val enabled = args["enabled"]?.toString()?.toBooleanStrictOrNull()
                ?: return@SimpleTool ToolResult.Failure("enabled deve ser true ou false.")
            val camera = context.getSystemService(android.hardware.camera2.CameraManager::class.java)
            val id = camera.cameraIdList.firstOrNull { id ->
                runCatching {
                    camera.getCameraCharacteristics(id).get(
                        android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE
                    ) == true
                }.getOrDefault(false)
            } ?: return@SimpleTool ToolResult.Failure("Não encontrei um flash disponível.")
            camera.setTorchMode(id, enabled)
            ToolResult.Success(if (enabled) "Lanterna ligada." else "Lanterna desligada.")
        })

        registry.register(SimpleTool(ToolDefinition(
            "open_app",
            "Abre um aplicativo instalado pelo nome.",
            parameters = mapOf("app_name" to "nome do aplicativo"),
            requiredParameters = setOf("app_name")
        )) { args ->
            val requested = args["app_name"]?.toString()?.trim().orEmpty()
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val apps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            val match = apps.firstOrNull {
                it.loadLabel(pm).toString().equals(requested, ignoreCase = true)
            } ?: apps.firstOrNull {
                it.loadLabel(pm).toString().contains(requested, ignoreCase = true)
            } ?: return@SimpleTool ToolResult.Failure("Não encontrei o aplicativo \"$requested\".")
            val launch = pm.getLaunchIntentForPackage(match.activityInfo.packageName)
                ?: return@SimpleTool ToolResult.Failure("Não consegui iniciar o aplicativo.")
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launch)
            ToolResult.Success("Aplicativo ${match.loadLabel(pm)} aberto.")
        })

        registry.register(SimpleTool(ToolDefinition(
            "create_alarm",
            "Abre a interface do sistema para criar um alarme.",
            parameters = mapOf("hour" to "0-23", "minute" to "0-59"),
            requiredParameters = setOf("hour", "minute"),
            riskLevel = RiskLevel.MEDIUM
        )) { args ->
            val hour = args["hour"]?.toString()?.toIntOrNull()
                ?: return@SimpleTool ToolResult.Failure("Hora inválida.")
            val minute = args["minute"]?.toString()?.toIntOrNull()
                ?: return@SimpleTool ToolResult.Failure("Minuto inválido.")
            if (hour !in 0..23 || minute !in 0..59) return@SimpleTool ToolResult.Failure("Horário inválido.")
            val intent = Intent(AlarmManager.ACTION_SET_ALARM).apply {
                putExtra(AlarmManager.EXTRA_HOUR, hour)
                putExtra(AlarmManager.EXTRA_MINUTES, minute)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolResult.Success("Abri a tela do sistema para criar o alarme de $hour:${"%02d".format(minute)}.")
        })
    }

    private class SimpleTool(
        override val definition: ToolDefinition,
        private val action: suspend (Map<String, Any?>) -> ToolResult
    ) : JarvisTool {
        override suspend fun execute(arguments: Map<String, Any?>): ToolResult =
            withContext(Dispatchers.Main) { action(arguments) }
    }
}
