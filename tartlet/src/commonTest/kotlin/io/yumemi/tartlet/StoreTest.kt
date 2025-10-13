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
sealed interface TestState : UiState {
    data class Loading(val message: String = "Loading...") : TestState
    data class Success(val value: Int) : TestState
    data class Error(val error: String) : TestState
}

// Test event implementations
sealed interface TestEvent : UiEvent {
    data class ShowToast(val message: String) : TestEvent
    data class NavigateToScreen(val screenId: String) : TestEvent
}

// Test store contract implementation
class TestStoreContract : StoreContract<TestState, TestEvent> {
    private val _uiState = MutableStateFlow<TestState>(TestState.Loading())
    override val uiState: StateFlow<TestState> = _uiState

    private val _uiEvent = MutableSharedFlow<TestEvent>()
    override val uiEvent = _uiEvent

    var actionCalled = false
    var actionValue: Int = 0

    fun updateState(state: TestState) {
        _uiState.value = state
    }

    fun performAction(value: Int) {
        actionCalled = true
        actionValue = value
    }

    suspend fun emitEvent(event: TestEvent) {
        _uiEvent.emit(event)
    }
}

class StoreTest {
    @Test
    fun `Store creation with state only`() {
        val state = TestState.Success(42)
        val store = Store<TestStoreContract, TestState, TestEvent>(
            uiState = state,
            storeContract = null,
        )

        assertEquals(state, store.uiState)
    }

    @Test
    fun `Store creation with state and contract`() {
        val contract = TestStoreContract()
        val state = TestState.Success(100)
        val store = Store(
            uiState = state,
            storeContract = contract,
        )

        assertEquals(state, store.uiState)
    }

    @Test
    fun `Store equals returns true for same state`() {
        val state = TestState.Success(42)
        val store1 = Store<TestStoreContract, TestState, TestEvent>(
            uiState = state,
            storeContract = null,
        )
        val store2 = Store<TestStoreContract, TestState, TestEvent>(
            uiState = state,
            storeContract = null,
        )

        assertEquals(store1, store2)
    }

    @Test
    fun `Store equals returns false for different state`() {
        val store1 = Store<TestStoreContract, TestState, TestEvent>(
            uiState = TestState.Success(42),
            storeContract = null,
        )
        val store2 = Store<TestStoreContract, TestState, TestEvent>(
            uiState = TestState.Success(100),
            storeContract = null,
        )

        assertNotEquals(store1, store2)
    }

    @Test
    fun `Store equals returns true for same instance`() {
        val store = Store<TestStoreContract, TestState, TestEvent>(
            uiState = TestState.Success(42),
            storeContract = null,
        )

        assertEquals(store, store)
    }

    @Test
    fun `Store equals returns false for different types`() {
        val store = Store<TestStoreContract, TestState, TestEvent>(
            uiState = TestState.Success(42),
            storeContract = null,
        )
        val other = "not a store"

        assertFalse(store.equals(other))
    }

    @Test
    fun `Store hashCode is consistent with equals`() {
        val state = TestState.Success(42)
        val store1 = Store<TestStoreContract, TestState, TestEvent>(
            uiState = state,
            storeContract = null,
        )
        val store2 = Store<TestStoreContract, TestState, TestEvent>(
            uiState = state,
            storeContract = null,
        )

        assertEquals(store1, store2)
        assertEquals(store1.hashCode(), store2.hashCode())
    }

    @Test
    fun `Store hashCode differs for different states`() {
        val store1 = Store<TestStoreContract, TestState, TestEvent>(
            uiState = TestState.Success(42),
            storeContract = null,
        )
        val store2 = Store<TestStoreContract, TestState, TestEvent>(
            uiState = TestState.Success(100),
            storeContract = null,
        )

        assertNotEquals(store1.hashCode(), store2.hashCode())
    }

    @Test
    fun `Store action executes block when contract is present`() {
        val contract = TestStoreContract()
        val store = Store(
            uiState = TestState.Loading(),
            storeContract = contract,
        )

        store.action {
            performAction(42)
        }

        assertTrue(contract.actionCalled)
        assertEquals(42, contract.actionValue)
    }

    @Test
    fun `Store action does nothing when contract is null`() {
        val store = Store<TestStoreContract, TestState, TestEvent>(
            uiState = TestState.Loading(),
            storeContract = null,
        )

        // This should not throw an exception
        store.action {
            performAction(42)
        }
    }

    @Test
    fun `Store render executes block when state matches type`() {
        val store = Store<TestStoreContract, TestState, TestEvent>(
            uiState = TestState.Success(42),
            storeContract = null,
        )

        var renderCalled = false
        var capturedValue = 0

        store.render<TestState.Success> {
            renderCalled = true
            capturedValue = uiState.value
        }

        assertTrue(renderCalled)
        assertEquals(42, capturedValue)
    }

    @Test
    fun `Store render does not execute block when state does not match type`() {
        val store = Store<TestStoreContract, TestState, TestEvent>(
            uiState = TestState.Loading(),
            storeContract = null,
        )

        var renderCalled = false

        store.render<TestState.Success> {
            renderCalled = true
        }

        assertFalse(renderCalled)
    }

    @Test
    fun `Store render works with different state types`() {
        val store = Store<TestStoreContract, TestState, TestEvent>(
            uiState = TestState.Error("Something went wrong"),
            storeContract = null,
        )

        var errorMessage = ""

        store.render<TestState.Error> {
            errorMessage = uiState.error
        }

        assertEquals("Something went wrong", errorMessage)
    }

    @Test
    fun `Store render with Loading state`() {
        val store = Store<TestStoreContract, TestState, TestEvent>(
            uiState = TestState.Loading("Please wait..."),
            storeContract = null,
        )

        var loadingMessage = ""

        store.render<TestState.Loading> {
            loadingMessage = uiState.message
        }

        assertEquals("Please wait...", loadingMessage)
    }

    @Test
    fun `Store action can access and modify contract state`() {
        val contract = TestStoreContract()
        contract.updateState(TestState.Loading())

        val store = Store(
            uiState = TestState.Loading(),
            storeContract = contract,
        )

        store.action {
            updateState(TestState.Success(999))
        }

        assertEquals(TestState.Success(999), contract.uiState.value)
    }

    @Test
    fun `Store with multiple action calls`() {
        val contract = TestStoreContract()
        val store = Store(
            uiState = TestState.Loading(),
            storeContract = contract,
        )

        store.action { performAction(10) }
        assertEquals(10, contract.actionValue)

        store.action { performAction(20) }
        assertEquals(20, contract.actionValue)

        store.action { performAction(30) }
        assertEquals(30, contract.actionValue)
    }
}
