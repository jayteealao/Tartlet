package io.yumemi.tartlet

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

// Test state implementations
sealed interface TestState {
    data class Loading(val message: String = "Loading...") : TestState
    data class Success(val value: Int) : TestState
    data class Error(val error: String) : TestState
}

// Test event implementations
sealed interface TestEvent {
    data class ShowToast(val message: String) : TestEvent
    data class NavigateToScreen(val screenId: String) : TestEvent
}

// Test store implementation
class TestStore : Store<TestState, TestEvent> {
    private val _state = MutableStateFlow<TestState>(TestState.Loading())
    override val state: StateFlow<TestState> = _state

    private val _event = MutableSharedFlow<TestEvent>()
    override val event = _event

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
    fun `ViewStore creation with state only`() {
        val state = TestState.Success(42)
        val viewStore = ViewStore<TestState, TestEvent, TestStore>(
            store = null,
            state = { state },
        )

        assertEquals(state, viewStore.state)
    }

    @Test
    fun `ViewStore creation with state and store`() {
        val store = TestStore()
        val state = TestState.Success(100)
        val viewStore = ViewStore(
            store = store,
            state = { state },
        )

        assertEquals(state, viewStore.state)
    }

    @Test
    fun `ViewStore equals returns true for same state`() {
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
    fun `ViewStore equals returns false for different state`() {
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

        assertEquals(viewStore, viewStore)
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
    fun `ViewStore hashCode is consistent with equals`() {
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
        val viewStore = ViewStore(
            store = store,
            state = { TestState.Loading() },
        )

        viewStore.action {
            performAction(42)
        }

        assertTrue(store.actionCalled)
        assertEquals(42, store.actionValue)
    }

    @Test
    fun `ViewStore action does nothing when store is null`() {
        val viewStore = ViewStore<TestState, TestEvent, TestStore>(
            store = null,
            state = { TestState.Loading() },
        )

        // This should not throw an exception
        viewStore.action {
            performAction(42)
        }
    }

    @Test
    fun `ViewStore render executes block when state matches type`() {
        val viewStore = ViewStore<TestState, TestEvent, TestStore>(
            store = null,
            state = { TestState.Success(42) },
        )

        var renderCalled = false
        var capturedValue = 0

        viewStore.render<TestState.Success> {
            renderCalled = true
            capturedValue = state.value
        }

        assertTrue(renderCalled)
        assertEquals(42, capturedValue)
    }

    @Test
    fun `ViewStore render does not execute block when state does not match type`() {
        val viewStore = ViewStore<TestState, TestEvent, TestStore>(
            store = null,
            state = { TestState.Loading() },
        )

        var renderCalled = false

        viewStore.render<TestState.Success> {
            renderCalled = true
        }

        assertFalse(renderCalled)
    }

    @Test
    fun `ViewStore render works with different state types`() {
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
    fun `ViewStore render with Loading state`() {
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
    fun `ViewStore action can access and modify store state`() {
        val store = TestStore()
        store.updateState(TestState.Loading())

        val viewStore = ViewStore(
            store = store,
            state = { TestState.Loading() },
        )

        viewStore.action {
            updateState(TestState.Success(999))
        }

        assertEquals(TestState.Success(999), store.state.value)
    }

    @Test
    fun `ViewStore with multiple action calls`() {
        val store = TestStore()
        val viewStore = ViewStore(
            store = store,
            state = { TestState.Loading() },
        )

        viewStore.action { performAction(10) }
        assertEquals(10, store.actionValue)

        viewStore.action { performAction(20) }
        assertEquals(20, store.actionValue)

        viewStore.action { performAction(30) }
        assertEquals(30, store.actionValue)
    }
}
