package io.github.kineks.neteaseviewer

import android.annotation.SuppressLint
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainAppState @OptIn(ExperimentalPagerApi::class) constructor(
    // For Snackbar
    val scope: CoroutineScope,
    val scaffoldState: ScaffoldState,

    // BottomBar
    var selectedItem: MutableState<Int>,
    val navItemList: List<String>,
    val navItemIconList: List<ImageVector>,
    val state: PagerState,
    val coroutineScope: CoroutineScope,

    // use UI Controller in compose
    val systemUiController: SystemUiController,
    val backgroundColor: Color

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

    @SuppressLint("ComposableNaming")
    fun setSystemBarColor(backgroundColor: Color = this.backgroundColor): MainAppState {
        systemUiController.setStatusBarColor(backgroundColor)
        systemUiController.setNavigationBarColor(backgroundColor)
        return this
    }

    @OptIn(ExperimentalPagerApi::class)
    fun scrollToPage(index: Int) {
        selectedItem.value = index
        coroutineScope.launch {
            state.scrollToPage(index)
        }
    }

}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun rememberMainAppState(
    scope: CoroutineScope = rememberCoroutineScope(),
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    selectedItem: MutableState<Int> = mutableStateOf(0),
    navItemList: List<String> = listOf("home", "play", "setting"),
    navItemIconList: List<ImageVector> =
        listOf(Icons.Outlined.Home, Icons.Outlined.MusicNote, Icons.Outlined.Settings),
    state: PagerState = rememberPagerState(initialPage = 0),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    systemUiController: SystemUiController = rememberSystemUiController(),
    backgroundColor: Color = MaterialTheme.colors.background
) = remember {
    MainAppState(
        scope, scaffoldState,
        selectedItem, navItemList, navItemIconList, state, coroutineScope,
        systemUiController, backgroundColor
    )
}