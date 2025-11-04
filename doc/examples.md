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

## Shared Element Transition

Using `tartlet` with Compose's Shared Element Transition is straightforward. The state managed by the `Store` determines which screen or composable is currently visible, and `AnimatedContent` handles the transition between them. `SharedTransitionLayout` enables elements to animate smoothly across these different screens.

Here's how you can structure it:

```kotlin
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.yumemi.tartlet.Store
import io.yumemi.tartlet.rememberViewStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// 1. Define the state for different screens
sealed interface ScreenState {
    object List : ScreenState
    object Detail : ScreenState
}

data class SharedTransitionExampleState(val currentScreen: ScreenState = ScreenState.List)

// 2. Create a store to manage screen state
class SharedTransitionStore : Store<SharedTransitionExampleState, Nothing> {
    private val _state = MutableStateFlow(SharedTransitionExampleState())
    override val state: StateFlow<SharedTransitionExampleState> = _state

    fun showDetail() {
        _state.value = _state.value.copy(currentScreen = ScreenState.Detail)
    }

    fun showList() {
        _state.value = _state.value.copy(currentScreen = ScreenState.List)
    }
}

// 3. Use in a composable with SharedTransitionLayout
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionExampleScreen() {
    val store = remember { SharedTransitionStore() }
    val viewStore = rememberViewStore { store }

    SharedTransitionLayout {
        // AnimatedContent switches UI based on tartlet's state
        AnimatedContent(
            targetState = viewStore.state.currentScreen,
            label = "screen-transition"
        ) { targetScreen ->
            when (targetScreen) {
                is ScreenState.List -> {
                    // Pass the scope and actions to the specific screen
                    ListScreen(
                        animatedVisibilityScope = this,
                        onShowDetail = { viewStore.action { showDetail() } }
                    )
                }
                is ScreenState.Detail -> {
                    DetailScreen(
                        animatedVisibilityScope = this,
                        onShowList = { viewStore.action { showList() } }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ListScreen(
    animatedVisibilityScope: AnimatedVisibilityScope,
    onShowDetail: () -> Unit
) {
    Column(modifier = Modifier.clickable(onClick = onShowDetail)) {
        // This Icon will be shared
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Profile",
            modifier = Modifier.sharedElement(
                state = rememberSharedContentState(key = "profile-icon"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        )
        Text("Go to Detail")
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun DetailScreen(
    animatedVisibilityScope: AnimatedVisibilityScope,
    onShowList: () -> Unit
) {
    Column(modifier = Modifier.clickable(onClick = onShowList)) {
        // This is the destination for the shared Icon
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Profile",
            modifier = Modifier.sharedElement(
                state = rememberSharedContentState(key = "profile-icon"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        )
        Text("This is the Detail Screen. Go Back.")
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
