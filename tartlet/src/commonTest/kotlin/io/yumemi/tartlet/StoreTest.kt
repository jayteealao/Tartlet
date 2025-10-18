package io.yumemi.tartlet

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

// Test state implementations - represents different UI states
sealed interface TestState {
    data class Loading(val message: String) : TestState
    data class Success(val value: Int) : TestState
    data class Error(val error: String) : TestState
}

// Test event implementations - represents one-time UI events
sealed interface TestEvent {
    data class ShowToast(val message: String) : TestEvent
    data class NavigateToScreen(val screenId: String) : TestEvent
}

// Test store implementation - minimal store for testing
class TestStore : Store<TestState, TestEvent> {
    private val _state = MutableStateFlow<TestState>(TestState.Loading("Loading..."))
    override val state: StateFlow<TestState> = _state

    private val _event = MutableSharedFlow<TestEvent>()
    override val event = _event

    // Properties to verify action calls
    var actionCalled = false
    var actionValue: Int = 0

    fun updateState(state: TestState) {
        _state.value = state
    }

    fun performAction(value: Int) {
        actionCalled = true
        actionValue = value
    }

    suspend fun emitEvent(event: TestEvent) {
        _event.emit(event)
    }
}

class StoreTest {
    @Test
    fun `ViewStore can be created with state only for preview mode`() {
        val state = TestState.Success(42)
        val viewStore = ViewStore<TestState, TestEvent, TestStore>(
            store = null,
            state = { state },
        )

        assertEquals(state, viewStore.state)
    }

    @Test
    fun `ViewStore can be created with both state and store`() {
        val store = TestStore()
        val state = TestState.Success(100)
        val viewStore = ViewStore<TestState, TestEvent, TestStore>(
            store = store,
            state = { state },
        )

        assertEquals(state, viewStore.state)
    }

    @Test
    fun `ViewStore equality is based on state - same state equals true`() {
        val state = TestState.Success(42)
        val viewStore1 = ViewStore<TestState, TestEvent, TestStore>(
            store = null,
            state = { state },
        )
        val viewStore2 = ViewStore<TestState, TestEvent, TestStore>(
            store = null,
            state = { state },
        )

        assertEquals(viewStore1, viewStore2)
    }

    @Test
    fun `ViewStore equality is based on state - different state equals false`() {
        val viewStore1 = ViewStore<TestState, TestEvent, TestStore>(
            store = null,
            state = { TestState.Success(42) },
        )
        val viewStore2 = ViewStore<TestState, TestEvent, TestStore>(
            store = null,
            state = { TestState.Success(100) },
        )

        assertNotEquals(viewStore1, viewStore2)
    }

    @Test
    fun `ViewStore equals returns true for same instance`() {
        val viewStore = ViewStore<TestState, TestEvent, TestStore>(
            store = null,
            state = { TestState.Success(42) },
        )

        assertTrue(viewStore == viewStore)
    }

    @Test
    fun `ViewStore equals returns false for different types`() {
        val viewStore = ViewStore<TestState, TestEvent, TestStore>(
            store = null,
            state = { TestState.Success(42) },
        )
        val other = "not a view store"

        assertFalse(viewStore.equals(other))
    }

    @Test
    fun `ViewStore hashCode is consistent with equals - same state has same hashCode`() {
        val state = TestState.Success(42)
        val viewStore1 = ViewStore<TestState, TestEvent, TestStore>(
            store = null,
            state = { state },
        )
        val viewStore2 = ViewStore<TestState, TestEvent, TestStore>(
            store = null,
            state = { state },
        )

        assertEquals(viewStore1, viewStore2)
        assertEquals(viewStore1.hashCode(), viewStore2.hashCode())
    }

    @Test
    fun `ViewStore hashCode differs for different states`() {
        val viewStore1 = ViewStore<TestState, TestEvent, TestStore>(
            store = null,
            state = { TestState.Success(42) },
        )
        val viewStore2 = ViewStore<TestState, TestEvent, TestStore>(
            store = null,
            state = { TestState.Success(100) },
        )

        assertNotEquals(viewStore1.hashCode(), viewStore2.hashCode())
    }

    @Test
    fun `ViewStore action executes block when store is present`() {
        val store = TestStore()
        val viewStore = ViewStore<TestState, TestEvent, TestStore>(
            store = store,
            state = { TestState.Loading("Loading...") },
        )

        viewStore.action {
            performAction(42)
        }

        assertTrue(store.actionCalled)
        assertEquals(42, store.actionValue)
    }

    @Test
    fun `ViewStore action does nothing when store is null in preview mode`() {
        val viewStore = ViewStore<TestState, TestEvent, TestStore>(
            store = null,
            state = { TestState.Loading("Loading...") },
        )

        // This should not throw an exception in preview mode
        viewStore.action {
            performAction(42)
        }
    }

    @Test
    fun `ViewStore render executes block when state matches type and narrows state type`() {
        val viewStore = ViewStore<TestState, TestEvent, TestStore>(
            store = null,
            state = { TestState.Success(42) },
        )

        var renderCalled = false
        var capturedValue = 0

        viewStore.render<TestState.Success> {
            renderCalled = true
            capturedValue = state.value // state is narrowed to TestState.Success
        }

        assertTrue(renderCalled)
        assertEquals(42, capturedValue)
    }

    @Test
    fun `ViewStore render does not execute block when state type does not match`() {
        val viewStore = ViewStore<TestState, TestEvent, TestStore>(
            store = null,
            state = { TestState.Loading("Loading...") },
        )

        var renderCalled = false

        viewStore.render<TestState.Success> {
            renderCalled = true
        }

        assertFalse(renderCalled)
    }

    @Test
    fun `ViewStore render works with Error state type`() {
        val viewStore = ViewStore<TestState, TestEvent, TestStore>(
            store = null,
            state = { TestState.Error("Something went wrong") },
        )

        var errorMessage = ""

        viewStore.render<TestState.Error> {
            errorMessage = state.error
        }

        assertEquals("Something went wrong", errorMessage)
    }

    @Test
    fun `ViewStore render works with Loading state type`() {
        val viewStore = ViewStore<TestState, TestEvent, TestStore>(
            store = null,
            state = { TestState.Loading("Please wait...") },
        )

        var loadingMessage = ""

        viewStore.render<TestState.Loading> {
            loadingMessage = state.message
        }

        assertEquals("Please wait...", loadingMessage)
    }

    @Test
    fun `ViewStore action can access and modify the underlying store state`() {
        val store = TestStore()
        store.updateState(TestState.Loading("Loading..."))

        val viewStore = ViewStore<TestState, TestEvent, TestStore>(
            store = store,
            state = { TestState.Loading("Loading...") },
        )

        viewStore.action {
            updateState(TestState.Success(999))
        }

        assertEquals(TestState.Success(999), store.state.value)
    }

    @Test
    fun `ViewStore supports multiple sequential action calls`() {
        val store = TestStore()
        val viewStore = ViewStore<TestState, TestEvent, TestStore>(
            store = store,
            state = { TestState.Loading("Loading...") },
        )

        viewStore.action { performAction(10) }
        assertEquals(10, store.actionValue)

        viewStore.action { performAction(20) }
        assertEquals(20, store.actionValue)

        viewStore.action { performAction(30) }
        assertEquals(30, store.actionValue)
    }

    @Test
    fun `ViewStore can use narrowed state type with wider store type`() {
        // Store is Store<TestState, TestEvent> (wider)
        val store = TestStore()
        store.updateState(TestState.Success(42))

        // ViewStore uses TestState.Success (narrower) but store is TestStore (Store<TestState, TestEvent>)
        val viewStore = ViewStore<TestState.Success, TestEvent, TestStore>(
            store = store,
            state = { TestState.Success(42) },
        )

        assertEquals(42, viewStore.state.value)

        // action should still work
        viewStore.action {
            performAction(100)
        }
        assertTrue(store.actionCalled)
        assertEquals(100, store.actionValue)
    }
}
