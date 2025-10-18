package io.yumemi.tartlet

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Contract for a Store that manages UI state and events.
 *
 * This interface defines the contract between a Store and its consumers,
 * providing access to the current UI state and a stream of UI events.
 *
 * @param S The type of UI state
 * @param E The type of UI event
 */
interface StoreContract<S : Any, E : Any> {
    /**
     * A [StateFlow] that emits the current UI state.
     *
     * Consumers can collect this flow to observe state changes.
     */
    val uiState: StateFlow<S>

    /**
     * A [Flow] that emits one-time UI events.
     *
     * By default, this returns an empty flow. Implementations should override this
     * to provide a flow of events that need to be consumed by the UI.
     */
    val uiEvent: Flow<E> get() = emptyFlow()
}
