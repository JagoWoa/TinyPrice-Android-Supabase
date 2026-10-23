package com.example.tinyprice_android_supabase.presentation.deals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tinyprice_android_supabase.data.model.ProductOffer
import com.example.tinyprice_android_supabase.data.repository.DealsRepository
import com.example.tinyprice_android_supabase.data.repository.DealsRepositoryProvider
import com.example.tinyprice_android_supabase.data.repository.RemoteSearchException
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DealsUiState(
    val query: String = "",
    val submittedQuery: String = "",
    val offers: List<ProductOffer> = emptyList(),
    val hasSearched: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isPreviewData: Boolean = true,
    val selectedOffer: ProductOffer? = null,
)

sealed interface DealsEvent {
    data class QueryChanged(val value: String) : DealsEvent
    data object SubmitSearch : DealsEvent
    data class UseSuggestion(val value: String) : DealsEvent
    data class OpenOffer(val offer: ProductOffer) : DealsEvent
    data object Back : DealsEvent
}

class DealsViewModel(
    private val repository: DealsRepository = DealsRepositoryProvider.create(),
) : ViewModel() {
    private var searchJob: Job? = null
    private val _state = MutableStateFlow(DealsUiState(isPreviewData = repository.isPreviewData))
    val state: StateFlow<DealsUiState> = _state.asStateFlow()

    fun onEvent(event: DealsEvent) {
        when (event) {
            is DealsEvent.QueryChanged -> _state.update { it.copy(query = event.value, selectedOffer = null, errorMessage = null) }
            DealsEvent.SubmitSearch -> search(_state.value.query)
            is DealsEvent.UseSuggestion -> {
                _state.update { it.copy(query = event.value, selectedOffer = null, errorMessage = null) }
                search(event.value)
            }
            is DealsEvent.OpenOffer -> _state.update { it.copy(selectedOffer = event.offer) }
            DealsEvent.Back -> _state.update { it.copy(selectedOffer = null) }
        }
    }

    private fun search(query: String) {
        searchJob?.cancel()
        val normalizedQuery = query.trim()
        if (normalizedQuery.length !in 2..200) {
            _state.update {
                it.copy(
                    hasSearched = true,
                    offers = emptyList(),
                    selectedOffer = null,
                    isLoading = false,
                    errorMessage = if (normalizedQuery.isEmpty()) {
                        "Escribe un producto para iniciar la búsqueda."
                    } else {
                        "La búsqueda debe tener entre 2 y 200 caracteres."
                    },
                )
            }
            return
        }

        _state.update {
            it.copy(isLoading = true, hasSearched = true, submittedQuery = normalizedQuery,
                selectedOffer = null, errorMessage = null, offers = emptyList())
        }
        searchJob = viewModelScope.launch {
            try {
                val offers = repository.search(normalizedQuery)
                _state.update { it.copy(offers = offers, isLoading = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: RemoteSearchException) {
                _state.update { it.copy(offers = emptyList(), isLoading = false, errorMessage = exception.message) }
            } catch (_: Exception) {
                _state.update {
                    it.copy(offers = emptyList(), isLoading = false,
                        errorMessage = "No fue posible completar la búsqueda. Inténtalo de nuevo.")
                }
            }
        }
    }
}
