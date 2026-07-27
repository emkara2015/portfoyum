package com.antigravity.networthtracker.presentation.markets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.networthtracker.domain.model.NewsCategory
import com.antigravity.networthtracker.domain.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MarketsViewModel @Inject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MarketsState())
    val state: StateFlow<MarketsState> = _state.asStateFlow()

    init {
        loadNews(category = NewsCategory.ECONOMY, isRefresh = false)
    }

    fun loadNews(category: NewsCategory, isRefresh: Boolean = false) {
        viewModelScope.launch {
            _state.update { 
                it.copy(
                    isLoading = !isRefresh, 
                    isRefreshing = isRefresh, 
                    selectedCategory = category,
                    errorMessage = null
                ) 
            }
            newsRepository.getNews(category)
                .onSuccess { news ->
                    _state.update { 
                        it.copy(
                            isLoading = false, 
                            isRefreshing = false, 
                            newsItems = news,
                            errorMessage = null
                        ) 
                    }
                }
                .onFailure { error ->
                    _state.update { 
                        it.copy(
                            isLoading = false, 
                            isRefreshing = false, 
                            errorMessage = error.localizedMessage ?: "Hata oluştu."
                        ) 
                    }
                }
        }
    }

    fun selectCategory(category: NewsCategory) {
        if (_state.value.selectedCategory != category) {
            loadNews(category, isRefresh = false)
        }
    }
}
