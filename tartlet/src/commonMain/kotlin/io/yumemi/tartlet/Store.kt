package io.yumemi.tartlet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.filter

/**
 * A store that holds UI state and provides access to actions and events.
 *
 * This class acts as a container for the current UI state and provides methods
 * to execute actions on the store contract, render UI based on specific state types,
 * and handle specific event types in Compose.
 *
 * @param SC The type of store contract, which must implement [StoreContract]
 * @param S The type of UI state, which must implement [UiState]
 * @param E The type of UI event, which must implement [UiEvent]
 * @property uiState The current UI state
 * @property storeContract The store contract instance, nullable to support state-only stores
 */
@Suppress("unused")
@Stable
class Store<SC : StoreContract<S, E>, S : UiState, E : UiEvent>(
    val uiState: S,
    @PublishedApi internal val storeContract: SC? = null,
) {
    /**
     * Checks equality based on the current state.
     *
     * Two stores are considered equal if they hold the same state.
     *
     * @param other The object to compare with
     * @return `true` if the states are equal, `false` otherwise
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Store<*, *, *>) return false
        return this.uiState == other.uiState
    }

    /**
     * Returns the hash code based on the current state.
     *
     * @return The hash code of the state
     */
    override fun hashCode(): Int {
        return uiState.hashCode()
    }

    /**
     * Executes an action on the store contract.
     *
     * This method allows you to call methods on the store contract to dispatch actions
     * that may update the state or emit events. If the store contract is null, this method
     * does nothing.
     *
     * @param block The action block to execute on the store contract
     */
    inline fun action(block: SC.() -> Unit) {
        storeContract?.let(block)
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
    inline fun <reified S2 : S> render(block: Store<*, S2, E>.() -> Unit) {
        if (uiState is S2) {
            @Suppress("UNCHECKED_CAST")
            block(this as Store<*, S2, E>)
        }
    }

    /**
     * Handles specific event types in Compose.
     *
     * This composable function collects events from the store contract's event flow
     * and executes the provided block for events of type [E2]. The collection is
     * tied to the lifecycle of the composition and will be cancelled when the
     * composable leaves the composition.
     *
     * @param E2 The specific event type to handle
     * @param block The handler block to execute when an event of type [E2] is emitted
     */
    @Composable
    inline fun <reified E2 : E> handle(crossinline block: Store<SC, S, E>.(E2) -> Unit) {
        LaunchedEffect(storeContract) {
            storeContract?.uiEvent?.filter { it is E2 }?.collect {
                block(this@Store, it as E2)
            }
        }
    }
}

/**
 * Remembers a [Store] instance in a Compose composition.
 *
 * This composable function creates and remembers a store that collects state from
 * the provided store contract. The store will be recomposed whenever the state changes.
 * The store contract itself is remembered to ensure stability across recompositions.
 *
 * @param SC The type of store contract, which must implement [StoreContract]
 * @param S The type of UI state, which must implement [UiState]
 * @param E The type of UI event, which must implement [UiEvent]
 * @param storeContract The store contract to collect state and events from
 * @return A remembered [Store] instance that updates with state changes
 */
@Suppress("unused")
@Composable
fun <SC : StoreContract<S, E>, S : UiState, E : UiEvent> rememberStore(storeContract: SC): Store<SC, S, E> {
    val rememberStoreContract =
        remember { storeContract } // allow different Store Contract instances to be passed
    val state by rememberStoreContract.uiState.collectAsState()
    return remember(state) {
        Store(
            uiState = state,
            storeContract = storeContract,
        )
    }
}
