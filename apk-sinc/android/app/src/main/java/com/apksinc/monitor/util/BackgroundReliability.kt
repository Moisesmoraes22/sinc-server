package com.apksinc.monitor.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/**
 * Ajuda a contornar o gerenciamento agressivo de segundo plano que faz push
 * (FCM) parar de chegar quando o app nao esta aberto - comum em qualquer
 * Android via "otimizacao de bateria", e ainda mais restritivo em telas
 * MIUI (Xiaomi), que tem uma permissao extra de "Inicializacao automatica"
 * sem API publica - so pode ser ligada pela propria pessoa, nunca pelo app.
 *
 * Isso NAO e um jeito de burlar essas protecoes: e o fluxo padrao e legitimo
 * que apps que dependem de notificacao confiavel (WhatsApp, Gmail, etc.) já
 * usam - pedir a isencao por uma tela do sistema (nunca automatico) e, no
 * caso da MIUI, levar a pessoa direto ate a tela certa em vez de ela ter que
 * procurar no meio dos menus.
 */
object BackgroundReliability {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Abre a tela do sistema que pede pra pessoa isentar o app da otimizacao
     * de bateria. Precisa ser confirmado manualmente - nenhum app consegue
     * se auto-isentar sem essa tela. */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (isIgnoringBatteryOptimizations(context)) return
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        runCatching { context.startActivity(intent) }
    }

    fun isMiui(): Boolean =
        !getSystemProperty("ro.miui.ui.version.name").isNullOrBlank()

    /** Tenta abrir a tela de "Inicializacao automatica" da MIUI. Cada versao
     * da MIUI usa um nome de tela ligeiramente diferente, entao tentamos
     * varias em ordem ate uma funcionar; se nenhuma existir (aparelho nao e
     * Xiaomi, ou a tela mudou de novo numa versao futura), caimos de volta
     * pras configuracoes gerais do app, onde a pessoa ainda consegue chegar
     * nas opcoes de bateria/notificacao manualmente. */
    fun openAutostartSettings(context: Context) {
        val candidates = listOf(
            "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
            "com.miui.securitycenter" to "com.miui.securitycenter.permission.AppPermissionsEditorActivity",
            "com.miui.powerkeeper" to "com.miui.powerkeeper.ui.HiddenAppsConfigActivity",
        )
        for ((pkg, cls) in candidates) {
            val intent = Intent().apply {
                component = android.content.ComponentName(pkg, cls)
            }
            if (runCatching { context.startActivity(intent); true }.getOrDefault(false)) return
        }
        openAppSettings(context)
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        runCatching { context.startActivity(intent) }
    }

    private fun getSystemProperty(name: String): String? = runCatching {
        val clazz = Class.forName("android.os.SystemProperties")
        val method = clazz.getMethod("get", String::class.java)
        method.invoke(null, name) as? String
    }.getOrNull()
}
