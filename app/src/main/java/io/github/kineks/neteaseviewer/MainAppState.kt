package io.github.kineks.neteaseviewer

import android.annotation.SuppressLint
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
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

    // BottomBar & Pager
    var selectedItem: MutableState<Int>,
    val navItemList: List<String>,
    val state: PagerState,
    val coroutineScope: CoroutineScope,

    // use UI Controller in compose
    val systemUiController: SystemUiController

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

    @SuppressLint("ComposableNaming")
    @Composable
    fun setSystemBarColor(backgroundColor: Color = MaterialTheme.colors.background) {
        systemUiController.setStatusBarColor(MaterialTheme.colors.background)
        systemUiController.setNavigationBarColor(MaterialTheme.colors.background)
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
    state: PagerState = rememberPagerState(initialPage = 0),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    systemUiController: SystemUiController = rememberSystemUiController()
) = remember {
    MainAppState(
        scope, scaffoldState, selectedItem,
        navItemList, state, coroutineScope,
        systemUiController
    )
}