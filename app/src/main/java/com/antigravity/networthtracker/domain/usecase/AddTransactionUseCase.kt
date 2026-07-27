package com.antigravity.networthtracker.domain.usecase

import com.antigravity.networthtracker.domain.model.Transaction
import com.antigravity.networthtracker.domain.repository.AssetRepository
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val assetRepository: AssetRepository
) {
    suspend operator fun invoke(transaction: Transaction): Result<Long> {
        if (transaction.quantity == 0.0) {
            return Result.failure(IllegalArgumentException("İşlem miktarı sıfır olamaz."))
        }
        if (transaction.price < 0.0) {
            return Result.failure(IllegalArgumentException("İşlem birim fiyatı veya değeri negatif olamaz."))
        }
        
        return try {
            val txId = assetRepository.insertTransaction(transaction)
            Result.success(txId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
