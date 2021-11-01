package io.github.kineks.neteaseviewer

import android.Manifest
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
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.permissionx.guolindev.PermissionX
import io.github.kineks.neteaseviewer.data.local.Music
import io.github.kineks.neteaseviewer.ui.HomeScreen
import io.github.kineks.neteaseviewer.ui.SettingScreen
import io.github.kineks.neteaseviewer.ui.theme.NeteaseViewerTheme
import io.github.kineks.neteaseviewer.ui.PlayScreen
import kotlinx.coroutines.launch


class MainActivity : FragmentActivity() {
    private val model: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DefaultView(model)
        }

    }


    override fun onStart() {
        super.onStart()

        PermissionX.init(this)
            .permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .onExplainRequestReason { scope, deniedList ->
                val message = getString(R.string.permission_request_description)
                scope.showRequestReasonDialog(
                    deniedList, message,
                    getString(R.string.permission_allow),
                    getString(R.string.permission_deny)
                )
            }
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    model.reloadSongsList()
                } else {
                    // todo: 提示用户
                }
            }

    }
}



/*    DefView    */


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DefaultView(model: MainViewModel) {
    val songs = model.songs//: List<Music>? by model.songs.toMutableList()

    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()

    var selectedItem by remember { mutableStateOf(0) }
    val navItemList: List<String> = listOf("home","play","setting")
    val navController = rememberNavController()


    val systemUiController = rememberSystemUiController()


    NeteaseViewerTheme {

        systemUiController.setStatusBarColor(MaterialTheme.colors.background)
        systemUiController.setNavigationBarColor(MaterialTheme.colors.background)


        Scaffold(
            scaffoldState = scaffoldState,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.app_name)) },
                    backgroundColor = MaterialTheme.colors.background,
                    elevation = 0.dp
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {

                        model.updateSongsInfo(
                            // todo : 修复加载 bug 之后更改这里的参数
                            quantity = songs.size.minus(2) ?: 50,
                            onUpdateComplete = {
                                Log.d("MainActivity", "All data update")

                                scope.launch {
                                    scaffoldState.snackbarHostState
                                        .showSnackbar(
                                            message = "All data update",
                                            actionLabel = "Dismissed",
                                            duration = SnackbarDuration.Short
                                        )
                                }
                            }
                        )

                    },
                    icon = {
                        Icon(
                            Icons.Outlined.CloudDownload,
                            contentDescription = "Update"
                        )
                    },
                    text = { Text("Update") }

                )
            },
            bottomBar = {
                BottomNavigation(
                    backgroundColor = MaterialTheme.colors.background
                ) {
                    for (index in 0..2) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                                .clickable(
                                    onClick = {
                                        selectedItem = index
                                        navController.popBackStack()
                                        navController.navigate(navItemList[index]) {
                                            //popUpTo("home") { inclusive = true }
                                            launchSingleTop = true
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
        ) {


            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
                    .padding(bottom = it.calculateBottomPadding())
            ) {

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") { HomeScreen(songs, scope, scaffoldState) }
                    composable("play") { PlayScreen() }
                    composable("setting") { SettingScreen(navController = navController) }
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