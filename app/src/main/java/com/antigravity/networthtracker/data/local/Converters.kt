package com.antigravity.networthtracker.data.local

import androidx.room.TypeConverter
import com.antigravity.networthtracker.domain.model.AssetType

class Converters {
    @TypeConverter
    fun fromAssetType(type: AssetType): String {
        return type.name
    }

    @TypeConverter
    fun toAssetType(value: String): AssetType {
        return AssetType.valueOf(value)
    }
}
