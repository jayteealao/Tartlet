package io.yumemi.tartlet

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Marker interface for UI state representations.
 *
 * Implementations of this interface represent the state of a UI component.
 */
interface UiState

/**
 * Marker interface for UI events.
 *
 * Implementations of this interface represent one-time events that should be consumed by the UI.
 */
interface UiEvent

/**
 * Contract for a Store that manages UI state and events.
 *
 * This interface defines the contract between a Store and its consumers,
 * providing access to the current UI state and a stream of UI events.
 *
 * @param S The type of UI state, which must implement [UiState]
 * @param E The type of UI event, which must implement [UiEvent]
 */
interface StoreContract<S : UiState, E : UiEvent> {
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
