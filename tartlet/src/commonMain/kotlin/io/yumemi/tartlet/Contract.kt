package io.yumemi.tartlet

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

interface UiState
interface UiEvent

interface StoreContract<S : UiState, E : UiEvent> {
    val uiState: StateFlow<S>
    val uiEvent: Flow<E> get() = emptyFlow()
}
