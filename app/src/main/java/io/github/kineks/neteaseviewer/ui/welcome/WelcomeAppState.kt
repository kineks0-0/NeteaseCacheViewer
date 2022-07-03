package io.github.kineks.neteaseviewer.ui.welcome

import android.annotation.SuppressLint
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import io.github.kineks.neteaseviewer.R
import io.github.kineks.neteaseviewer.getString
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

    // 跳转下一页
    val nextPage: () -> Unit = {
        animateScrollToNextPage()
    }

    // 是否同意隐私协议
    var agreeAgreement by mutableStateOf(false)


    // 设置 fab 的文本
    var floatingActionText by mutableStateOf(
        getString(id = R.string.welcome_next)
    )

    // 弹出需要同意隐私协议的吐司
    val needAgreeAgreementToast: () -> Unit = {
        snackbar(
            message = getString(R.string.welcome_need_agree_agreement),
            actionLabel = getString(R.string.welcome_agree),
            ActionPerformed = {
                agreeAgreement = true
                nextPage()
            }
        )
    }

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