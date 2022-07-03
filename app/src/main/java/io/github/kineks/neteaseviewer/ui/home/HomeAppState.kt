package io.github.kineks.neteaseviewer.ui.home

import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.google.accompanist.swiperefresh.SwipeRefreshState
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.github.kineks.neteaseviewer.R
import io.github.kineks.neteaseviewer.getString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class HomeAppState(

    // For Snackbar
    private val scope: CoroutineScope,
    private val scaffoldState: ScaffoldState,

    val refreshState: SwipeRefreshState
) {
    val snackbar: (message: String) -> Unit = {
        scope.launch {
            scaffoldState.snackbarHostState
                .currentSnackbarData?.dismiss()
            scaffoldState.snackbarHostState
                .showSnackbar(
                    message = it,
                    actionLabel = getString(R.string.snackbar_dismissed),
                    duration = SnackbarDuration.Short
                )
        }
    }

    val snackbarLong: (message: String) -> Unit = { message ->
        scope.launch {
            scaffoldState.snackbarHostState
                .currentSnackbarData?.dismiss()
            scaffoldState.snackbarHostState
                .showSnackbar(
                    message = message,
                    actionLabel = getString(R.string.snackbar_dismissed),
                    duration = SnackbarDuration.Long
                )
        }
    }

    val snackbarIndefinite: (message: String) -> Unit = { message ->
        scope.launch {
            scaffoldState.snackbarHostState
                .currentSnackbarData?.dismiss()
            scaffoldState.snackbarHostState
                .showSnackbar(
                    message = message,
                    actionLabel = getString(R.string.snackbar_dismissed),
                    duration = SnackbarDuration.Indefinite
                )
        }
    }

}

@Composable
fun rememberHomeAppState(
    scope: CoroutineScope = rememberCoroutineScope(),
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    refreshState: SwipeRefreshState = rememberSwipeRefreshState(false)
) = remember {
    HomeAppState(
        scope, scaffoldState, refreshState
    )
}