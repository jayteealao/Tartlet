# Tartlet

[![Maven Central](https://img.shields.io/maven-central/v/io.yumemi/tartlet)](https://central.sonatype.com/artifact/io.yumemi/tartlet)
![License](https://img.shields.io/github/license/yumemi-inc/Tartlet)
[![Java CI with Gradle](https://github.com/yumemi-inc/Tartlet/actions/workflows/gradle.yml/badge.svg)](https://github.com/yumemi-inc/Tartlet/actions/workflows/gradle.yml)

Tartlet is a helper library for Compose Multiplatform.

Key benefits:
- **Eliminate callback hoisting**: Pass the *ViewStore* to child Composables, eliminating the need to hoist click events and other callbacks to parent Composables
- **Simplified preview development**: Develop UI with Android Studio previews by creating *ViewStore* instances with only state, without requiring ViewModels

## Installation

```kt
implementation("io.yumemi:tartlet:<latest-release>")
```

## Basic usage

### Define state

Define a data class to represent your UI state:

```kotlin
data class CounterState(val count: Int)
```

### Define event

Define a sealed interface for one-time UI events:

```kotlin
sealed interface CounterEvent {
    data class ShowToast(val message: String) : CounterEvent
}
```

### Store

Typically implemented by a ViewModel:

```kotlin
class CounterViewModel : ViewModel(), Store<CounterState, CounterEvent> {
    private val _state = MutableStateFlow<CounterState>(CounterState(count = 0))
    override val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<CounterEvent>()
    override val event = _event.asSharedFlow()

    fun increment() {
        _state.update { it.copy(count = it.count + 1) }
    }

    fun decrement() {
        if (0 <= _state.value.count) {
            _state.update { it.copy(count = it.count - 1) }
        } else {
            viewModelScope.launch { _event.emit(CounterEvent.ShowToast("Can not Decrement.")) }
        }
    }
}
```

### ViewStore

A container for UI state that provides methods to render state values, execute actions, and handle events:

```kotlin
@Composable
fun CounterScreen(
    viewStore: ViewStore<CounterViewModel, CounterState, CounterEvent> = rememberViewStore { viewModel() },
) {
    Column {
        Text("Count: ${viewStore.state.count}")

        Button(onClick = { viewStore.action { increment() } }) { // Call ViewModel method
            Text("Increment")
        }
    }

    viewStore.handle<CounterEvent.ShowToast> { event ->
        // Show toast
    }
}
```

## Cases where there are no events to handle

Specify `Nothing` for the event type.

```kt
class CounterViewModel : ViewModel(), Store<CounterState, Nothing> {
    private val _state = MutableStateFlow<CounterState>(CounterState(count = 0))
    override val state = _state.asStateFlow()

    fun increment() { ... }
    fun decrement() { ... }
}
```

## Rendering multiple states

When using sealed interfaces for multiple states, use `ViewStore.render()` to render different UI based on the current state type:

```kotlin
sealed interface CounterState {
    data object Loading : CounterState
    data class Stable(val count: Int) : CounterState
    data class Error(val message: String) : CounterState
}

@Composable
fun CounterScreen(
    viewStore: ViewStore<CounterViewModel, CounterState, Nothing> = rememberViewStore { viewModel() },
) {
    viewStore.render<CounterState.Loading> {
        CircularProgressIndicator()
    }

    viewStore.render<CounterState.Stable> {
        Column {
            Text("Count: ${state.count}") // state is casted to CounterState.Stable
            Button(onClick = { action { increment() } }) {
                Text("Increment")
            }
        }
    }

    viewStore.render<CounterState.Error> {
        Text("Error: ${state.message}", color = Color.Red) // state is casted to CounterState.Error
    }
}
```

You can extract a state's UI into a separate composable function by passing the ViewStore. This eliminates the need to hoist click events and other callbacks to the parent:

```kt
@Composable
fun CounterScreen(
    viewStore: ViewStore<CounterViewModel, CounterState, Nothing> = rememberViewStore { viewModel() },
) {
    viewStore.render<CounterState.Loading> {
        // ...
    }

    viewStore.render<CounterState.Stable> {
        StableCounterContent(viewStore = this) // Pass the ViewStore to child composable
    }

    viewStore.render<CounterState.Error> {
        // ...
    }
}

@Composable
private fun StableCounterContent(
    viewStore: ViewStore<CounterViewModel, CounterState.Stable, Nothing> // state is casted to CounterState.Stable
) {
    Column {
        Text("Count: ${viewStore.state.count}")
        Button(onClick = { viewStore.action { increment() } }) { // No need to hoist click events to parent
            Text("Increment")
        }
    }
}
```

## Handling multiple events

You can handle the parent event type and use `when` expressions to process each event type:

```kt
sealed interface CounterEvent {
    data class ShowToast(val message: String) : CounterEvent
    data class NavigateToDetail(val id: Int) : CounterEvent
    data object Refresh : CounterEvent
}

@Composable
fun CounterScreen(
    viewStore: ViewStore<CounterViewModel, CounterState, CounterEvent> = rememberViewStore { viewModel() },
) {
    // ...

    viewStore.handle<CounterEvent> { event ->
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

Create an instance of `ViewStore` directly with the target state.

```kt
@Preview
@Composable
fun CounterScreenLoadingPreview() {
    MyApplicationTheme {
        CounterScreen(
            viewStore = ViewStore {
                CounterState.Loading
            },
        )
    }
}
```

Therefore, if you prepare only the state, it is possible to develop the UI.

## Mock a ViewModel for testing

Make ViewModel methods an interface and replace them with mocks during testing.

```kt
interface CounterStore : Store<CounterState, Nothing> {
    fun increment()
    fun decrement()
}

class MainViewModel : ViewModel(), CounterStore {
    private val _state = MutableStateFlow<CounterState>(CounterState(count = 0))
    override val state = _state.asStateFlow()

    override fun increment() { ... }
    override fun decrement() { ... }
}

@Composable
fun CounterScreen(
    viewStore: ViewStore<CounterStore, CounterState, CounterEvent> = rememberViewStore { viewModel<CounterViewModel>() },
) {
    // ...
}
```
