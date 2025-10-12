package io.yumemi.tartlet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.filter

@Suppress("unused")
@Stable
class Store<SC : StoreContract<S, E>, S : UiState, E : UiEvent>(
    val state: S,
    @PublishedApi internal val storeContract: SC? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Store<*, *, *>) return false
        return this.state == other.state
    }

    override fun hashCode(): Int {
        return state.hashCode()
    }

    inline fun action(block: SC.() -> Unit) {
        storeContract?.let(block)
    }

    inline fun <reified S2 : S> render(block: Store<*, S2, E>.() -> Unit) {
        if (state is S2) {
            @Suppress("UNCHECKED_CAST")
            block(this as Store<*, S2, E>)
        }
    }

    @Composable
    inline fun <reified E2 : E> handle(crossinline block: Store<SC, S, E>.(E2) -> Unit) {
        LaunchedEffect(storeContract) {
            storeContract?.uiEvent?.filter { it is E2 }?.collect {
                block(this@Store, it as E2)
            }
        }
    }
}

@Suppress("unused")
@Composable
fun <SC : StoreContract<S, E>, S : UiState, E : UiEvent> rememberStore(storeContract: SC): Store<SC, S, E> {
    val rememberStoreContract = remember { storeContract } // allow different Store Contract instances to be passed
    val state by rememberStoreContract.uiState.collectAsState()
    return remember(state) {
        Store(
            state = state,
            storeContract = storeContract,
        )
    }
}
