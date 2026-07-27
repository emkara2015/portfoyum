package com.antigravity.networthtracker.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.networthtracker.data.local.AppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    fun resetAllData(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isResetting = true) }
            withContext(Dispatchers.IO) {
                try {
                    database.clearAllTables()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            _state.update { it.copy(isResetting = false) }
            onSuccess()
        }
    }
}
