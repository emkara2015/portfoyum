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

class CalculateNetWorthUseCaseTest {

    private lateinit var assetRepository: AssetRepository
    private lateinit var livePriceRepository: LivePriceRepository
    private lateinit var useCase: CalculateNetWorthUseCase

    @Before
    fun setUp() {
        assetRepository = mockk()
        livePriceRepository = mockk()
        useCase = CalculateNetWorthUseCase(assetRepository, livePriceRepository)
    }

    @Test
    fun `when assets list is empty, should calculate net worth as zero`() = runTest {
        every { assetRepository.getAssetsWithTransactions() } returns flowOf(emptyList())
        coEvery { livePriceRepository.getLivePrices(any(), any()) } returns Result.success(
            mapOf(
                "USDTRY=X" to PriceInfo(33.0, 0.0),
                "EURTRY=X" to PriceInfo(36.0, 0.0)
            )
        )

        useCase().test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            val netWorthResult = result.getOrNull()!!
            assertEquals(0.0, netWorthResult.totalAssetsTry, 0.001)
            assertEquals(0.0, netWorthResult.totalLiabilitiesTry, 0.001)
            assertEquals(0.0, netWorthResult.netWorthTry, 0.001)
            awaitComplete()
        }
    }

    @Test
    fun `when assets have mixed static, dynamic, assets and liabilities, calculates correctly`() = runTest {
        // Static Asset in TRY (e.g. Cash 1000 TRY)
        val staticAsset = Asset(id = 1, type = AssetType.CASH, name = "Nakit", symbol = null, currency = "TRY", isLiability = false, isAutoUpdate = false)
        val staticTx = Transaction(id = 1, assetId = 1, quantity = 1.0, price = 1000.0, date = 0L)
        val staticAssetWithTx = AssetWithTransactions(staticAsset, listOf(staticTx))

        // Dynamic Asset in USD (e.g. 10 AAPL Stocks)
        val dynamicAsset = Asset(id = 2, type = AssetType.STOCK, name = "Apple", symbol = "AAPL", currency = "USD", isLiability = false, isAutoUpdate = true)
        val dynamicTx = Transaction(id = 2, assetId = 2, quantity = 10.0, price = 150.0, date = 0L) // cost is 150 USD
        val dynamicAssetWithTx = AssetWithTransactions(dynamicAsset, listOf(dynamicTx))

        // Liability in EUR (e.g. Loan 500 EUR)
        val liabilityAsset = Asset(id = 3, type = AssetType.DEBT, name = "Kredi", symbol = null, currency = "EUR", isLiability = true, isAutoUpdate = false)
        val liabilityTx = Transaction(id = 3, assetId = 3, quantity = 1.0, price = 500.0, date = 0L)
        val liabilityAssetWithTx = AssetWithTransactions(liabilityAsset, listOf(liabilityTx))

        every { assetRepository.getAssetsWithTransactions() } returns flowOf(
            listOf(staticAssetWithTx, dynamicAssetWithTx, liabilityAssetWithTx)
        )

        // Mock live price for AAPL as 200.0 USD
        coEvery { livePriceRepository.getLivePrices(any(), any()) } returns Result.success(
            mapOf(
                "AAPL" to PriceInfo(200.0, 1.2),
                "USDTRY=X" to PriceInfo(33.0, 0.0),
                "EURTRY=X" to PriceInfo(36.0, 0.0)
            )
        )

        useCase().test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            val netWorthResult = result.getOrNull()!!

            // Calculations:
            // 1. Static Asset: 1.0 * 1000.0 TRY = 1000.0 TRY
            // 2. Dynamic Asset: 10.0 * 200.0 USD = 2000.0 USD -> * 33.0 = 66000.0 TRY
            // Total Assets TRY = 1000.0 + 66000.0 = 67000.0 TRY
            // 3. Liability: 1.0 * 500.0 EUR = 500.0 EUR -> * 36.0 = 18000.0 TRY
            // Total Liabilities TRY = 18000.0 TRY
            // Net Worth TRY = 67000.0 - 18000.0 = 49000.0 TRY

            assertEquals(67000.0, netWorthResult.totalAssetsTry, 0.001)
            assertEquals(18000.0, netWorthResult.totalLiabilitiesTry, 0.001)
            assertEquals(49000.0, netWorthResult.netWorthTry, 0.001)
            
            // Total Assets USD = 67000.0 / 33.0 = 2030.303 USD
            assertEquals(67000.0 / 33.0, netWorthResult.totalAssetsUsd, 0.001)
            assertEquals(18000.0 / 33.0, netWorthResult.totalLiabilitiesUsd, 0.001)
            assertEquals(49000.0 / 33.0, netWorthResult.netWorthUsd, 0.001)

            assertEquals(33.0, netWorthResult.usdTryRate, 0.001)
            assertEquals(36.0, netWorthResult.eurTryRate, 0.001)
            awaitComplete()
        }
    }

    @Test
    fun `when livePriceRepository throws exception, returns failure`() = runTest {
        val dynamicAsset = Asset(id = 2, type = AssetType.STOCK, name = "Apple", symbol = "AAPL", currency = "USD", isLiability = false, isAutoUpdate = true)
        val dynamicTx = Transaction(id = 2, assetId = 2, quantity = 10.0, price = 150.0, date = 0L)
        val dynamicAssetWithTx = AssetWithTransactions(dynamicAsset, listOf(dynamicTx))

        every { assetRepository.getAssetsWithTransactions() } returns flowOf(listOf(dynamicAssetWithTx))
        
        val exception = RuntimeException("Network Error")
        coEvery { livePriceRepository.getLivePrices(any(), any()) } returns Result.failure(exception)

        useCase().test {
            val result = awaitItem()
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
            awaitComplete()
        }
    }
}
