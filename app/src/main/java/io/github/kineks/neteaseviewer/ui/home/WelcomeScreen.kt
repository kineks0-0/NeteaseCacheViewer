package io.github.kineks.neteaseviewer.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import io.github.kineks.neteaseviewer.R
import io.github.kineks.neteaseviewer.data.local.NeteaseCacheProvider
import io.github.kineks.neteaseviewer.openBrowser
import io.github.kineks.neteaseviewer.ui.theme.NeteaseViewerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun WelcomeScreen(
    callback: () -> Unit,
    checkPermission: (checkPermissionCallback: (allGranted: Boolean) -> Unit) -> Unit,
    display: Boolean = true
) {
    if (display) {

        // For Snackbar
        val scope = rememberCoroutineScope()
        val scaffoldState = rememberScaffoldState()

        // Pager
        val pagerState = rememberPagerState(initialPage = 0)
        val coroutineScope = rememberCoroutineScope()

        // use UI Controller in compose
        val systemUiController = rememberSystemUiController()

        // 是否同意隐私协议
        var agreeAgreement by remember {
            mutableStateOf(false)
        }

        // 设置 fab 的文本
        var floatingActionText by remember {
            mutableStateOf("下一步")
        }

        // 跳转下一页
        val nextPage: () -> Unit = {
            coroutineScope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }

        // 弹出需要同意隐私协议的吐司
        val needAgreeAgreementToast: () -> Unit = {
            scope.launch {
                scaffoldState.snackbarHostState
                    .currentSnackbarData?.dismiss()
                val result = scaffoldState.snackbarHostState
                    .showSnackbar("需要同意隐私协议才能继续", actionLabel = "同意")
                if (result == SnackbarResult.ActionPerformed) {
                    agreeAgreement = true
                    nextPage.invoke()
                }
            }
        }

        // 点击下一步
        val onNextClick: () -> Unit = {
            when (pagerState.currentPage) {
                0 -> {
                    if (!agreeAgreement) {
                        needAgreeAgreementToast.invoke()
                    } else nextPage.invoke()
                }
                pagerState.pageCount - 1 -> {
                    if (!agreeAgreement) {
                        needAgreeAgreementToast.invoke()
                        coroutineScope.launch {
                            delay(600)
                            pagerState
                                .animateScrollToPage(0)
                        }
                    } else {
                        callback.invoke()
                    }

                }
                else -> nextPage.invoke()
            }
        }

        NeteaseViewerTheme {

            systemUiController.setStatusBarColor(MaterialTheme.colors.background)
            systemUiController.setNavigationBarColor(MaterialTheme.colors.background)

            Scaffold(
                scaffoldState = scaffoldState,
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        text = { Text(text = floatingActionText) }, onClick = onNextClick
                    )
                }
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colors.background.copy(0.1f))
                        .padding(it),
                    count = 2
                ) { page ->

                    LaunchedEffect(pagerState.currentPage) {
                        floatingActionText = when (pagerState.currentPage) {
                            pagerState.pageCount - 1 -> "完成"
                            else -> "下一步"
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(0.75f),
                        contentAlignment = Alignment.Center
                    ) {
                        when (page) {
                            0 -> PageOne(
                                onNextClick = onNextClick,
                                nextPage = nextPage,
                                agreeAgreement = agreeAgreement,
                                onValueChange = { agreeAgreement = it }
                            )
                            1 -> PageTwo(
                                needAgreeAgreementToast = needAgreeAgreementToast,
                                checkPermission = checkPermission,
                                pagerState = pagerState,
                                coroutineScope = coroutineScope,
                                agreeAgreement = agreeAgreement
                            )
                        }


                    }

                }
            }


        }


    } else {
        callback.invoke()
    }


}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun PageTwo(
    needAgreeAgreementToast: () -> Unit,
    checkPermission: (checkPermissionCallback: (allGranted: Boolean) -> Unit) -> Unit,
    pagerState: PagerState,
    coroutineScope: CoroutineScope,
    agreeAgreement: Boolean
) {
    Column {


        var toNext by remember {
            mutableStateOf(false)
        }
        var title by remember {
            mutableStateOf("检查权限中")
        }

        LaunchedEffect(pagerState.currentPage) {
            if (pagerState.currentPage != 1) return@LaunchedEffect
            if (!agreeAgreement) {
                needAgreeAgreementToast.invoke()
                coroutineScope.launch {
                    delay(600)
                    pagerState.animateScrollToPage(0)
                }
            } else {
                checkPermission.invoke {
                    toNext = it
                    if (!it) title = "权限被拒"
                }
            }
        }

        InfoCard(
            shape = MaterialTheme.shapes.small,
            elevation = 10.dp
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(
                        top = 8.dp,
                        bottom = 4.dp
                    )
            )
        }

        InfoCard(
            shape = MaterialTheme.shapes.small,
            elevation = 10.dp,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            if (toNext) {
                LazyColumn {
                    NeteaseCacheProvider.cacheDir.forEachIndexed { index, neteaseAppCache ->
                        item {
                            var isLoading by remember {
                                mutableStateOf(true)
                            }
                            var text by remember {
                                mutableStateOf("size: N/A")
                            }
                            Row(horizontalArrangement = Arrangement.Center) {
                                Surface(shape = MaterialTheme.shapes.medium) {
                                    Image(
                                        contentDescription = "AppIcon",
                                        painter = rememberImagePainter(
                                            data = R.mipmap.ic_launcher
                                        ),
                                        modifier = Modifier
                                            .background(Color.Transparent)
                                            .padding(8.dp)
                                            .padding(bottom = 4.dp)
                                            .size(40.dp)
                                    )
                                }
                                Column(
                                    modifier = Modifier.padding(
                                        top = 6.dp,
                                        end = 4.dp,
                                        start = 4.dp
                                    )
                                ) {
                                    Text(
                                        text = neteaseAppCache.type,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = text
                                    )
                                }
                            }
                            if (isLoading) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .padding(
                                            top = 8.dp,
                                            bottom = 8.dp
                                        )
                                        .fillMaxWidth()
                                )
                            }
                            LaunchedEffect(Unit) {
                                val size =
                                    NeteaseCacheProvider.getCacheFiles(
                                        neteaseAppCache
                                    ).size
                                text = "Size: $size"
                                isLoading = false
                                if (index == NeteaseCacheProvider.cacheDir.lastIndex) {
                                    title = "数据检查完成"
                                }
                            }

                        }
                    }
                }
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .padding(
                            top = 8.dp,
                            bottom = 8.dp
                        )
                        .fillMaxWidth()
                )
            }

        }


    }
}

@Composable
fun PageOne(
    onNextClick: () -> Unit,
    nextPage: () -> Unit,
    agreeAgreement: Boolean,
    onValueChange: (Boolean) -> Unit
) {
    Column {
        InfoCard(
            shape = MaterialTheme.shapes.small,
            elevation = 10.dp,
            boxModifier = Modifier.padding(top = 6.dp)
        ) {
            Column {
                Row {
                    Surface(shape = MaterialTheme.shapes.medium) {
                        Image(
                            contentDescription = "AppIcon",
                            painter = rememberImagePainter(
                                data = R.mipmap.ic_launcher
                            ),
                            modifier = Modifier
                                .background(Color.Transparent)
                                .padding(8.dp)
                                .padding(bottom = 4.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.padding(
                            top = 12.dp,
                            end = 6.dp,
                            start = 6.dp
                        )
                    ) {
                        Text(
                            text = "欢迎使用 NeteaseViewer",
                            style = MaterialTheme.typography.body1,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "一个用来管理网易云音乐缓存的工具",
                            style = MaterialTheme.typography.subtitle2
                        )
                    }
                }

                Text(
                    text = "该应用仅用于提供学习 Compose 的示例,\n并无修改或破坏其他应用的能力",
                    style = MaterialTheme.typography.subtitle2
                )
            }
        }

        InfoCard(
            shape = MaterialTheme.shapes.small,
            elevation = 10.dp,
            modifier = Modifier.padding(top = 14.dp),
            boxModifier = Modifier.padding(top = 4.dp)
        ) {
            Column {
                Text(text = "请阅读后再同意该应用的隐私协议")
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onValueChange.invoke(!agreeAgreement) }
                ) {
                    Checkbox(
                        checked = agreeAgreement,
                        onCheckedChange = { onValueChange.invoke(it) })
                    val annotatedText = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = MaterialTheme.colors.onSurface)) {
                            append("我同意该")
                        }

                        pushStringAnnotation(
                            tag = "URL",
                            annotation = "https://github.com/kineks0-0/NeteaseCacheViewer/blob/dev/README.md#%E9%9A%90%E7%A7%81%E5%8D%8F%E8%AE%AE"
                        )
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colors.primary,
                                fontWeight = FontWeight.Medium,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append("隐私协议")
                        }

                        withStyle(style = SpanStyle(color = MaterialTheme.colors.onSurface)) {
                            append("并授权所需权限")
                        }
                    }

                    ClickableText(
                        text = annotatedText,
                        onClick = { offset ->
                            annotatedText.getStringAnnotations(
                                tag = "URL", start = offset, end = offset
                            ).firstOrNull()?.item?.openBrowser()
                        }
                    )

                }

                Button(onClick = {
                    if (!agreeAgreement)
                        onNextClick.invoke()
                    else
                        nextPage.invoke()
                }) {
                    Text(text = "我同意")
                }
            }
        }
    }
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    boxModifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    elevation: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    Card(
        shape = shape,
        elevation = elevation,
        modifier = modifier,
        contentColor = contentColor,
        backgroundColor = backgroundColor
    ) {
        Box(
            modifier = boxModifier
                .padding(17.dp)
                .fillMaxWidth()
        ) {
            content.invoke()
        }
    }

}

@Composable
@Preview
fun Preview() {
    WelcomeScreen(callback = { }, checkPermission = { }, display = true)
}