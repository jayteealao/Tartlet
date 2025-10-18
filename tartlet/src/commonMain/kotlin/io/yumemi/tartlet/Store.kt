package io.yumemi.tartlet

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Interface for a Store implementation that manages UI state and events.
 *
 * This interface defines the contract between a Store implementation and its consumers,
 * providing access to the current UI state and a stream of UI events.
 *
 * @param S The type of UI state
 * @param E The type of UI event
 */
interface Store<S : Any, E : Any> {
    /**
     * A [StateFlow] that emits the current UI state.
     *
     * Consumers can collect this flow to observe state changes.
     */
    val state: StateFlow<S>

    /**
     * A [Flow] that emits one-time UI events.
     *
     * By default, this returns an empty flow. Implementations should override this
     * to provide a flow of events that need to be consumed by the UI.
     */
    val event: Flow<E> get() = emptyFlow()
}
