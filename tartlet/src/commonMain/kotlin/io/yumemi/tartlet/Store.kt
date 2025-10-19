package io.yumemi.tartlet

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Interface for a Store implementation that manages UI state and events.
 *
 * This interface defines the contract for state management, typically implemented
 * by ViewModels. It provides access to the current UI state through a [StateFlow]
 * and optionally emits one-time UI events through a [Flow].
 *
 * Implementations should expose the [state] property to represent the current UI state,
 * and optionally override the [event] property to emit one-time events like navigation
 * or showing toasts.
 *
 * @param S The type of UI state (covariant)
 * @param E The type of UI event (covariant, use [Nothing] if no events are needed)
 */
interface Store<out S : Any, out E : Any> {
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
