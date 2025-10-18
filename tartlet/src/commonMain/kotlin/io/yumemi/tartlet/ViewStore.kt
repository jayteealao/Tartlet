package io.yumemi.tartlet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.filter

/**
 * A view store that holds UI state and provides access to actions and events.
 *
 * This class acts as a container for the current UI state and provides methods
 * to execute actions on the store, render UI based on specific state types,
 * and handle specific event types in Compose.
 *
 * @param ST The type of store, which must implement [Store]
 * @param S The type of UI state
 * @param E The type of UI event
 * @property store The store instance, nullable to support state-only stores
 * @param state A lambda that provides the current UI state
 */
@Suppress("unused")
@Stable
class ViewStore<ST : Store<S, E>, S : Any, E : Any>(
    @PublishedApi internal val store: ST? = null,
    state: () -> S,
) {
    val state: S = state()
    /**
     * Checks equality based on the current state.
     *
     * Two view stores are considered equal if they hold the same state.
     *
     * @param other The object to compare with
     * @return `true` if the states are equal, `false` otherwise
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ViewStore<*, *, *>) return false
        return this.state == other.state
    }

    /**
     * Returns the hash code based on the current state.
     *
     * @return The hash code of the state
     */
    override fun hashCode(): Int {
        return state.hashCode()
    }

    /**
     * Executes an action on the store.
     *
     * This method allows you to call methods on the store to dispatch actions
     * that may update the state or emit events. If the store is null, this method
     * does nothing.
     *
     * @param block The action block to execute on the store
     */
    inline fun action(block: ST.() -> Unit) {
        store?.let(block)
    }

    /**
     * Renders UI for a specific state type.
     *
     * This method checks if the current state is of the specified type [S2] and
     * executes the render block if it matches. This is useful for handling different
     * state variants in a sealed class hierarchy.
     *
     * @param S2 The specific state type to check for
     * @param block The render block to execute if the state matches the type
     */
    inline fun <reified S2 : S> render(block: ViewStore<*, S2, E>.() -> Unit) {
        if (state is S2) {
            @Suppress("UNCHECKED_CAST")
            block(this as ViewStore<*, S2, E>)
        }
    }

    /**
     * Handles specific event types in Compose.
     *
     * This composable function collects events from the store's event flow
     * and executes the provided block for events of type [E2]. The collection is
     * tied to the lifecycle of the composition and will be cancelled when the
     * composable leaves the composition.
     *
     * @param E2 The specific event type to handle
     * @param block The handler block to execute when an event of type [E2] is emitted
     */
    @Composable
    inline fun <reified E2 : E> handle(crossinline block: ViewStore<ST, S, E>.(E2) -> Unit) {
        LaunchedEffect(store) {
            store?.event?.filter { it is E2 }?.collect {
                block(this@ViewStore, it as E2)
            }
        }
    }
}

/**
 * Remembers a [ViewStore] instance in a Compose composition.
 *
 * This composable function creates and remembers a view store that collects state from
 * the provided store. The view store will be recomposed whenever the state changes.
 * The store itself is remembered to ensure stability across recompositions.
 *
 * @param ST The type of store, which must implement [Store]
 * @param S The type of UI state
 * @param E The type of UI event
 * @param store A lambda that provides the store to collect state and events from
 * @return A remembered [ViewStore] instance that updates with state changes
 */
@Suppress("unused")
@Composable
fun <ST : Store<S, E>, S : Any, E : Any> rememberViewStore(store: () -> ST): ViewStore<ST, S, E> {
    val rememberStore = remember { store() } // allow different Store instances to be passed
    val state by rememberStore.state.collectAsState()
    return remember(state) {
        ViewStore(
            store = rememberStore,
            state = { state },
        )
    }
}
