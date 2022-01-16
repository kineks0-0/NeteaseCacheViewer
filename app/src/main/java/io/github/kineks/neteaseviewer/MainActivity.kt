package io.github.kineks.neteaseviewer

//import androidx.navigation.compose.composable
import android.Manifest
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.permissionx.guolindev.PermissionX
import io.github.kineks.neteaseviewer.data.local.Music
import io.github.kineks.neteaseviewer.ui.HomeScreen
import io.github.kineks.neteaseviewer.ui.PlayScreen
import io.github.kineks.neteaseviewer.ui.SettingScreen
import io.github.kineks.neteaseviewer.ui.theme.NeteaseViewerTheme
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

        // 检查权限, 如果已授权读写权限就初始化数据
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
                    // 注: 该函数仅在第一次调用会重新加载数据
                    // 重载数据请用 model.reload()
                    model.initList()
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

    //val songs = model.songs

    // For Snackbar
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()

    // BottomBar & NavController
    var selectedItem by remember { mutableStateOf(0) }
    val navItemList: List<String> = listOf("home", "play", "setting")
    val navController = rememberAnimatedNavController()//rememberNavController()

    // use UI Controller in compose
    val systemUiController = rememberSystemUiController()

    var selectedMusicItem: Music? by remember { mutableStateOf(null) }


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
                                        //navController.popBackStack()
                                        navController.navigate(navItemList[index]) {
                                            popUpTo("home")// { inclusive = true }
                                            launchSingleTop = true
                                            restoreState = true

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

                AnimatedNavHost(navController = navController, startDestination = "home") {

                    composable(
                        route = "home",
                        enterTransition = { fadeIn(animationSpec = tween(255)) },
                        exitTransition = { ExitTransition.None }
                    ) {
                        HomeScreen(
                            model = model,
                            //scope = scope,
                            //scaffoldState = scaffoldState,
                            clickable = { index, song ->
                                selectedMusicItem = song
                                scope.launch {
                                    scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                                    val result = scaffoldState.snackbarHostState
                                        .showSnackbar(
                                            message = "$index  ${song.name}",
                                            actionLabel = getString(R.string.snackbar_dismissed),
                                            duration = SnackbarDuration.Short
                                        )
                                    when (result) {
                                        SnackbarResult.ActionPerformed -> {

                                        }
                                        SnackbarResult.Dismissed -> {

                                        }
                                    }
                                }

                            }
                        )
                    }

                    composable(
                        route = "play",
                        enterTransition = { fadeIn(animationSpec = tween(255)) },
                        exitTransition = { ExitTransition.None }
                    ) {
                        PlayScreen(selectedMusicItem)
                    }

                    composable(
                        route = "setting",
                        enterTransition = { fadeIn(animationSpec = tween(255)) },
                        exitTransition = { ExitTransition.None }
                    ) {
                        SettingScreen(navController = navController)
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