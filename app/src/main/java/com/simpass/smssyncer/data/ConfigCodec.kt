package com.simpass.smssyncer.data

import org.json.JSONArray
import org.json.JSONObject

object ConfigCodec {
    fun isLegacy(raw: String): Boolean {
        val json = JSONObject(raw)
        return !json.has("providers") && (json.has("watched") || json.has("destinations"))
    }

    fun decode(raw: String): ForwardingConfig {
        val json = JSONObject(raw)
        val array = json.optJSONArray("providers")
        val providers = if (array != null) (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val canonical = item.getString("canonical")
            Provider(item.getString("id"), item.getString("name"), NumberRules.domesticNumber(canonical),
                canonical, entries(item.optJSONArray("receivers")))
        } else {
            val receivers = entries(json.optJSONArray("destinations"))
            entries(json.optJSONArray("watched")).map { entry ->
                Provider(entry.id, entry.label.ifBlank { entry.displayNumber }, entry.number, entry.canonical, receivers.toList())
            }
        }
        return ForwardingConfig(json.optBoolean("enabled"), providers)
    }

    fun encode(config: ForwardingConfig): String {
        val providers = JSONArray()
        config.providers.forEach { provider ->
            providers.put(JSONObject().put("id", provider.id).put("name", provider.name)
                .put("number", NumberRules.domesticNumber(provider.canonical)).put("canonical", provider.canonical)
                .put("receivers", encodeEntries(provider.receivers)))
        }
        return JSONObject().put("schemaVersion", 2).put("enabled", config.enabled)
            .put("providers", providers).toString()
    }

    private fun entries(json: JSONArray?): List<PhoneEntry> = if (json == null) emptyList() else
        (0 until json.length()).map { index ->
            val item = json.getJSONObject(index)
            val canonical = item.getString("canonical")
            PhoneEntry(item.getString("id"), NumberRules.domesticNumber(canonical), canonical, item.optString("label"))
        }

    private fun encodeEntries(entries: List<PhoneEntry>): JSONArray = JSONArray().also { array ->
        entries.forEach { array.put(JSONObject().put("id", it.id)
            .put("number", NumberRules.domesticNumber(it.canonical)).put("canonical", it.canonical).put("label", it.label)) }
    }
}
