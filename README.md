# Tartlet

[![Maven Central](https://img.shields.io/maven-central/v/io.yumemi/tartlet)](https://central.sonatype.com/artifact/io.yumemi/tartlet)
![License](https://img.shields.io/github/license/yumemi-inc/Tartlet)
[![Java CI with Gradle](https://github.com/yumemi-inc/Tartlet/actions/workflows/gradle.yml/badge.svg)](https://github.com/yumemi-inc/Tartlet/actions/workflows/gradle.yml)

Tartlet is a helper library for Compose Multiplatform.

## Installation

```kt
implementation("io.yumemi:tartlet:<latest-release>")
```

## Core Concepts

### UiState

Marker interface for UI state representations. Implement this interface for your state objects:

```kotlin
data class CounterState(val count: Int) : UiState

// If there are multiple States:
//
// sealed interface CounterState : UiState {
//     data object Loading : CounterState
//     data class Stable(val count: Int) : CounterState
//     data class Error(val message: String) : CounterState
// }
```

### UiEvent

Marker interface for one-time UI events. Implement this interface for events that should be consumed by the UI:

```kotlin
sealed interface CounterEvent : UiEvent {
    data class ShowToast(val message: String) : CounterEvent
}
```

### StoreContract

Typically implemented by a ViewModel:

```kotlin
class CounterViewModel : ViewModel(), StoreContract<CounterState, CounterEvent> {
    private val _uiState = MutableStateFlow<CounterState>(CounterState(count = 0))
    override val uiState = _uiState.asStateFlow()

    // Events are optional and do not need to be defined if not needed
    private val _uiEvent = MutableSharedFlow<CounterEvent>()
    override val uiEvent = _uiEvent.asSharedFlow()

    fun increment() {
        _uiState.update { it.copy(count = it.count + 1) }
    }

    fun decrement() {
        if (0 <= _uiState.value.count) {
            _uiState.update { it.copy(count = it.count - 1) }
        } esle {
            viewModelScope.launch { _uiEvent.emit(CounterEvent.ShowToast("Can not Decrement.")) }            
        }
    }
}
```

### Store

A container for UI state that provides methods to render state values, execute actions, and handle events:

```kotlin
@Composable
fun CounterScreen(viewModel: CounterViewModel = viewModel()) {
    val store = rememberStore(viewModel)

    Column {
        Text("Count: ${store.uiState.count}")

        Button(onClick = { store.action { increment() } }) {
            Text("Increment")
        }
    }

    store.handle<CounterEvent.ShowToast> { event ->
        // Show toast
    }
}
```

## Mock for previewing in Android Studio

Create an instance of `Store` directly with the target *UiState*.

```kt
@Preview
@Composable
fun LoadingPreview() {
    MyApplicationTheme {
        YourComposable(
            store = Store(
                uiState = CounterState.Loading,
            ),
        )
    }
}
```

Therefore, if you prepare only the *UiState*, it is possible to develop the UI.

## Mock a ViewModel for testing

Wrap the ViewModel methods in an interface.

```kt
interface CounterStoreContract : StoreContract<CounterState, Nothing> {
    fun increment()
    fun decrement()
}

class MainViewModel : ViewModel(), CounterStoreContract {
    private val _uiState = MutableStateFlow<CounterState>(CounterState(count = 0))
    override val uiState = _uiState.asStateFlow()

    override fun increment() {
        // ...
    }

    override fun decrement() {
        // ...
    }
}

// in Compose
val viewModel: CounterViewModel = viewModel()
val store = rememberStore<CounterStoreContract, CounterState, Nothing>(viewModel)
```
