package io.github.kineks.neteaseviewer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.permissionx.guolindev.PermissionX
import io.github.kineks.neteaseviewer.data.local.NeteaseCacheProvider
import io.github.kineks.neteaseviewer.data.local.Setting
import io.github.kineks.neteaseviewer.ui.home.HomeScreen
import io.github.kineks.neteaseviewer.ui.home.WelcomeScreen
import io.github.kineks.neteaseviewer.ui.home.working
import io.github.kineks.neteaseviewer.ui.play.PlayScreen
import io.github.kineks.neteaseviewer.ui.setting.SettingScreen
import io.github.kineks.neteaseviewer.ui.theme.NeteaseViewerTheme
import kotlinx.coroutines.launch


class MainActivity : FragmentActivity() {
    private val model: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        working = true
        setContent {
            if (model.displayWelcomeScreen) {
                WelcomeScreen(
                    callback = {
                        lifecycleScope.launchWhenCreated {
                            Setting.setFirstTimeLaunch(false)
                        }
                        model.displayWelcomeScreen = false
                    },
                    checkPermission = {
                        checkPermission { allGranted, _, _ -> it.invoke(allGranted) }
                    },
                    display = model.displayWelcomeScreen
                )
            } else DefaultView(model)
        }
    }

    @SuppressLint("InlinedApi")
    private fun checkPermission(
        callback: (
            allGranted: Boolean, grantedList: List<String>, deniedList: List<String>
        ) -> Unit
    ) {
        lifecycleScope.launchWhenResumed {
            // 检查权限, 如果已授权读写权限就初始化数据
            PermissionX.init(this@MainActivity)
                .run {
                    if (App.isAndroidRorAbove)
                        permissions(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.MANAGE_EXTERNAL_STORAGE
                        )
                    else
                        permissions(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                }
                .onExplainRequestReason { scope, deniedList ->
                    val message =
                        io.github.kineks.neteaseviewer.getString(R.string.permission_request_description)
                    scope.showRequestReasonDialog(
                        deniedList, message,
                        io.github.kineks.neteaseviewer.getString(R.string.permission_allow),
                        io.github.kineks.neteaseviewer.getString(R.string.permission_deny)
                    )
                }
                .request { allGranted, grantedList, deniedList ->
                    callback.invoke(allGranted, grantedList, deniedList)
                }
        }
    }

    override fun onStart() {
        super.onStart()
        // 避免在进入引导页时先申请权限
        lifecycleScope.launchWhenStarted {
            Setting.firstTimeLaunch.collect { firstTimeLaunch ->
                if (firstTimeLaunch) return@collect
                checkPermission { allGranted, _, _ ->
                    if (allGranted) {
                        // 注: 该函数仅在第一次调用会重新加载数据
                        // 重载数据请用 model.reload()
                        if (model.hadListInited) {
                            working = false
                        }
                        model.initList(
                            callback = {
                                model.updateSongsInfo()
                                working = false
                            }
                        )
                    } else {
                        model.displayWelcomeScreen = true
                    }
                }
            }
        }
    }


}

@Composable
fun SaveFilesAlertDialog(
    model: MainViewModel,
    openDialog: Boolean,
    onValueChange: (Boolean) -> Unit,
    snackbar: (message: String) -> Unit
) {
    var skipIncomplete by remember { mutableStateOf(true) }
    var skipMissingInfo by remember { mutableStateOf(true) }

    if (openDialog) {
        AlertDialog(
            onDismissRequest = { onValueChange.invoke(false) },
            title = {
                Text(text = "批量导出缓存文件")
            },
            text = {
                Column(
                    modifier = Modifier
                        .padding(2.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        "总缓存文件数: " + model.songs.size
                    )
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = skipIncomplete,
                                onCheckedChange = { skipIncomplete = it }
                            )
                            Text("跳过不完整缓存文件", textAlign = TextAlign.Start)
                        }

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = skipMissingInfo,
                                onCheckedChange = { skipMissingInfo = it }
                            )
                            Text(
                                "跳过丢失info文件缓存",
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }


            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValueChange.invoke(false)
                        working = true
                        NeteaseCacheProvider.decryptSongList(
                            model.songs, skipIncomplete, skipMissingInfo,
                            callback = { out, hasError, e ->
                                if (hasError) {
                                    Log.e("decrypt songs", e?.message, e)
                                    snackbar.invoke("导出歌曲失败 : ${e?.message} ${out?.toString()}")
                                }
                            },
                            isLastOne = { working = false }
                        )
                    }
                ) {
                    Text("开始导出")
                }
            },
            dismissButton = {
                TextButton(onClick = { onValueChange.invoke(false) }) { Text("取消") }
            }
        )
    }
}

@Composable
fun CheckUpdate(model: MainViewModel) {

    if (model.hasUpdate) {
        AlertDialog(
            onDismissRequest = { model.hasUpdate = false },
            title = { Text("发现新版本[" + model.updateJSON.versionName + "]") },
            text = {
                Column(
                    modifier = Modifier
                        .padding(2.dp)
                        .fillMaxWidth()
                ) {
                    Text(text = model.updateJSON.updateInfo)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        model.hasUpdate = false
                        val uri: Uri = Uri.parse(model.updateJSON.updateLink)
                        val intent = Intent()
                        intent.action =
                            "android.intent.action.VIEW"
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.data = uri
                        App.context.startActivity(intent)
                    }
                ) {
                    Text("更新")
                }
            },
            dismissButton = {
                TextButton(onClick = { model.hasUpdate = false }) { Text("取消") }
            }
        )
    }

}

/*    DefView    */

@OptIn(
    ExperimentalAnimationApi::class, com.google.accompanist.pager.ExperimentalPagerApi::class,
    kotlinx.coroutines.DelicateCoroutinesApi::class
)
@Composable
fun DefaultView(model: MainViewModel) {

    // For Snackbar
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()

    // BottomBar & Pager
    var selectedItem by remember { mutableStateOf(0) }
    val navItemList: List<String> = listOf("home", "play", "setting")
    val state = rememberPagerState(initialPage = 0)
    val coroutineScope = rememberCoroutineScope()

    // use UI Controller in compose
    val systemUiController = rememberSystemUiController()

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


    NeteaseViewerTheme {

        systemUiController.setStatusBarColor(MaterialTheme.colors.background)
        systemUiController.setNavigationBarColor(MaterialTheme.colors.background)

        CheckUpdate(model)

        Scaffold(
            scaffoldState = scaffoldState,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.app_name)) },
                    backgroundColor = MaterialTheme.colors.background,
                    elevation = 0.dp,
                    actions = {
                        IconButton(onClick = {
                            model.updateSongsInfo()
                        }) {
                            Icon(
                                Icons.Rounded.CloudDownload, contentDescription = stringResource(
                                    id = R.string.list_update
                                )
                            )
                        }

                        var openDialog by remember {
                            mutableStateOf(false)
                        }
                        SaveFilesAlertDialog(
                            model = model,
                            openDialog = openDialog,
                            onValueChange = { openDialog = it },
                            snackbar = snackbar
                        )

                        IconButton(onClick = {
                            openDialog = true
                        }) {
                            Icon(Icons.Rounded.SaveAlt, contentDescription = "导出所有文件")
                        }
                    }
                )
            },
            bottomBar = {
                BottomNavigation(
                    backgroundColor = MaterialTheme.colors.background
                ) {
                    for (index in navItemList.indices) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                                .clickable(
                                    onClick = {
                                        selectedItem = index
                                        coroutineScope.launch {
                                            state.scrollToPage(index)
                                        }
                                    },
                                    indication = null,
                                    interactionSource = MutableInteractionSource()
                                ),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            NavigationIcon(index, selectedItem)
                            Spacer(Modifier.padding(top = 2.dp))
                            AnimatedVisibility(visible = index == selectedItem) {
                                Surface(
                                    shape = CircleShape,
                                    modifier = Modifier.size(5.dp),
                                    color = MaterialTheme.colors.onSurface
                                ) { }
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->

            HorizontalPager(
                state = state,
                count = navItemList.size,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) { page ->
                LaunchedEffect(currentPage) {
                    selectedItem = currentPage
                }
                when (navItemList[page]) {
                    "home" -> {
                        HomeScreen(
                            model = model,
                            scope = scope,
                            scaffoldState = scaffoldState,
                            clickable = { index, song ->
                                if (song.deleted) {
                                    snackbar.invoke("该文件已被删除")
                                    return@HomeScreen
                                }
                                model.playMusic(song)
                                snackbar.invoke("$index  ${song.name}")
                                if (model.errorWhenPlaying) {
                                    snackbar.invoke("播放出错 : $index  ${song.name}")
                                    model.errorWhenPlaying = false
                                }
                            }
                        )
                    }
                    "play" -> {
                        PlayScreen(model.selectedMusicItem)
                    }
                    "setting" -> {
                        SettingScreen()
                    }
                }


            }

        }

    }

}

@Composable
fun NavigationIcon(
    index: Int,
    selectedItem: Int
) {
    val alpha = if (selectedItem != index) 0.5f else 1f
    CompositionLocalProvider(LocalContentAlpha provides alpha) {
        when (index) {
            0 -> Icon(Icons.Outlined.Home, contentDescription = null)
            1 -> Icon(Icons.Outlined.MusicNote, contentDescription = null)
            else -> Icon(Icons.Outlined.Settings, contentDescription = null)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DefaultPreview(viewModel: MainViewModel = viewModel()) {

    DefaultView(
        viewModel
    )

}