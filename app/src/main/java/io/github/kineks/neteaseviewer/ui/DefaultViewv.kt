package io.github.kineks.neteaseviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.pager.HorizontalPager
import io.github.kineks.neteaseviewer.MainViewModel
import io.github.kineks.neteaseviewer.R
import io.github.kineks.neteaseviewer.rememberMainAppState
import io.github.kineks.neteaseviewer.ui.home.HomeScreen
import io.github.kineks.neteaseviewer.ui.play.PlayScreen
import io.github.kineks.neteaseviewer.ui.setting.SettingScreen
import io.github.kineks.neteaseviewer.ui.theme.NeteaseViewerTheme
import io.github.kineks.neteaseviewer.ui.view.BottomNavigationBar
import io.github.kineks.neteaseviewer.ui.view.CheckUpdate
import io.github.kineks.neteaseviewer.ui.view.SaveFilesAlertDialog
import kotlinx.coroutines.launch


/*    DefView    */

@OptIn(
    com.google.accompanist.pager.ExperimentalPagerApi::class
)
@Composable
fun DefaultView(model: MainViewModel = viewModel()) {

    val appState =
        rememberMainAppState()

    SideEffect {
        appState.setSystemBarColor()
    }

    CheckUpdate(model)

    var openDialog by remember {
        mutableStateOf(false)
    }
    SaveFilesAlertDialog(
        model = model,
        openDialog = openDialog,
        onValueChange = { openDialog = it },
        snackbar = appState.snackbar
    )

    Scaffold(
        scaffoldState = appState.scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                backgroundColor = MaterialTheme.colors.background,
                elevation = 0.dp,
                actions = {

                    IconButton(onClick = {
                        model.viewModelScope.launch {
                            model.displayWelcomeScreen = true
                            //model.reloadSongsList(updateInfo = true)
                        }
                    }) {
                        Icon(
                            Icons.Rounded.CloudDownload,
                            stringResource(R.string.list_reload)
                        )
                    }

                    IconButton(onClick = { openDialog = true }) {
                        Icon(
                            Icons.Rounded.SaveAlt,
                            stringResource(R.string.list_decrypt_all_files)
                        )
                    }

                }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                selectedItem = appState.selectedItem.value,
                navItemList = appState.navItemList,
                navItemIconList = appState.navItemIconList,
                whenIndexChange = { index ->
                    appState.scrollToPage(index)
                }
            )
        }
    ) { paddingValues ->

        LaunchedEffect(appState.state.currentPage) {
            appState.selectedItem.value = appState.state.currentPage
        }

        HorizontalPager(
            count = appState.navItemList.size,
            state = appState.state
        ) { page ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
                    .padding(paddingValues)
                //.padding(vertical = 16.dp)
            ) {
                when (appState.navItemList[page]) {
                    "home" -> {
                        HomeScreen(
                            scope = appState.scope,
                            scaffoldState = appState.scaffoldState
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
fun DefaultPreview() {

    NeteaseViewerTheme {
        DefaultView(
            viewModel()
        )
    }

}