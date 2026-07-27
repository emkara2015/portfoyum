package com.antigravity.networthtracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TefasResponseDto(
    @SerialName("data")
    val data: List<TefasFundItemDto>? = null
)

@Serializable
data class TefasFundItemDto(
    @SerialName("FONKODU")
    val fonKodu: String? = null,
    @SerialName("FONUNVAN")
    val fonUnvan: String? = null,
    @SerialName("FIYAT")
    val fiyat: Double? = null,
    @SerialName("TARIH")
    val tarih: Long? = null,
    @SerialName("TOPTOPLAMDEGER")
    val topToplamDeger: Double? = null
)
