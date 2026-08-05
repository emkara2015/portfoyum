package com.antigravity.networthtracker.domain.usecase

import com.antigravity.networthtracker.domain.model.Asset
import com.antigravity.networthtracker.domain.model.Transaction
import com.antigravity.networthtracker.domain.repository.AssetRepository
import javax.inject.Inject

class AddAssetUseCase @Inject constructor(
    private val assetRepository: AssetRepository
) {
    suspend operator fun invoke(
        asset: Asset,
        initialQuantity: Double,
        initialPrice: Double,
        purchaseDate: Long = System.currentTimeMillis(),
        note: String = ""
    ): Result<Long> {
        if (asset.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Varlık adı boş bırakılamaz."))
        }
        if (asset.isAutoUpdate && asset.symbol.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Dinamik varlıklar için sembol boş bırakılamaz."))
        }
        if (initialQuantity <= 0) {
            return Result.failure(IllegalArgumentException("Miktar 0'dan büyük olmalıdır."))
        }
        if (initialPrice < 0) {
            return Result.failure(IllegalArgumentException("Birim fiyat veya değer negatif olamaz."))
        }

        return try {
            val existingAsset = if (asset.isAutoUpdate && !asset.symbol.isNullOrBlank()) {
                assetRepository.getAssetByTypeAndSymbol(asset.type, asset.symbol)
            } else {
                null
            }

            val assetId = if (existingAsset != null) {
                existingAsset.id
            } else {
                assetRepository.insertAsset(asset)
            }
            
            val initialTransaction = Transaction(
                assetId = assetId,
                quantity = initialQuantity,
                price = initialPrice,
                date = purchaseDate,
                note = note
            )
            assetRepository.insertTransaction(initialTransaction)
            Result.success(assetId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
