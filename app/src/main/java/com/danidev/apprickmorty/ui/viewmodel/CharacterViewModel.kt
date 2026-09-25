package com.danidev.apprickmorty.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

sealed interface CharacterUiState {
    object Loading : CharacterUiState
    data class Success(val characters: List<RickCharacter>) : CharacterUiState
    data class Error(val message: String) : CharacterUiState
}

@OptIn(FlowPreview::class)
class CharacterViewModel : ViewModel() {
    private val repository = CharacterRepository()

    private val _uiState = MutableStateFlow<CharacterUiState>(CharacterUiState.Loading)
    val uiState: StateFlow<CharacterUiState> = _uiState

    // Estado del query de búsqueda
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    init {
        // Escucha cambios en la búsqueda con debounce para evitar Spam de llamadas HTTP
        _searchQuery
            .debounce(300L)
            .distinctUntilChanged()
            .onEach { query ->
                fetchCharacters(query)
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    private fun fetchCharacters(query: String = "") {
        viewModelScope.launch {
            _uiState.value = CharacterUiState.Loading
            repository.getCharacters(name = query.ifBlank { null })
                .onSuccess { list ->
                    _uiState.value = CharacterUiState.Success(list)
                }
                .onFailure { error ->
                    _uiState.value = CharacterUiState.Error("No se encontraron resultados")
                }
        }
    }
}