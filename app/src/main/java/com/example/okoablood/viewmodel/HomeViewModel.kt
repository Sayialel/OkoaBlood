package com.example.okoablood.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.okoablood.data.model.BloodRequest
import com.example.okoablood.data.model.HomeUiState
import com.example.okoablood.data.remote.FirebaseService
import com.example.okoablood.data.repository.BloodDonationRepository
import com.example.okoablood.data.repository.RequestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val requestRepository: RequestRepository,
    private val repository: BloodDonationRepository,
    private val firebaseService: FirebaseService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadBloodRequests()
    }

    fun loadBloodRequests() {
        viewModelScope.launch {
            _uiState.value = HomeUiState(isLoading = true)

            try {
                val allRequests = mutableListOf<BloodRequest>()

                // Collect urgent requests
                requestRepository.getUrgentBloodRequests().collect { urgentResult ->
                    if (urgentResult.isSuccess) {
                        allRequests.addAll(urgentResult.getOrDefault(emptyList()))
                    } else {
                        _uiState.value = HomeUiState(
                            isLoading = false,
                            error = urgentResult.exceptionOrNull()?.message
                        )
                        return@collect
                    }
                }

                // Collect recent (non-urgent) requests
                requestRepository.getActiveBloodRequests().collect { recentResult ->
                    if (recentResult.isSuccess) {
                        allRequests.addAll(recentResult.getOrDefault(emptyList()))
                        _uiState.value = HomeUiState(
                            isLoading = false,
                            bloodRequests = allRequests
                        )
                    } else {
                        _uiState.value = HomeUiState(
                            isLoading = false,
                            error = recentResult.exceptionOrNull()?.message
                        )
                    }
                }

            } catch (e: Exception) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    error = e.message ?: "Unexpected error occurred"
                )
            }
        }
    }
}
