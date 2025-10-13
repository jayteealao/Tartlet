# Tartlet

[![Maven Central](https://img.shields.io/maven-central/v/io.yumemi/tartlet)](https://central.sonatype.com/artifact/io.yumemi/tartlet)
![License](https://img.shields.io/github/license/yumemi-inc/Tartlet)
[![Java CI with Gradle](https://github.com/yumemi-inc/Tartlet/actions/workflows/gradle.yml/badge.svg)](https://github.com/yumemi-inc/Tartlet/actions/workflows/gradle.yml)

Tartlet is a helper library for Compose Multiplatform.

Key benefits:
- **Eliminate callback hoisting**: Pass the *Store* to child composables, eliminating the need to hoist click events and other callbacks to parent cmposables
- **Simplified preview development**: Develop UI with Android Studio previews by creating *Store* instances with only *UiState*, without requiring ViewModels

## Installation

```kt
implementation("io.yumemi:tartlet:<latest-release>")
```

## Basic usage

### UiState

Marker interface for UI state representations. Implement this interface for your state objects:

```kotlin
data class CounterState(val count: Int) : UiState
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

    private val _uiEvent = MutableSharedFlow<CounterEvent>()
    override val uiEvent = _uiEvent.asSharedFlow()

    fun increment() {
        _uiState.update { it.copy(count = it.count + 1) }
    }

    fun decrement() {
        if (0 <= _uiState.value.count) {
            _uiState.update { it.copy(count = it.count - 1) }
        } else {
            viewModelScope.launch { _uiEvent.emit(CounterEvent.ShowToast("Can not Decrement.")) }            
        }
    }
}
```

### Store

A container for UI state that provides methods to render state values, execute actions, and handle events:

```kotlin
@Composable
fun CounterScreen(
    store: Store<CounterViewModel, CounterState, CounterEvent> = rememberStore(viewModel()),
) {
    Column {
        Text("Count: ${store.uiState.count}")

        Button(onClick = { store.action { increment() } }) { // Call ViewModel method
            Text("Increment")
        }
    }

    store.handle<CounterEvent.ShowToast> { event ->
        // Show toast
    }
}
```

## Cases where there are no events to handle

Specify `Nothing` for *UiEvent*.

```kt
class CounterViewModel : ViewModel(), StoreContract<CounterState, Nothing> {
    private val _uiState = MutableStateFlow<CounterState>(CounterState(count = 0))
    override val uiState = _uiState.asStateFlow()

    fun increment() { ... }
    fun decrement() { ... }
}
```

## Rendering Multiple States

When using sealed interfaces for multiple states, use `Store.render()` to render different UI based on the current state type:

```kotlin
sealed interface CounterState : UiState {
    data object Loading : CounterState
    data class Stable(val count: Int) : CounterState
    data class Error(val message: String) : CounterState
}

@Composable
fun CounterScreen(
    store: Store<CounterViewModel, CounterState, Nothing> = rememberStore(viewModel()),
) {
    store.render<CounterState.Loading> {
        CircularProgressIndicator()
    }

    store.render<CounterState.Stable> {
        Column {
            Text("Count: ${uiState.count}") // UiState is casted to CounterState.Stable
            Button(onClick = { action { increment() } }) {
                Text("Increment")
            }
        }
    }

    store.render<CounterState.Error> {
        Text("Error: ${uiState.message}", color = Color.Red) // UiState is casted to CounterState.Error
    }
}
```

You can extract a state's UI into a separate composable function by passing the Store. This eliminates the need to hoist click events and other callbacks to the parent:

```kt
@Composable
fun CounterScreen(
    store: Store<CounterViewModel, CounterState, Nothing> = rememberStore(viewModel()),
) {
    store.render<CounterState.Loading> {
        // ...
    }

    store.render<CounterState.Stable> {
        StableCounterContent(store = this) // Pass the Store to child composable
    }

    store.render<CounterState.Error> {
        // ...
    }
}

@Composable
private fun StableCounterContent(
    store: Store<CounterViewModel, CounterState.Stable, Nothing> // UiState is casted to CounterState.Stable
) {
    Column {
        Text("Count: ${store.uiState.count}")
        Button(onClick = { store.action { increment() } }) { // No need to hoist click　events to parent
            Text("Increment")
        }
    }
}
```

## Handling Multiple Events

You can handle the parent event type and use `when` expressions to process each event type:

```kt
sealed interface CounterEvent : UiEvent {
    data class ShowToast(val message: String) : CounterEvent
    data class NavigateToDetail(val id: Int) : CounterEvent
    data object Refresh : CounterEvent
}

@Composable
fun CounterScreen(
    store: Store<CounterViewModel, CounterState, CounterEvent> = rememberStore(viewModel()),
) {
    // ...

    store.handle<CounterEvent> { event ->
        when (event) {
            is CounterEvent.ShowToast -> {
                // Show toast with event.message
            }
            is CounterEvent.NavigateToDetail -> {
                // Navigate to detail screen with event.id
            }
            is CounterEvent.Refresh -> {
                // Refresh the screen
            }
        }
    }
}
```

## Mock for previewing in Android Studio

Create an instance of `Store` directly with the target *UiState*.

```kt
@Preview
@Composable
fun CounterScreenLoadingPreview() {
    MyApplicationTheme {
        CounterScreen(
            store = Store(
                uiState = CounterState.Loading,
            ),
        )
    }
}
```

Therefore, if you prepare only the *UiState*, it is possible to develop the UI.

## Mock a ViewModel for testing

Make ViewModel methods an interface and replace them with mocks during testing.

```kt
interface CounterStoreContract : StoreContract<CounterState, Nothing> {
    fun increment()
    fun decrement()
}

class MainViewModel : ViewModel(), CounterStoreContract {
    private val _uiState = MutableStateFlow<CounterState>(CounterState(count = 0))
    override val uiState = _uiState.asStateFlow()

    override fun increment() { ... }
    override fun decrement() { ... }
}

@Composable
fun CounterScreen(
    store: Store<CounterStoreContract, CounterState, CounterEvent> = rememberStore(viewModel<CounterViewModel>()),
) {
    // ...
}
```
