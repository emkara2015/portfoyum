package com.antigravity.networthtracker.presentation.dashboard

import app.cash.turbine.test
import com.antigravity.networthtracker.data.local.DailyNetWorthDao
import com.antigravity.networthtracker.domain.model.*
import com.antigravity.networthtracker.domain.usecase.CalculateNetWorthUseCase
import com.antigravity.networthtracker.domain.usecase.DeleteAssetUseCase
import com.antigravity.networthtracker.domain.usecase.GetCalculatedAssetsUseCase
import com.antigravity.networthtracker.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val calculateNetWorthUseCase: CalculateNetWorthUseCase = mockk()
    private val getCalculatedAssetsUseCase: GetCalculatedAssetsUseCase = mockk()
    private val deleteAssetUseCase: DeleteAssetUseCase = mockk()
    private val dailyNetWorthDao: DailyNetWorthDao = mockk()

    private fun createViewModel(): DashboardViewModel {
        every { dailyNetWorthDao.getAllDailyNetWorth() } returns flowOf(emptyList())
        coEvery { dailyNetWorthDao.insertDailyNetWorth(any()) } returns Unit
        return DashboardViewModel(
            calculateNetWorthUseCase,
            getCalculatedAssetsUseCase,
            deleteAssetUseCase,
            dailyNetWorthDao
        )
    }

    @Test
    fun `initial state is empty and not loading`() {
        val viewModel = createViewModel()
        val state = viewModel.state.value

        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertFalse(state.isInitialLoadComplete)
        assertTrue(state.assets.isEmpty())
        assertNull(state.netWorthResult)
        assertNull(state.errorMessage)
    }

    @Test
    fun `first load triggers isRefreshing and updates state with success`() = runTest {
        val viewModel = createViewModel()

        val netWorthResult = NetWorthResult(
            totalAssetsTry = 100.0,
            totalLiabilitiesTry = 0.0,
            netWorthTry = 100.0,
            totalAssetsUsd = 3.0,
            totalLiabilitiesUsd = 0.0,
            netWorthUsd = 3.0,
            usdTryRate = 33.0,
            eurTryRate = 36.0
        )
        val testAsset = Asset(id = 1, type = AssetType.CASH, name = "Cash", symbol = null, currency = "TRY", isLiability = false, isAutoUpdate = false)
        val calculatedAsset = CalculatedAsset(
            asset = testAsset,
            totalQuantity = 1.0,
            currentPrice = 100.0,
            currentValue = 100.0,
            totalCost = 100.0,
            profitLoss = 0.0,
            profitLossPercentage = 0.0,
            dailyChangePercentage = 0.0
        )
        val calculatedAssetsList = listOf(calculatedAsset)

        every { calculateNetWorthUseCase() } returns flowOf(Result.success(netWorthResult))
        every { getCalculatedAssetsUseCase() } returns flowOf(Result.success(calculatedAssetsList))

        viewModel.state.test {
            // Initial state check
            val initialItem = awaitItem()
            assertFalse(initialItem.isLoading)
            assertFalse(initialItem.isRefreshing)
            assertFalse(initialItem.isInitialLoadComplete)

            // Trigger load (should trigger isRefreshing = true)
            viewModel.loadDashboardData(isRefresh = true)

            // First state emitted after launch is refreshing state
            val refreshingItem = awaitItem()
            assertFalse(refreshingItem.isLoading)
            assertTrue(refreshingItem.isRefreshing)
            assertFalse(refreshingItem.isInitialLoadComplete)

            // Success state emitted
            val successItem = awaitItem()
            assertFalse(successItem.isLoading)
            assertFalse(successItem.isRefreshing)
            assertTrue(successItem.isInitialLoadComplete)
            assertEquals(netWorthResult, successItem.netWorthResult)
            assertEquals(calculatedAssetsList, successItem.assets)
        }
    }

    @Test
    fun `subsequent loads show isRefreshing progress`() = runTest {
        val viewModel = createViewModel()
        val mockNetWorthResult = NetWorthResult(100.0, 0.0, 100.0, 3.0, 0.0, 3.0, 33.0, 36.0)

        every { calculateNetWorthUseCase() } returns flowOf(Result.success(mockNetWorthResult))
        every { getCalculatedAssetsUseCase() } returns flowOf(Result.success(emptyList()))

        viewModel.state.test {
            awaitItem() // Initial

            // 1. First load starts (sets isRefreshing = true)
            viewModel.loadDashboardData(isRefresh = true)
            assertTrue(awaitItem().isRefreshing) // refreshing state
            
            val successItem = awaitItem()
            assertFalse(successItem.isRefreshing) // success state
            assertTrue(successItem.isInitialLoadComplete)

            // 2. Second load starts (isRefreshing = true)
            viewModel.loadDashboardData(isRefresh = true)
            assertTrue(awaitItem().isRefreshing) // refreshing state
            assertFalse(awaitItem().isRefreshing) // success state
        }
    }

    @Test
    fun `when load fails, updates state with error message`() = runTest {
        val viewModel = createViewModel()

        val errorMessage = "Database Error"
        every { calculateNetWorthUseCase() } returns flowOf(Result.failure(RuntimeException(errorMessage)))
        every { getCalculatedAssetsUseCase() } returns flowOf(Result.success(emptyList()))

        viewModel.state.test {
            awaitItem() // Initial

            viewModel.loadDashboardData(isRefresh = false)
            assertTrue(awaitItem().isRefreshing) // refreshing state

            val errorItem = awaitItem()
            assertFalse(errorItem.isRefreshing)
            assertEquals(errorMessage, errorItem.errorMessage)
        }
    }

    @Test
    fun `deleteAsset calls usecase and triggers refresh`() = runTest {
        val viewModel = createViewModel()
        val testAsset = Asset(id = 1, type = AssetType.CASH, name = "Cash", symbol = null, currency = "TRY", isLiability = false, isAutoUpdate = false)
        val mockNetWorthResult = NetWorthResult(100.0, 0.0, 100.0, 3.0, 0.0, 3.0, 33.0, 36.0)

        coEvery { deleteAssetUseCase(testAsset) } returns Result.success(Unit)
        every { calculateNetWorthUseCase() } returns flowOf(Result.success(mockNetWorthResult))
        every { getCalculatedAssetsUseCase() } returns flowOf(Result.success(emptyList()))

        viewModel.deleteAsset(testAsset)

        coVerify(exactly = 1) { deleteAssetUseCase(testAsset) }
    }
}
