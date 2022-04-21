package io.github.kineks.neteaseviewer

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.pager.HorizontalPager
import com.permissionx.guolindev.PermissionX
import io.github.kineks.neteaseviewer.data.local.Setting
import io.github.kineks.neteaseviewer.ui.home.HomeScreen
import io.github.kineks.neteaseviewer.ui.home.WelcomeScreen
import io.github.kineks.neteaseviewer.ui.home.working
import io.github.kineks.neteaseviewer.ui.play.PlayScreen
import io.github.kineks.neteaseviewer.ui.setting.SettingScreen
import io.github.kineks.neteaseviewer.ui.theme.NeteaseViewerTheme
import io.github.kineks.neteaseviewer.ui.view.CheckUpdate
import io.github.kineks.neteaseviewer.ui.view.SaveFilesAlertDialog
import kotlinx.coroutines.GlobalScope
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
                            updateInfo = true,
                            callback = {
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

/*    DefView    */

@OptIn(
    com.google.accompanist.pager.ExperimentalPagerApi::class,
    kotlinx.coroutines.DelicateCoroutinesApi::class
)
@Composable
fun DefaultView(model: MainViewModel) {


    NeteaseViewerTheme {

        val appState = rememberMainAppState()
        appState.setSystemBarColor()

        CheckUpdate(model)

        Scaffold(
            scaffoldState = appState.scaffoldState,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.app_name)) },
                    backgroundColor = MaterialTheme.colors.background,
                    elevation = 0.dp,
                    actions = {

                        IconButton(onClick = {
                            GlobalScope.launch {
                                model.reloadSongsList(updateInfo = true)
                            }
                        }) {
                            Icon(
                                Icons.Rounded.CloudDownload, contentDescription = stringResource(
                                    id = R.string.list_reload
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
                            snackbar = appState.snackbar
                        )

                        IconButton(onClick = {
                            openDialog = true
                        }) {
                            Icon(
                                Icons.Rounded.SaveAlt, contentDescription = stringResource(
                                    id = R.string.list_decrypt_all_files
                                )
                            )
                        }

                    }
                )
            },
            bottomBar = {
                io.github.kineks.neteaseviewer.ui.view.BottomNavigation(
                    selectedItem = appState.selectedItem,
                    navItemList = appState.navItemList,
                    whenIndexChange = { index ->
                        appState.scrollToPage(index)
                    }
                )
            }
        ) { paddingValues ->

            HorizontalPager(
                state = appState.state,
                count = appState.navItemList.size,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) { page ->
                LaunchedEffect(currentPage) {
                    appState.selectedItem.value = currentPage
                }
                when (appState.navItemList[page]) {
                    "home" -> {
                        HomeScreen(
                            model = model,
                            scope = appState.scope,
                            scaffoldState = appState.scaffoldState,
                            clickable = { index, song ->
                                if (song.deleted) {
                                    appState.snackbar(getString(R.string.list_file_has_deleted))
                                    return@HomeScreen
                                }
                                model.playMusic(song)
                                appState.snackbar("$index  ${song.name}")
                                if (model.errorWhenPlaying) {
                                    appState.snackbar(
                                        getString(
                                            R.string.play_error,
                                            "$index  ${song.name}"
                                        )
                                    )
                                    model.errorWhenPlaying = false
                                }
                            }
                        )
                    }
                    "play" -> {
                        PlayScreen(model.selectedMusicStateItem)
                    }
                    "setting" -> {
                        SettingScreen()
                    }
                }


            }

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