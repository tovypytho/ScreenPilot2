package com.example.data

import org.json.JSONArray
import org.json.JSONObject

enum class GeminiKeyHealth {
    NOT_CONFIGURED,
    NOT_TESTED,
    READY,
    AUTH_FAILED,
    PERMISSION_DENIED,
    COOLDOWN,
    TEMPORARY_FAILURE,
    DISABLED
}

data class GeminiKeySlot(
    val id: String,
    val label: String,
    val enabled: Boolean,
    val priority: Int,
    val maskedSuffix: String,
    val healthStatus: String = GeminiKeyHealth.NOT_TESTED.name,
    val lastSuccessTimestamp: Long = 0L,
    val lastFailureType: String = "",
    val cooldownExpiration: Long = 0L,
    val lastCheckedTimestamp: Long = 0L,
    val lastCheckStatus: String = "",
    val lastCheckReason: String = ""
)

object GeminiKeySlotSerializer {
    fun serialize(slots: List<GeminiKeySlot>): String {
        val array = JSONArray()
        for (slot in slots) {
            val obj = JSONObject().apply {
                put("id", slot.id)
                put("label", slot.label)
                put("enabled", slot.enabled)
                put("priority", slot.priority)
                put("maskedSuffix", slot.maskedSuffix)
                put("healthStatus", slot.healthStatus)
                put("lastSuccessTimestamp", slot.lastSuccessTimestamp)
                put("lastFailureType", slot.lastFailureType)
                put("cooldownExpiration", slot.cooldownExpiration)
                put("lastCheckedTimestamp", slot.lastCheckedTimestamp)
                put("lastCheckStatus", slot.lastCheckStatus)
                put("lastCheckReason", slot.lastCheckReason)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun deserialize(jsonStr: String): List<GeminiKeySlot> {
        if (jsonStr.isEmpty()) return emptyList()
        val list = mutableListOf<GeminiKeySlot>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val rawHealth = obj.optString("healthStatus", "NOT_TESTED")
                val normalizedHealth = normalizeHealth(rawHealth)
                list.add(
                    GeminiKeySlot(
                        id = obj.getString("id"),
                        label = obj.optString("label", ""),
                        enabled = obj.optBoolean("enabled", true),
                        priority = obj.optInt("priority", 1),
                        maskedSuffix = obj.optString("maskedSuffix", ""),
                        healthStatus = normalizedHealth,
                        lastSuccessTimestamp = obj.optLong("lastSuccessTimestamp", 0L),
                        lastFailureType = obj.optString("lastFailureType", ""),
                        cooldownExpiration = obj.optLong("cooldownExpiration", 0L),
                        lastCheckedTimestamp = obj.optLong("lastCheckedTimestamp", 0L),
                        lastCheckStatus = obj.optString("lastCheckStatus", ""),
                        lastCheckReason = obj.optString("lastCheckReason", "")
                    )
                )
            }
        } catch (e: Exception) {
            // fallback or empty
        }
        return list
    }

    private fun normalizeHealth(raw: String): String {
        val trimmed = raw.trim().uppercase()
        return when (trimmed) {
            "READY" -> GeminiKeyHealth.READY.name
            "FAILED", "TEMPORARY_FAILURE", "TEMPORARY_FAILED", "TEMPORARYFAILURE" -> GeminiKeyHealth.TEMPORARY_FAILURE.name
            "COOLDOWN" -> GeminiKeyHealth.COOLDOWN.name
            "NOT TESTED", "NOT_TESTED" -> GeminiKeyHealth.NOT_TESTED.name
            "NOT CONFIGURED", "NOT_CONFIGURED" -> GeminiKeyHealth.NOT_CONFIGURED.name
            "AUTH_FAILED", "AUTHFAILED" -> GeminiKeyHealth.AUTH_FAILED.name
            "PERMISSION_DENIED", "PERMISSIONDENIED" -> GeminiKeyHealth.PERMISSION_DENIED.name
            "DISABLED" -> GeminiKeyHealth.DISABLED.name
            else -> {
                if (trimmed.contains("READY")) GeminiKeyHealth.READY.name
                else if (trimmed.contains("FAILED") || trimmed.contains("TEMPORARY")) GeminiKeyHealth.TEMPORARY_FAILURE.name
                else if (trimmed.contains("COOLDOWN")) GeminiKeyHealth.COOLDOWN.name
                else if (trimmed.contains("CONFIGURED")) GeminiKeyHealth.NOT_CONFIGURED.name
                else if (trimmed.contains("AUTH")) GeminiKeyHealth.AUTH_FAILED.name
                else if (trimmed.contains("PERMISSION") || trimmed.contains("DENIED")) GeminiKeyHealth.PERMISSION_DENIED.name
                else if (trimmed.contains("DISABLED")) GeminiKeyHealth.DISABLED.name
                else GeminiKeyHealth.NOT_TESTED.name
            }
        }
    }
}
