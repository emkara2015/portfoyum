package com.antigravity.networthtracker.presentation.add_asset

import com.antigravity.networthtracker.domain.model.AssetType
import com.antigravity.networthtracker.domain.model.SearchSuggestion

import com.antigravity.networthtracker.domain.model.MetalCategory
import com.antigravity.networthtracker.domain.model.GoldInputMode
import com.antigravity.networthtracker.domain.model.GoldPieceType
import com.antigravity.networthtracker.domain.model.GoldKarat

enum class FundCategory {
    TEFAS,
    OTHER
}

data class AddAssetState(
    val selectedType: AssetType? = null,
    val name: String = "",
    val symbol: String = "",
    val quantity: String = "",
    val price: String = "",
    val currency: String = "TRY",
    val isLoading: Boolean = false,
    val isLivePriceLoading: Boolean = false,
    val purchaseDate: Long = System.currentTimeMillis(),
    val searchSuggestions: List<SearchSuggestion> = emptyList(),
    val isSearchLoading: Boolean = false,
    val errorMessageResId: Int? = null,
    val errorMessageText: String? = null,
    val isSuccess: Boolean = false,
    val isSymbolSelected: Boolean = false,
    
    // Fund Category State (TEFAŞ vs Diğer)
    val fundCategory: FundCategory = FundCategory.TEFAS,
    
    // Precious Metal Specialized States
    val metalCategory: MetalCategory? = null,
    val goldInputMode: GoldInputMode? = null,
    val selectedGoldPieceType: GoldPieceType? = null,
    val selectedGoldKarat: GoldKarat? = null,
    val metalPieceCount: String = "1",
    val metalGramWeight: String = ""
)
