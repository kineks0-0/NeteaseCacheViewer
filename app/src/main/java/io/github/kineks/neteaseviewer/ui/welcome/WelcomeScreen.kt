package io.github.kineks.neteaseviewer.ui.welcome

import android.annotation.SuppressLint
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import io.github.kineks.neteaseviewer.App
import io.github.kineks.neteaseviewer.R
import io.github.kineks.neteaseviewer.data.local.FileUriUtils
import io.github.kineks.neteaseviewer.data.local.NeteaseCacheProvider
import io.github.kineks.neteaseviewer.data.local.RFile
import io.github.kineks.neteaseviewer.data.local.RFileAndroidData
import io.github.kineks.neteaseviewer.data.local.RFileAndroidData.Companion.getNewDocumentFile
import io.github.kineks.neteaseviewer.getString
import io.github.kineks.neteaseviewer.openBrowser
import io.github.kineks.neteaseviewer.ui.theme.NeteaseViewerTheme
import io.github.kineks.neteaseviewer.ui.view.PermissionComposeX

@OptIn(ExperimentalPagerApi::class)
@Composable
fun WelcomeScreen(
    callback: () -> Unit,
    checkPermission: @Composable (checkPermissionCallback: (allGranted: Boolean) -> Unit) -> Unit,
    display: Boolean = true
) {
    if (display) {

        val appState = rememberWelcomeAppState()

        // 点击下一步
        val onNextClick: () -> Unit = {
            when (appState.pagerState.currentPage) {
                0 -> {
                    if (!appState.agreeAgreement)
                        appState.needAgreeAgreementToast()
                    else
                        appState.nextPage()
                }

                appState.pagerState.pageCount - 1 -> {
                    if (!appState.agreeAgreement) {
                        appState.needAgreeAgreementToast()
                        appState.animateScrollToPage(0, 600)
                    } else {
                        // 完成用户引导页时的回调
                        callback()
                    }

                }

                else -> appState.nextPage()
            }
        }

        NeteaseViewerTheme {

            appState.setSystemBarColor()

            Scaffold(
                scaffoldState = appState.scaffoldState,
                /*floatingActionButton = {
                    ExtendedFloatingActionButton(
                        text = { Text(text = appState.floatingActionText) }, onClick = onNextClick
                    )
                }*/
            ) { paddingValues ->
                HorizontalPager(
                    state = appState.pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colors.background.copy(0.1f))
                        .padding(paddingValues),
                    count = 2,
                    userScrollEnabled = appState.agreeAgreement
                ) { page ->

                    LaunchedEffect(appState.pagerState.currentPage) {
                        appState.floatingActionText = when (appState.pagerState.currentPage) {
                            appState.pagerState.pageCount - 1 -> getString(R.string.welcome_finish)
                            else -> getString(R.string.welcome_next)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.75f),
                        contentAlignment = Alignment.Center
                    ) {
                        when (page) {
                            0 -> PageOne(
                                onNextClick = onNextClick,
                                nextPage = appState.nextPage,
                                agreeAgreement = appState.agreeAgreement,
                                onValueChange = { appState.agreeAgreement = it }
                            )

                            1 -> PageTwo(
                                checkPermission = checkPermission,
                                onNextClick = onNextClick
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


@SuppressLint("NewApi")
@Composable
fun PageTwo(
    checkPermission: @Composable (checkPermissionCallback: (allGranted: Boolean) -> Unit) -> Unit,
    onNextClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        //val context = LocalContext.current

        var allGranted by remember {
            mutableStateOf(false)
        }
        var title by remember {
            mutableStateOf("")
        }.apply {
            this.value =
                stringResource(R.string.welcome_check_permissions)
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
            if (allGranted) {
                val list by remember {
                    mutableStateOf(NeteaseCacheProvider.cacheDir)
                }
                LazyColumn {
                    list.forEachIndexed { index, neteaseAppCache ->
                        item {
                            var isLoading by remember {
                                mutableStateOf(true)
                            }
                            var text by remember {
                                mutableStateOf("size: N/A")
                            }
                            Row(horizontalArrangement = Arrangement.Center) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .padding(bottom = 4.dp)
                                ) {
                                    Image(
                                        contentDescription = "AppIcon",
                                        painter = rememberAsyncImagePainter(
                                            R.mipmap.ic_launcher
                                        ),
                                        modifier = Modifier
                                            .background(Color.Transparent)
                                            .size(40.dp)
                                    )
                                }
                                Column(
                                    modifier = Modifier.padding(
                                        top = 10.dp,
                                        end = 4.dp,
                                        start = 4.dp
                                    ),
                                    verticalArrangement = Arrangement.Center
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
                                    NeteaseCacheProvider.getCacheFiles(neteaseAppCache).size
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

        if (App.isAndroidRorAbove && !FileUriUtils.isGrant && allGranted)
            InfoCard(
                shape = MaterialTheme.shapes.small,
                elevation = 10.dp,
                modifier = Modifier.padding(top = 16.dp),
                boxModifier = Modifier.padding(vertical = 15.dp),
                boxAlignment = Alignment.Center
            ) {
                PermissionComposeX(
                    callback = { },
                    permissions = listOf("RFile"),
                    request = { _: List<String>, _: (Boolean) -> Unit ->
                        FileUriUtils.startForRoot {
                            NeteaseCacheProvider.cacheDir.forEach { neteaseAppCache ->
                                neteaseAppCache.rFiles.forEach { rFile ->
                                    if (rFile.type == RFile.RType.AndroidData) {
                                        val file = rFile as RFileAndroidData
                                        file.documentFile =
                                            getNewDocumentFile(file.path, file.type)
                                    }
                                }
                            }
                        }
                    },
                    requestDescription = "/Android/Data/无法访问，使用重定向存储和修改版可能无法识别到"
                )
            }

        if (!allGranted)
            InfoCard(
                shape = MaterialTheme.shapes.small,
                elevation = 10.dp,
                modifier = Modifier.padding(top = 16.dp),
                boxModifier = Modifier.padding(vertical = 15.dp),
                boxAlignment = Alignment.Center
            ) {
                checkPermission {
                    allGranted = it
                    title = if (allGranted)
                        getString(R.string.welcome_loading)
                    else
                        getString(R.string.welcome_permission_denied)
                }
            }


        Button(
            onClick = onNextClick,
            modifier = Modifier.padding(top = 50.dp)
        ) { Text(text = "完成") }


    }
}


@Composable
fun PageOne(
    onNextClick: () -> Unit,
    nextPage: () -> Unit,
    agreeAgreement: Boolean,
    onValueChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        InfoCard(
            shape = MaterialTheme.shapes.small,
            elevation = 0.dp,
            backgroundColor = Color.Transparent,
            boxModifier = Modifier.padding(bottom = 0.dp),
            boxAlignment = Alignment.TopCenter
        ) {

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = Color.Transparent,
                    elevation = 12.dp
                ) {
                    Image(
                        contentDescription = "AppIcon",
                        painter = rememberAsyncImagePainter(R.mipmap.ic_launcher),
                        modifier = Modifier.size(80.dp)
                    )
                }

                Text(
                    text = "欢迎使用 NeteaseViewer",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 20.dp)
                )

                Text(
                    text = "一个用来管理网易云音乐缓存的工具",
                    style = MaterialTheme.typography.subtitle2,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Text(
                    text = "该应用仅用于提供学习 Compose 的示例,\n并无修改或破坏其他应用的能力",
                    style = MaterialTheme.typography.subtitle2,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(
                            top = 20.dp,
                            bottom = 20.dp,
                            start = 10.dp
                        )
                        .alpha(0.5f)
                )

                /*Text(
                    text = "请阅读后再同意该应用的隐私协议",
                    style = MaterialTheme.typography.subtitle2,
                    modifier = Modifier.padding(
                        top = 15.dp,
                        bottom = 0.dp,
                        start = 2.dp
                    ).alpha(0.9f)
                )*/
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
                            annotation = "https://github.com/kineks0-0/NeteaseCacheViewer/blob/dev/README.md#-%E9%9A%90%E7%A7%81%E5%8D%8F%E8%AE%AE"
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
                        pop()

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
                    if (!agreeAgreement) onNextClick.invoke() else nextPage.invoke()
                }) { Text(text = "我同意") }
            }

        }
    }
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    boxModifier: Modifier = Modifier,
    boxAlignment: Alignment = Alignment.TopStart,
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
                .fillMaxWidth(),
            contentAlignment = boxAlignment
        ) {
            content.invoke()
        }
    }

}

@Composable
@Preview
fun PageOnePreview() {
    var agreeAgreement by remember {
        mutableStateOf(false)
    }
    PageOne(
        onNextClick = { },
        nextPage = { },
        agreeAgreement = agreeAgreement,
        onValueChange = { agreeAgreement = it }
    )
}

@Composable
@Preview
fun PageTwoPreview() {
    PageTwo({}, {})
}