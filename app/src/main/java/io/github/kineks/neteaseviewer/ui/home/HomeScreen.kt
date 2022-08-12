package io.github.kineks.neteaseviewer.ui.home

import android.content.res.Configuration.UI_MODE_NIGHT_UNDEFINED
import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Feed
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemsIndexed
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.swiperefresh.SwipeRefresh
import io.github.kineks.neteaseviewer.*
import io.github.kineks.neteaseviewer.R
import io.github.kineks.neteaseviewer.data.local.NeteaseCacheProvider
import io.github.kineks.neteaseviewer.data.local.cacheFile.CacheFileInfo
import io.github.kineks.neteaseviewer.data.local.cacheFile.EmptyCacheFileInfo
import io.github.kineks.neteaseviewer.data.local.cacheFile.MusicState
import io.github.kineks.neteaseviewer.data.local.toRFile
import io.github.kineks.neteaseviewer.ui.theme.NeteaseViewerTheme
import io.github.kineks.neteaseviewer.ui.view.checkPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

var working by mutableStateOf(true)

@Composable
fun HomeScreen(
    model: MainViewModel = viewModel(),
    scope: CoroutineScope = rememberCoroutineScope(),
    scaffoldState: ScaffoldState = rememberScaffoldState()
) {

    val appState = rememberHomeAppState(scope, scaffoldState)

    val clickable: (index: Int, musicState: MusicState) -> Unit = { index, song ->
        if (song.deleted) {
            appState.snackbar(getString(R.string.list_file_has_deleted))
        } else {
            model.playMusic(song)
            appState.snackbar("$index  ${song.name}")
            if (model.errorWhenPlaying) {
                appState.snackbar(
                    getString(
                        R.string.play_error, "$index  ${song.name}"
                    )
                )
                model.errorWhenPlaying = false
            }
        }
    }

    if (working || model.isUpdating) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .offset(y = (-3).dp)
                .zIndex(20f)
        )
    }

    LaunchedEffect(model.isUpdating) {
        if (model.isUpdating) appState.snackbarIndefinite("Working...")
    }

    LaunchedEffect(model.isUpdateComplete) {
        if (model.isUpdateComplete) appState.snackbar(
            getString(
                if (model.isFailure) R.string.list_update_failure
                else R.string.list_updated
            )
        )
    }

    val list = model.songsFlow.collectAsLazyPagingItems()

    LaunchedEffect(appState.refreshState.isRefreshing) {
        if (appState.refreshState.isRefreshing)
            list.refresh()
    }

    SwipeRefresh(state = appState.refreshState, onRefresh = {
        model.viewModelScope.launch {
            working = true
            appState.refreshState.isRefreshing = true
            //model.reloadSongsList(updateInfo = true)
            //delay(1500)
        }

    }) {
        SongsList(
            songs = list,
            model = model,
            clickable = clickable,
            snackbar = appState.snackbar,
            onWorking = { working = it })
    }


    when (list.loadState.append) {
        is LoadState.NotLoading -> {
            appState.refreshState.isRefreshing = false
            working = false
        }
        else -> {}
    }

}


@Composable
fun SongsList(
    model: MainViewModel = viewModel(),
    songs: LazyPagingItems<MusicState>,
    available: Boolean = songs.itemCount != 0,
    snackbar: (message: String) -> Unit,
    onWorking: (Boolean) -> Unit,
    clickable: (index: Int, musicState: MusicState) -> Unit = { _, _ -> }
) {
    if (available && songs.itemCount != 0) {

        val artBackground = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(songs) { index, music ->

                if (music != null)
                    MusicItem(
                        index = index,
                        title = music.name,
                        subtitle = "${music.artists} - ${music.album}",
                        artPainter = rememberAsyncImagePainter(music.smallAlbumArt),
                        artBackground = artBackground,
                        incomplete = music.incomplete,
                        fastReader = NeteaseCacheProvider.fastReader,
                        deleted = music.deleted,
                        saved = music.saved,
                        missInfoFile = music.missingInfo,
                        displayBitrate = music.displayBitrate,
                        clickable = { clickable(index, music) },
                        musicItemAlertDialog = { openDialog, onOpenDialog ->
                            MusicItemAlertDialog(
                                openDialog = openDialog,
                                onOpenDialog = onOpenDialog,
                                musicState = music,
                                snackbar = snackbar
                            )
                        },
                        musicItemDropdownMenu = { expanded, onExpanded, onOpenDialog ->
                            MusicItemDropdownMenu(
                                index = index,
                                musicState = music,
                                expanded = expanded,
                                onExpandedChange = onExpanded,
                                onOpenDialog = onOpenDialog,
                                onWorking = onWorking,
                                snackbar = snackbar
                            )
                        })

            }
        }

    } else {

        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.6f)
            ) {

                Text(
                    text = stringResource(
                        if (working)
                            R.string.list_loading
                        else
                            R.string.list_no_data
                    ),
                    style = if (model.displayPermissionDialog)
                        MaterialTheme.typography.h5
                    else
                        MaterialTheme.typography.h6

                )

                if (model.displayPermissionDialog) {
                    Spacer(modifier = Modifier.height(8.dp))
                    checkPermission { allGranted ->
                        if (allGranted) {
                            model.displayPermissionDialog = false
                            // 注: 该函数仅在第一次调用会重新加载数据
                            // 重载数据请用 model.reload()
                            if (model.hadListInited) {
                                working = false
                            }
                            model.initList {
                                working = false
                            }
                        } else {
                            model.displayWelcomeScreen = true
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.refresh),
                        color = MaterialTheme.colors.primary,
                        fontWeight = FontWeight.Medium,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            model.viewModelScope.launch {
                                working = true
                                delay(1500)
                                working = false
                            }
                        })
                }


            }
        }


    }

}

@Composable
fun MusicItemDropdownMenu(
    index: Int,
    musicState: MusicState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOpenDialog: (Boolean) -> Unit,
    onWorking: (Boolean) -> Unit,
    snackbar: (message: String) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange.invoke(false) }) {

        DropdownMenuItem(onClick = {
            CoroutineScope(Dispatchers.IO).launch {
                onExpandedChange(false)
                onWorking(true)
                delay(250)
                snackbar("导出中: 索引 $index 歌曲 " + musicState.displayFileName)
                musicState.decryptFile { out, hasError, e ->
                    onWorking(false)
                    val text = if (hasError) {
                        Log.e("decrypt songs", e?.message, e)
                        "保存失败! : ${e?.message} ${out?.toString()}"
                    } else "保存成功:  ${musicState.displayFileName}"

                    snackbar(text)
                }
            }
        }) {
            Icon(
                Icons.Rounded.CloudDownload,
                contentDescription = "Decrypt Songs",
                modifier = Modifier.padding(end = 16.dp)
            )
            Text("导出到音乐媒体库")
        }

        DropdownMenuItem(onClick = {
            CoroutineScope(Dispatchers.IO).launch {
                onExpandedChange(false)
                musicState.delete()
                snackbar("已删除: 索引 $index  " + musicState.file.name)
            }
        }) {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = "Delete",
                modifier = Modifier.padding(end = 16.dp)
            )
            Text("删除缓存文件")
        }

        DropdownMenuItem(onClick = {
            onExpandedChange.invoke(false)
            onOpenDialog(true)
        }) {
            Icon(
                Icons.Rounded.Feed,
                contentDescription = "Info",
                modifier = Modifier.padding(end = 16.dp)
            )
            Text("查看缓存详细")
        }

    }
}

@OptIn(ExperimentalUnitApi::class)
@Composable
fun MusicItemAlertDialog(
    openDialog: Boolean,
    onOpenDialog: (Boolean) -> Unit,
    musicState: MusicState,
    snackbar: (message: String) -> Unit,
    cacheFileInfo: CacheFileInfo =
        NeteaseCacheProvider.getCacheFileInfo(musicState) ?: EmptyCacheFileInfo,
    ext: String = musicState.ext
) {

    if (openDialog) {
        AlertDialog(onDismissRequest = { onOpenDialog(false) },
            title = {
                Text(
                    text = "详情 [${musicState.neteaseAppCache?.type ?: "Netease"},${musicState.id}]",
                    modifier = Modifier.alpha(0.9f),
                    fontWeight = FontWeight.Medium,
                    letterSpacing = TextUnit(0.95f, TextUnitType.Sp),
                    color = MaterialTheme.colors.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .padding(2.dp)
                        .fillMaxWidth()
                ) {
                    Text(text = "", modifier = Modifier.padding(8.dp))
                    musicState.run {

                        MusicItemAlertDialogItem("文件名称", file.name, snackbar)
                        MusicItemAlertDialogItem("文件路径", "${file.type}://${file.path}", snackbar)
                        MusicItemAlertDialogItem("导出文件名", "$displayFileName.$ext", snackbar)
                        LazyVerticalGrid(
                            modifier = Modifier.fillMaxWidth(),
                            columns = GridCells.Fixed(2),
                            content = {

                                item {
                                    MusicItemAlertDialogItem(
                                        "文件大小",
                                        file.length().formatFileSize(), snackbar
                                    )
                                }
                                item { MusicItemAlertDialogItem("文件格式", ext.uppercase(), snackbar) }
                                item { MusicItemAlertDialogItem("歌曲名称", name, snackbar) }
                                item { MusicItemAlertDialogItem("歌曲歌手", artists, snackbar) }
                                item { MusicItemAlertDialogItem("歌曲专辑", album, snackbar) }
                                item { MusicItemAlertDialogItem("发行年份", year, snackbar) }
                                item { MusicItemAlertDialogItem("比特率", displayBitrate, snackbar) }
                                item {
                                    MusicItemAlertDialogItem(
                                        "总时长",
                                        cacheFileInfo.duration.formatMilSec(min = ":"), snackbar
                                    )
                                }
                                item { MusicItemAlertDialogItem("碟片号", disc, snackbar) }
                                item { MusicItemAlertDialogItem("音轨号", track.toString(), snackbar) }
                                //item { MusicItemAlertDialogItem("MD5", md5) }

                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onOpenDialog(false) }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { onOpenDialog(false) }) {
                    Text("取消")
                }
            })
    }
}

@OptIn(ExperimentalUnitApi::class)
@Composable
fun MusicItemAlertDialogItem(
    title: String, text: String,
    snackbar: (message: String) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 15.dp, end = 15.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.caption,
            modifier = Modifier
                .alpha(0.6f)
                .padding(bottom = 2.dp),
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onSurface
        )
        SelectionContainer {
            Text(
                text = text,
                //style = MaterialTheme.typography.body1,
                modifier = Modifier
                    .alpha(0.9f)
                    .clickable {
                        App.copyText(text)
                        snackbar("已复制")
                    },
                fontWeight = FontWeight.Medium,
                letterSpacing = TextUnit(0.95f, TextUnitType.Sp),
                color = MaterialTheme.colors.onSurface
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicItem(
    index: Int,
    title: String,
    subtitle: String,

    fastReader: Boolean,
    deleted: Boolean,
    incomplete: Boolean,
    saved: Boolean,
    displayBitrate: String,
    missInfoFile: Boolean,
    artBackground: Color,
    artPainter: Painter,
    musicItemDropdownMenu: @Composable (expanded: Boolean, onExpanded: (Boolean) -> Unit, onOpenDialog: (Boolean) -> Unit) -> Unit,
    musicItemAlertDialog: @Composable (openDialog: Boolean, onOpenDialog: (Boolean) -> Unit) -> Unit,
    clickable: () -> Unit = { }
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    val alpha = if (deleted) 0.4f else 1f

    var openDialog by remember { mutableStateOf(false) }
    if (openDialog) musicItemAlertDialog(openDialog) { openDialog = it }



    Row(
        modifier = Modifier
            .height(68.dp)
            .combinedClickable(onLongClick = {
                expanded = true
            }, onClick = {
                clickable()
            })
            .alpha(alpha)
            .padding(start = 17.dp, end = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium, elevation = 4.dp
        ) {
            Image(
                painter = artPainter,
                contentDescription = "Song Album Art",
                modifier = Modifier
                    .size(50.dp)
                    .background(artBackground)
            )
        }

        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(start = 6.dp)
        ) {

            Row(
                modifier = Modifier
                    .padding(start = 10.dp, bottom = 2.dp)
                    .height(25.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (index + 1).toString(),
                    color = MaterialTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .offset(y = (2).dp)
                )
                Text(
                    text = title,
                    maxLines = 1,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .weight(0.60f)
                        .offset(y = (2).dp)
                )

                InfoBoxText(
                    text = displayBitrate,
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colors.primary,
                    //modifier = Modifier.width(40.dp)
                )
            }

            Row(
                modifier = Modifier
                    .padding(start = 9.dp, top = 2.dp)
                    .height(25.dp)
                    .alpha(0.8f),
                verticalAlignment = Alignment.CenterVertically
            ) {

                InfoText(
                    text = subtitle,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .weight(0.60f)
                        .offset(y = (-3).dp)
                )


                if (incomplete) {
                    InfoBoxText(
                        text = stringResource(id = R.string.list_incomplete),
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.error,
                    )
                }

                if (!fastReader && missInfoFile) {
                    InfoBoxText(
                        text = stringResource(
                            R.string.list_missing_info_file, NeteaseCacheProvider.infoExt
                        ), color = MaterialTheme.colors.error
                    )
                }

                if (deleted) {
                    InfoBoxText(
                        text = stringResource(id = R.string.list_deleted),
                        color = MaterialTheme.colors.error
                    )
                }

                if (saved) {
                    InfoBoxText(
                        text = stringResource(id = R.string.list_saved),
                        color = MaterialTheme.colors.primary
                    )
                }


            }


        }



        Box(
            modifier = Modifier
                .fillMaxSize()
                .fillMaxWidth()
                .padding(end = 0.dp)
        ) {

            Icon(Icons.Rounded.MoreVert,
                contentDescription = "MoreVert",
                modifier = Modifier
                    .fillMaxSize()
                    .fillMaxWidth()
                    //.width(20.dp)
                    //.padding(top = 3.dp)
                    .padding(start = 0.dp, end = 7.dp)
                    .clickable {
                        expanded = true
                    })

            if (expanded) musicItemDropdownMenu(expanded, { expanded = it }, { openDialog = it })
        }


    }

}

@Composable
fun InfoText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = TextAlign.Center,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = 1,
    style: TextStyle = MaterialTheme.typography.body2
) {
    Text(
        text = text,
        modifier = modifier.padding(start = 1.dp, end = 2.dp),
        color = color,
        fontWeight = fontWeight,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        style = style
    )
}

@Composable
fun InfoBoxText(
    text: String,
    //modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = TextAlign.Center,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = 1,
    style: TextStyle = MaterialTheme.typography.caption
) {
    Surface(
        shape = MaterialTheme.shapes.medium, elevation = 0.dp,
        color = color.copy(alpha = 0.1f),
        modifier = Modifier
            .size(width = 60.dp, 25.dp)
            .fillMaxHeight()
            .padding(start = 1.dp, end = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(55.dp)
                .fillMaxHeight()
                .padding(start = 1.dp, end = 1.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                /*modifier = modifier
                    .size(50.dp)
                    .fillMaxHeight(),*/
                color = color,
                fontWeight = fontWeight,
                textAlign = textAlign,
                overflow = overflow,
                maxLines = maxLines,
                style = style
            )
        }

    }

}

@Preview(showBackground = true, widthDp = 500, heightDp = 80, uiMode = UI_MODE_NIGHT_UNDEFINED)
@Composable
fun ListItemPreView() {
    NeteaseViewerTheme {
        Column(
            Modifier.padding(start = 15.dp, end = 10.dp), verticalArrangement = Arrangement.Center
        ) {
            MusicItem(index = 1,
                title = "NameNameNameNameName",
                subtitle = "AlbumAlbumAlbum - ArtistsArtistsArtists",
                artPainter = painterResource(id = R.drawable.ic_launcher_background),
                artBackground = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
                incomplete = true,
                fastReader = false,
                deleted = false,
                saved = true,
                missInfoFile = true,
                displayBitrate = "192 k",
                musicItemAlertDialog = { _, _ ->
                    Text("Todo")
                },
                musicItemDropdownMenu = { _, _, _ ->
                    Text("Todo")
                })


        }
    }
}

@Preview
@Composable
fun DialogPreview() {

    NeteaseViewerTheme {

        MusicItemAlertDialog(
            openDialog = true,
            onOpenDialog = {},
            musicState = MusicState(
                1008611,
                96000,
                "N/A",
                File("Debug/cache.file").toRFile()
            ),
            cacheFileInfo = EmptyCacheFileInfo,
            ext = "mp3",
            snackbar = { }
        )

    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_UNDEFINED)
@Composable
fun DefPreView() {
    NeteaseViewerTheme {
        HomeScreen()
    }
}
