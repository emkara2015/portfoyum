package com.antigravity.networthtracker.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.networthtracker.data.local.DailyNetWorthDao
import com.antigravity.networthtracker.data.local.entity.DailyNetWorthEntity
import com.antigravity.networthtracker.domain.model.Asset
import com.antigravity.networthtracker.domain.usecase.CalculateNetWorthUseCase
import com.antigravity.networthtracker.domain.usecase.DeleteAssetUseCase
import com.antigravity.networthtracker.domain.usecase.GetCalculatedAssetsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val calculateNetWorthUseCase: CalculateNetWorthUseCase,
    private val getCalculatedAssetsUseCase: GetCalculatedAssetsUseCase,
    private val deleteAssetUseCase: DeleteAssetUseCase,
    private val dailyNetWorthDao: DailyNetWorthDao
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private var dbCollectionJob: Job? = null
    private var isFirstLoad = true

    val dailyNetWorthHistory = dailyNetWorthDao.getAllDailyNetWorth()

    init {
        // Initial load is triggered from UI lifecycle
    }

    fun loadDashboardData(isRefresh: Boolean = false) {
        dbCollectionJob?.cancel()
        dbCollectionJob = viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }

            combine(
                calculateNetWorthUseCase(),
                getCalculatedAssetsUseCase()
            ) { netWorthResult, assetsResult ->
                if (netWorthResult.isSuccess && assetsResult.isSuccess) {
                    val result = netWorthResult.getOrNull()
                    if (result != null) {
                        viewModelScope.launch {
                            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                            dailyNetWorthDao.insertDailyNetWorth(
                                DailyNetWorthEntity(
                                    date = today,
                                    netWorthTry = result.netWorthTry,
                                    netWorthUsd = result.netWorthUsd
                                )
                            )
                        }
                    }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isInitialLoadComplete = true,
                            netWorthResult = result,
                            assets = assetsResult.getOrDefault(emptyList()),
                            errorMessage = null
                        )
                    }
                } else {
                    val error = netWorthResult.exceptionOrNull() ?: assetsResult.exceptionOrNull()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = error?.localizedMessage ?: "Veri yüklenirken hata oluştu."
                        )
                    }
                }
            }.catch { e ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = e.localizedMessage ?: "Beklenmeyen hata."
                    )
                }
            }.collect()
        }
    }

    fun deleteAsset(asset: Asset) {
        viewModelScope.launch {
            deleteAssetUseCase(asset).onSuccess {
                loadDashboardData(isRefresh = false)
            }.onFailure { e ->
                _state.update {
                    it.copy(errorMessage = "Varlık silinirken hata: ${e.localizedMessage}")
                }
            }
        }
    }
}
