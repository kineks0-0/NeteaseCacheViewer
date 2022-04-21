package io.github.kineks.neteaseviewer.ui.home

import android.annotation.SuppressLint
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WelcomeAppState @OptIn(ExperimentalPagerApi::class) constructor(
    // For Snackbar
    val scope: CoroutineScope,
    val scaffoldState: ScaffoldState,

    // Pager
    val pagerState: PagerState,
    val coroutineScope: CoroutineScope,

    // use UI Controller in compose
    val systemUiController: SystemUiController
) {

    @OptIn(ExperimentalPagerApi::class)
    fun animateScrollToPage(index: Int, delay: Long = 0) {
        coroutineScope.launch {
            if (delay > 0)
                delay(delay)
            pagerState.animateScrollToPage(index)
        }
    }

    @OptIn(ExperimentalPagerApi::class)
    fun animateScrollToNextPage(delay: Long = 0) =
        animateScrollToPage(pagerState.currentPage + 1, delay)

    fun snackbar(message: String, actionLabel: String, ActionPerformed: () -> Unit) {
        scope.launch {
            scaffoldState.snackbarHostState
                .currentSnackbarData?.dismiss()
            val result = scaffoldState.snackbarHostState
                .showSnackbar(
                    message = message,
                    actionLabel = actionLabel
                )
            if (result == SnackbarResult.ActionPerformed) {
                ActionPerformed()
            }
        }
    }

    @SuppressLint("ComposableNaming")
    @Composable
    fun setSystemBarColor(backgroundColor: Color = MaterialTheme.colors.background) {
        systemUiController.setStatusBarColor(backgroundColor)
        systemUiController.setNavigationBarColor(backgroundColor)
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun rememberWelcomeAppState(
    scope: CoroutineScope = rememberCoroutineScope(),
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    pagerState: PagerState = rememberPagerState(initialPage = 0),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    systemUiController: SystemUiController = rememberSystemUiController()
) = remember {
    WelcomeAppState(scope, scaffoldState, pagerState, coroutineScope, systemUiController)
}