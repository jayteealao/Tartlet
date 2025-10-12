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
data class CounterState(val count: Int = 0) : UiState
```

### UiEvent

Marker interface for one-time UI events. Implement this interface for events that should be consumed by the UI:

```kotlin
sealed interface CounterEvent : UiEvent {
    data object ShowToast : CounterEvent
}
```

### StoreContract

Typically implemented by a ViewModel:

```kotlin
class CounterViewModel : ViewModel(), toreContract<CounterState, CounterEvent> {
    private val _uiState: MutableStateFlow<CounterState> = MutableStateFlow(CounterState())
    override val uiState: StateFlow<CounterState> = _uiState.asStateFlow()

    fun increment() {
        _uiState.update { it.copy(count = it.count + 1) }
    }

    fun decrement() {
        _uiState.update { it.copy(count = it.count - 1) }
    }
}
```

### Store

A container for UI state that provides methods to execute actions, render specific states, and handle events:

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

## Mock for preview on Android Studio

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
