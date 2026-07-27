package com.antigravity.networthtracker.domain.usecase

import app.cash.turbine.test
import com.antigravity.networthtracker.domain.model.*
import com.antigravity.networthtracker.domain.repository.AssetRepository
import com.antigravity.networthtracker.domain.repository.LivePriceRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetCalculatedAssetsUseCaseTest {

    private lateinit var assetRepository: AssetRepository
    private lateinit var livePriceRepository: LivePriceRepository
    private lateinit var useCase: GetCalculatedAssetsUseCase

    @Before
    fun setUp() {
        assetRepository = mockk()
        livePriceRepository = mockk()
        useCase = GetCalculatedAssetsUseCase(assetRepository, livePriceRepository)
    }

    @Test
    fun `when liability debt increases, profitLoss should be negative (loss)`() = runTest {
        // Initial debt cost was 10,000 TRY, current debt value grew to 15,000 TRY
        val debtAsset = Asset(
            id = 1,
            type = AssetType.DEBT,
            name = "Kredi",
            symbol = null,
            currency = "TRY",
            isLiability = true,
            isAutoUpdate = false,
            initialPrice = 10000.0
        )
        val debtTx = Transaction(id = 1, assetId = 1, quantity = 1.0, price = 15000.0, date = 0L)
        val debtAssetWithTx = AssetWithTransactions(debtAsset, listOf(debtTx))

        every { assetRepository.getAssetsWithTransactions() } returns flowOf(listOf(debtAssetWithTx))
        coEvery { livePriceRepository.getLivePrices(any(), any()) } returns Result.success(emptyMap())

        useCase().test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            val calculatedAssets = result.getOrNull()!!
            assertEquals(1, calculatedAssets.size)

            val calculated = calculatedAssets.first()
            assertEquals(15000.0, calculated.currentValue, 0.001)
            assertEquals(10000.0, calculated.totalCost, 0.001)
            // For liability, profitLoss = totalCost - currentValue = 10000 - 15000 = -5000 (Loss)
            assertEquals(-5000.0, calculated.profitLoss, 0.001)
            assertEquals(-50.0, calculated.profitLossPercentage, 0.001)
            awaitComplete()
        }
    }

    @Test
    fun `when liability debt decreases, profitLoss should be positive (profit)`() = runTest {
        // Initial debt cost was 10,000 TRY, paid down so current debt value is 8,000 TRY
        val debtAsset = Asset(
            id = 1,
            type = AssetType.DEBT,
            name = "Kredi",
            symbol = null,
            currency = "TRY",
            isLiability = true,
            isAutoUpdate = false,
            initialPrice = 10000.0
        )
        val debtTx = Transaction(id = 1, assetId = 1, quantity = 1.0, price = 8000.0, date = 0L)
        val debtAssetWithTx = AssetWithTransactions(debtAsset, listOf(debtTx))

        every { assetRepository.getAssetsWithTransactions() } returns flowOf(listOf(debtAssetWithTx))
        coEvery { livePriceRepository.getLivePrices(any(), any()) } returns Result.success(emptyMap())

        useCase().test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            val calculatedAssets = result.getOrNull()!!
            assertEquals(1, calculatedAssets.size)

            val calculated = calculatedAssets.first()
            assertEquals(8000.0, calculated.currentValue, 0.001)
            assertEquals(10000.0, calculated.totalCost, 0.001)
            // For liability, profitLoss = totalCost - currentValue = 10000 - 8000 = +2000 (Gain)
            assertEquals(2000.0, calculated.profitLoss, 0.001)
            assertEquals(20.0, calculated.profitLossPercentage, 0.001)
            awaitComplete()
        }
    }

    @Test
    fun `when regular asset value increases, profitLoss should be positive`() = runTest {
        val stockAsset = Asset(
            id = 2,
            type = AssetType.STOCK,
            name = "Hisse",
            symbol = "THYAO",
            currency = "TRY",
            isLiability = false,
            isAutoUpdate = true
        )
        val stockWithTx = AssetWithTransactions(stockAsset, listOf(Transaction(id = 2, assetId = 2, quantity = 10.0, price = 100.0, date = 0L)))

        every { assetRepository.getAssetsWithTransactions() } returns flowOf(listOf(stockWithTx))
        coEvery { livePriceRepository.getLivePrices(any(), any()) } returns Result.success(
            mapOf("THYAO" to PriceInfo(price = 150.0, dailyChangePercent = 5.0))
        )

        useCase().test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            val calculatedAssets = result.getOrNull()!!
            val calculated = calculatedAssets.first()
            assertEquals(1500.0, calculated.currentValue, 0.001)
            assertEquals(1000.0, calculated.totalCost, 0.001)
            assertEquals(500.0, calculated.profitLoss, 0.001)
            assertEquals(50.0, calculated.profitLossPercentage, 0.001)
            awaitComplete()
        }
    }

    @Test
    fun `when fund asset has positive profit and stopaj tax, net values should deduct stopaj`() = runTest {
        val fundAsset = Asset(
            id = 3,
            type = AssetType.FUND,
            name = "Para Piyasası Fonu",
            symbol = "TMM",
            currency = "TRY",
            isLiability = false,
            isAutoUpdate = true
        )
        val fundWithTx = AssetWithTransactions(fundAsset, listOf(Transaction(id = 3, assetId = 3, quantity = 100.0, price = 10.0, date = 0L)))
        val fundDetails = TefasFundDetails(taxPercent = 10.0)

        every { assetRepository.getAssetsWithTransactions() } returns flowOf(listOf(fundWithTx))
        coEvery { livePriceRepository.getLivePrices(any(), any()) } returns Result.success(
            mapOf("TMM" to PriceInfo(price = 15.0, dailyChangePercent = 1.0, tefasFundDetails = fundDetails))
        )

        useCase().test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            val calculatedAssets = result.getOrNull()!!
            val calculated = calculatedAssets.first()

            assertEquals(1500.0, calculated.currentValue, 0.001)
            assertEquals(500.0, calculated.profitLoss, 0.001)
            assertEquals(50.0, calculated.stopajAmount, 0.001)
            assertEquals(1450.0, calculated.netCurrentValue, 0.001)
            assertEquals(450.0, calculated.netProfitLoss, 0.001)
            assertEquals(45.0, calculated.netProfitLossPercentage, 0.001)
            awaitComplete()
        }
    }
}
