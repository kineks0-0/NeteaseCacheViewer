package io.github.kineks.neteaseviewer.ui.home

import android.content.res.Configuration.UI_MODE_NIGHT_UNDEFINED
import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.swiperefresh.SwipeRefresh
import io.github.kineks.neteaseviewer.MainViewModel
import io.github.kineks.neteaseviewer.R
import io.github.kineks.neteaseviewer.data.local.NeteaseCacheProvider
import io.github.kineks.neteaseviewer.data.local.cacheFile.MusicState
import io.github.kineks.neteaseviewer.formatFileSize
import io.github.kineks.neteaseviewer.getString
import io.github.kineks.neteaseviewer.ui.theme.NeteaseViewerTheme
import io.github.kineks.neteaseviewer.ui.view.checkPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                        R.string.play_error,
                        "$index  ${song.name}"
                    )
                )
                model.errorWhenPlaying = false
            }
        }
    }

    if (working || model.isUpdating) {
        LinearProgressIndicator(
            modifier = Modifier
                /*.padding(
                    top = 8.dp,
                    bottom = 8.dp
                )*/
                .fillMaxWidth()
                .offset(y = (-3).dp)
                .zIndex(1f)
        )
    }

    LaunchedEffect(model.isUpdating) {
        if (model.isUpdating)
            appState.snackbarIndefinite("Working...")
    }

    LaunchedEffect(model.isUpdateComplete) {
        if (model.isUpdateComplete)
            appState.snackbar(
                getString(
                    if (model.isFailure)
                        R.string.list_update_failure
                    else
                        R.string.list_updated
                )
            )
    }

    SwipeRefresh(
        state = appState.refreshState,
        onRefresh = {
            model.viewModelScope.launch {
                working = true
                appState.refreshState.isRefreshing = true
                model.reloadSongsList(updateInfo = true)
                delay(1500)
                appState.refreshState.isRefreshing = false
                working = false
            }
        }
    ) {
        SongsList(
            songs = model.songs,
            model = model,
            clickable = clickable,
            snackbar = appState.snackbar,
            onWorking = { working = it }
        )
    }


}


@Composable
fun SongsList(
    model: MainViewModel = viewModel(),
    songs: List<MusicState> = model.songs,
    available: Boolean = songs.isNotEmpty(),
    snackbar: (message: String) -> Unit,
    onWorking: (Boolean) -> Unit,
    clickable: (index: Int, musicState: MusicState) -> Unit = { _, _ -> }
) {
    if (available && songs.isNotEmpty()) {

        val artBackground = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(songs) { index, music ->

                MusicItem(
                    index = index,
                    title = music.name,
                    subtitle = "${music.artists} - ${music.album}",
                    artPainter = rememberAsyncImagePainter(music.smallAlbumArt),
                    artBackground = artBackground,
                    incomplete = music.incomplete,
                    deleted = music.deleted,
                    saved = music.saved,
                    missInfoFile = music.missingInfo,
                    displayBitrate = music.displayBitrate,
                    clickable = { clickable(index, music) },
                    musicItemAlertDialog = { openDialog, onOpenDialog ->
                        MusicItemAlertDialog(
                            openDialog = openDialog,
                            onOpenDialog = onOpenDialog,
                            musicState = music
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
                    }
                )

            }
        }

    } else {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.6f)
            ) {

                if (model.displayPermissionDialog) {
                    Text(
                        text = stringResource(R.string.list_no_data),
                        style = MaterialTheme.typography.h5
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    checkPermission { allGranted ->
                        if (allGranted) {
                            model.displayPermissionDialog = false
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
                } else {
                    Text(
                        text = stringResource(R.string.list_no_data),
                        style = MaterialTheme.typography.h6
                    )
                    Text(
                        text = stringResource(R.string.refresh),
                        //style = MaterialTheme.typography.h,
                        color = MaterialTheme.colors.primary,
                        fontWeight = FontWeight.Medium,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            model.viewModelScope.launch {
                                working = true
                                model.reloadSongsList(updateInfo = true)
                                delay(1500)
                                working = false
                            }
                        }
                    )
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
                    val text =
                        if (hasError) {
                            Log.e("decrypt songs", e?.message, e)
                            "保存失败! : ${e?.message} ${out?.toString()}"
                        } else
                            "保存成功:  ${musicState.displayFileName}"

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

@Composable
fun MusicItemAlertDialog(
    openDialog: Boolean,
    onOpenDialog: (Boolean) -> Unit,
    musicState: MusicState,
) {

    if (openDialog) {
        AlertDialog(
            onDismissRequest = { onOpenDialog(false) },
            title = { Text("缓存文件详细 [${musicState.neteaseAppCache?.type ?: "Netease"},${musicState.id}]") },
            text = {
                Column(
                    modifier = Modifier
                        .padding(2.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        buildAnnotatedString {
                            musicState.run {
                                append("\n")
                                append("歌曲名称: $name\n\n")
                                append("歌曲专辑: $album\n\n")
                                append("歌曲歌手: $artists\n\n")
                                append("导出文件名: $displayFileName\n\n")
                                append("\n\n")

                                val info = NeteaseCacheProvider.getCacheFileInfo(musicState)
                                append("完整缓存大小:  ${(info?.fileSize ?: -1).formatFileSize()}\n\n")
                                append("该缓存比特率: $displayBitrate\n\n")
                                append("完整缓存时长: ${(info?.duration ?: -1)}\n\n")
                                append("完整缓存MD5: ${(info?.fileMD5 ?: -1)}\n\n")
                                append("\n\n")

                                append("文件大小:  ${file.length().formatFileSize()}\n\n")
                                append("文件名称: ${file.name}\n\n")
                                append("文件路径: ${file.type}://${file.path}\n\n")
                            }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onOpenDialog(false) }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { onOpenDialog(false) }) { Text("取消") }
            }
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicItem(
    index: Int,
    title: String,
    subtitle: String,

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
            .height(64.dp)
            .combinedClickable(
                onLongClick = {
                    expanded = true
                },
                onClick = {
                    clickable()
                })
            .padding(start = 15.dp, end = 10.dp)
            .alpha(alpha),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            elevation = 4.dp
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
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {

            Row(
                modifier = Modifier.padding(start = 10.dp, bottom = 4.dp)
            ) {
                Text(
                    text = (index + 1).toString(),
                    color = MaterialTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = title,
                    maxLines = 1,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.subtitle1
                )
            }

            Row(
                modifier = Modifier.padding(start = 9.dp, end = 8.dp, top = 3.dp)
            ) {

                InfoText(
                    text = subtitle,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(0.70f)
                )


                if (incomplete) {
                    InfoText(
                        text = getString(id = R.string.list_incomplete),
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.error,
                    )
                }

                if (!NeteaseCacheProvider.fastReader && missInfoFile) {
                    InfoText(
                        text = getString(
                            R.string.list_missing_info_file,
                            NeteaseCacheProvider.infoExt
                        ),
                        color = MaterialTheme.colors.error
                    )
                }

                if (deleted) {
                    InfoText(
                        text = getString(id = R.string.list_deleted),
                        color = MaterialTheme.colors.error
                    )
                }

                if (saved) {
                    InfoText(
                        text = getString(id = R.string.list_saved),
                        color = MaterialTheme.colors.primary
                    )
                }

                InfoText(
                    text = displayBitrate,
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.width(40.dp)
                )


            }


        }



        Box {

            Icon(
                Icons.Rounded.MoreVert,
                contentDescription = "MoreVert",
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        expanded = true
                    }
                    .padding(end = 2.dp, top = 3.dp)
            )

            if (expanded)
                musicItemDropdownMenu(
                    expanded,
                    { expanded = it },
                    { openDialog = it }
                )
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

@Preview(showBackground = true, widthDp = 500, heightDp = 80, uiMode = UI_MODE_NIGHT_UNDEFINED)
@Composable
fun DefPreView() {
    NeteaseViewerTheme {
        Column(
            Modifier.padding(start = 15.dp, end = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            MusicItem(
                index = 1,
                title = "Name",
                subtitle = "Album - Artists",
                artPainter = painterResource(id = R.drawable.ic_launcher_background),
                artBackground = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
                incomplete = false,
                deleted = false,
                saved = false,
                missInfoFile = false,
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