package com.simpass.smssyncer.data

import android.content.Context
import org.json.JSONObject

/** All receivers and the activity run in one process. The lock protects read/modify/write operations. */
class ForwardingStore(context: Context) {
    val preferences = context.applicationContext.getSharedPreferences("sms_syncer", Context.MODE_PRIVATE)

    init {
        synchronized(lock) {
            if (preferences.contains(LEGACY_HISTORY)) {
                check(preferences.edit().remove(LEGACY_HISTORY).commit()) { "Could not remove old send history." }
            }
        }
    }

    fun config(): ForwardingConfig = synchronized(lock) {
        val raw = preferences.getString(CONFIG, "{}") ?: "{}"
        val config = ConfigCodec.decode(raw)
        if (ConfigCodec.isLegacy(raw)) writeConfig(config)
        config
    }

    fun saveProvider(provider: Provider): NumberValidationError? = synchronized(lock) {
        val config = config()
        NumberRules.providerError(config, provider)?.let { return@synchronized it }
        val normalized = provider.copy(name = provider.name.trim(), number = NumberRules.domesticNumber(provider.canonical),
            receivers = provider.receivers.map { it.copy(number = NumberRules.domesticNumber(it.canonical), label = it.label.trim()) })
        val providers = if (config.providers.any { it.id == provider.id }) {
            config.providers.map { if (it.id == provider.id) normalized else it }
        } else config.providers + normalized
        val next = config.copy(providers = providers)
        writeConfig(next.copy(enabled = next.enabled && next.isReady))
        null
    }

    fun removeProvider(id: String) = synchronized(lock) {
        val config = config()
        val next = config.copy(providers = config.providers.filterNot { it.id == id })
        writeConfig(next.copy(enabled = next.enabled && next.isReady))
    }

    fun setEnabled(enabled: Boolean) = synchronized(lock) {
        val config = config()
        writeConfig(config.copy(enabled = enabled && config.isReady))
    }

    // Persist before any SMS is submitted. Never retry an ambiguous send automatically.
    fun claimMessage(fingerprint: String, now: Long): Boolean = synchronized(lock) {
        val seen = JSONObject(preferences.getString(SEEN, "{}") ?: "{}")
        if (seen.has(fingerprint)) return@synchronized false
        seen.keys().asSequence().toList().forEach { key ->
            if (now - seen.getLong(key) > 24 * 60 * 60 * 1000L) seen.remove(key)
        }
        if (seen.length() >= 512) {
            seen.keys().asSequence().minByOrNull { seen.getLong(it) }?.let(seen::remove)
        }
        seen.put(fingerprint, now)
        check(preferences.edit().putString(SEEN, seen.toString()).commit()) { "Could not save message receipt." }
        true
    }

    private fun writeConfig(config: ForwardingConfig) {
        check(preferences.edit().putString(CONFIG, ConfigCodec.encode(config)).commit()) { "Could not save numbers." }
    }

    companion object {
        private val lock = Any()
        const val CONFIG = "config"
        private const val LEGACY_HISTORY = "history"
        private const val SEEN = "seen"
    }
}
