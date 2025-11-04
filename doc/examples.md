# Tartlet Examples

## AnimatedVisibility

Here's an example of how to use `tartlet` with `AnimatedVisibility`.

```kotlin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import io.yumemi.tartlet.Store
import io.yumemi.tartlet.rememberViewStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// 1. Define the state
data class MyState(val isVisible: Boolean = true)

// 2. Create a store
class MyStore : Store<MyState, Nothing> {
    private val _state = MutableStateFlow(MyState())
    override val state: StateFlow<MyState> = _state

    fun toggleVisibility() {
        _state.value = _state.value.copy(isVisible = !_state.value.isVisible)
    }
}

// 3. Use in a composable
@Composable
fun MyScreen() {
    val store = remember { MyStore() }
    val viewStore = rememberViewStore { store }

    Column {
        Button(onClick = { viewStore.action { toggleVisibility() } }) {
            Text("Toggle Visibility")
        }

        AnimatedVisibility(visible = viewStore.state.isVisible) {
            Text("Hello, World!")
        }
    }
}
```

## AnimatedContent

Here's an example of how to use `tartlet` with `AnimatedContent`.

```kotlin
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import io.yumemi.tartlet.Store
import io.yumemi.tartlet.rememberViewStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// 1. Define the state
sealed class ContentState {
    object Loading : ContentState()
    data class Loaded(val data: String) : ContentState()
}

data class AnimatedContentState(val content: ContentState = ContentState.Loading)

// 2. Create a store
class AnimatedContentStore : Store<AnimatedContentState, Nothing> {
    private val _state = MutableStateFlow(AnimatedContentState())
    override val state: StateFlow<AnimatedContentState> = _state

    fun loadContent() {
        _state.value = _state.value.copy(content = ContentState.Loaded("Hello, AnimatedContent!"))
    }
}

// 3. Use in a composable
@Composable
fun AnimatedContentScreen() {
    val store = remember { AnimatedContentStore() }
    val viewStore = rememberViewStore { store }

    Column {
        Button(onClick = { viewStore.action { loadContent() } }) {
            Text("Load Content")
        }

        AnimatedContent(targetState = viewStore.state.content) { targetState ->
            when (targetState) {
                is ContentState.Loading -> Text("Loading...")
                is ContentState.Loaded -> Text(targetState.data)
            }
        }
    }
}
```
