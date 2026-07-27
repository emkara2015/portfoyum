package com.antigravity.networthtracker.domain.usecase

import com.antigravity.networthtracker.domain.model.Asset
import com.antigravity.networthtracker.domain.repository.AssetRepository
import javax.inject.Inject

class DeleteAssetUseCase @Inject constructor(
    private val assetRepository: AssetRepository
) {
    suspend operator fun invoke(asset: Asset): Result<Unit> {
        return try {
            assetRepository.deleteAsset(asset)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
