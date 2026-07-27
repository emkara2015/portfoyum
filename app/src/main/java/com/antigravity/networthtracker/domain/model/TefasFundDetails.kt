package com.antigravity.networthtracker.domain.model

data class TefasFundDetails(
    val taxPercent: Double? = null,
    val yield1m: Double? = null,
    val yield3m: Double? = null,
    val yield6m: Double? = null,
    val yield1y: Double? = null,
    val yieldYtd: Double? = null
) {
    fun hasAnyData(): Boolean {
        return taxPercent != null || yield1m != null || yield3m != null || yield6m != null || yield1y != null || yieldYtd != null
    }
}
