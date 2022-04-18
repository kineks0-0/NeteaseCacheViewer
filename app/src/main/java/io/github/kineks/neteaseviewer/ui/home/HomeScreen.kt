package io.github.kineks.neteaseviewer.ui.home

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewModelScope
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshState
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.github.kineks.neteaseviewer.MainViewModel
import io.github.kineks.neteaseviewer.R
import io.github.kineks.neteaseviewer.data.local.NeteaseCacheProvider
import io.github.kineks.neteaseviewer.data.local.cacheFile.Music
import io.github.kineks.neteaseviewer.formatFileSize
import io.github.kineks.neteaseviewer.getString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

var working by mutableStateOf(false)

@Composable
fun HomeScreen(
    model: MainViewModel,
    scope: CoroutineScope = rememberCoroutineScope(),
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    clickable: (index: Int, music: Music) -> Unit = { _, _ -> }
) {

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

    if (working || model.isUpdating) {
        LinearProgressIndicator(
            modifier = Modifier
                .padding(
                    top = 8.dp,
                    bottom = 8.dp
                )
                .fillMaxWidth()
                .offset(y = (-8).dp)
                .zIndex(1f)
        )
    }

    LaunchedEffect(model.isUpdating) {
        if (model.isUpdating)
            scope.launch {
                scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                scaffoldState.snackbarHostState
                    .showSnackbar(
                        message = "Working...",
                        actionLabel = getString(R.string.snackbar_dismissed),
                        duration = SnackbarDuration.Indefinite
                    )
            }
    }

    LaunchedEffect(model.isUpdateComplete) {
        if (model.isUpdateComplete)
            scope.launch {
                scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                scaffoldState.snackbarHostState
                    .showSnackbar(
                        message = getString(
                            if (model.isFailure)
                                R.string.list_update_failure
                            else
                                R.string.list_updated
                        ),
                        actionLabel = getString(R.string.snackbar_dismissed),
                        duration = SnackbarDuration.Short
                    )
            }
    }


    val refreshState: SwipeRefreshState = rememberSwipeRefreshState(false)

    SwipeRefresh(
        state = refreshState,
        onRefresh = {
            working = true
            refreshState.isRefreshing = true
            model.viewModelScope.launch {
                model.reloadSongsList(updateInfo = true)
                delay(1500)
                refreshState.isRefreshing = false
                working = false
            }
        }
    ) {
        SongsList(
            songs = model.songs,
            clickable = clickable,
            snackbar = snackbar,
            onWorking = { working = it }
        )
    }


}


@Composable
fun SongsList(
    songs: List<Music>,
    available: Boolean = songs.isNotEmpty(),
    snackbar: (message: String) -> Unit,
    onWorking: (Boolean) -> Unit,
    clickable: (index: Int, music: Music) -> Unit = { _, _ -> }
) {
    if (available && songs.isNotEmpty()) {
        val color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
        LazyColumn(
            contentPadding = PaddingValues(top = 7.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(songs) { index, music ->
                MusicItem(
                    index = index,
                    name = music.name,
                    album = music.album,
                    artists = music.artists,
                    artPainter = rememberAsyncImagePainter(music.smallAlbumArt),
                    artBackground = color,
                    incomplete = music.incomplete,
                    deleted = music.deleted,
                    saved = music.saved,
                    info = music.info == null,
                    displayBitrate = music.displayBitrate,
                    clickable = { clickable(index, music) },
                    musicItemAlertDialog = { openDialog, onOpenDialog ->
                        MusicItemAlertDialog(
                            openDialog = openDialog,
                            onOpenDialog = onOpenDialog,
                            music = music
                        )
                    },
                    musicItemDropdownMenu = { expanded, onExpanded, onOpenDialog ->
                        MusicItemDropdownMenu(
                            index = index,
                            music = music,
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
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = stringResource(id = R.string.list_no_data),
                textAlign = TextAlign.Center
            )
        }
    }

}

@Composable
fun MusicItemDropdownMenu(
    index: Int,
    music: Music,
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
                snackbar("导出中: 索引 $index 歌曲 " + music.displayFileName)
                music.decryptFile { out, hasError, e ->
                    onWorking(false)
                    val text =
                        if (hasError) {
                            Log.e("decrypt songs", e?.message, e)
                            "保存失败! : ${e?.message} ${out?.toString()}"
                        } else
                            "保存成功:  ${music.displayFileName}"

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
                music.delete()
                snackbar("已删除: index $index  " + music.file.name)
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
    music: Music,
) {

    if (openDialog) {
        AlertDialog(
            onDismissRequest = { onOpenDialog(false) },
            title = { Text("缓存文件详细 [${music.neteaseAppCache?.type ?: "Netease"},${music.id}]") },
            text = {
                Column(
                    modifier = Modifier
                        .padding(2.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        buildAnnotatedString {
                            music.run {
                                append("\n")
                                append("歌曲名称: $name\n\n")
                                append("歌曲专辑: $album\n\n")
                                append("歌曲歌手: $artists\n\n")
                                append("导出文件名: $displayFileName\n\n")
                                append("\n\n")

                                append("完整缓存大小:  ${(info?.fileSize ?: -1).formatFileSize()}\n\n")
                                append("该缓存比特率: $displayBitrate\n\n")
                                append("完整缓存时长: ${(info?.duration ?: -1)}\n\n")
                                append("完整缓存MD5: ${(info?.fileMD5 ?: -1)}\n\n")
                                append("\n\n")

                                append("文件大小:  ${file.length().formatFileSize()}\n\n")
                                append("文件名称: ${file.name}\n\n")
                                append("文件路径: ${file.canonicalPath}\n\n")
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
    name: String,
    deleted: Boolean,
    artists: String,
    album: String,
    incomplete: Boolean,
    saved: Boolean,
    displayBitrate: String,
    info: Boolean,
    artBackground: Color,
    artPainter: Painter,// = rememberAsyncImagePainter(music.smallAlbumArt),
    musicItemDropdownMenu: @Composable (expanded: Boolean, onExpanded: (Boolean) -> Unit, onOpenDialog: (Boolean) -> Unit) -> Unit,
    musicItemAlertDialog: @Composable (openDialog: Boolean, onOpenDialog: (Boolean) -> Unit) -> Unit,
    clickable: () -> Unit = { }
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    var alpha by remember {
        mutableStateOf(1f)
    }

    LaunchedEffect(deleted) {
        if (deleted)
            alpha = 0.4f
    }


    var openDialog by remember { mutableStateOf(false) }
    musicItemAlertDialog(openDialog) { openDialog = it }

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


            musicItemDropdownMenu(
                expanded,
                { expanded = it },
                { openDialog = it }
            )
        }

        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {

            Row(
                modifier = Modifier
                    .padding(start = 10.dp, bottom = 4.dp)
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
                    text = name,
                    maxLines = 1,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.subtitle1
                )
            }

            Row(
                modifier = Modifier
                    .padding(start = 9.dp, end = 8.dp, top = 3.dp)
            ) {

                InfoText(
                    text = "$artists - $album",
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

                if (!NeteaseCacheProvider.fastReader && info) {
                    InfoText(
                        text = getString(id = R.string.list_missing_info_file) + " " + NeteaseCacheProvider.infoExt,
                        color = MaterialTheme.colors.error,
                        modifier = Modifier
                            .padding(end = 2.dp)
                    )
                }

                if (deleted) {
                    InfoText(
                        text = getString(id = R.string.list_deleted),
                        color = MaterialTheme.colors.error,
                        modifier = Modifier
                            .padding(end = 2.dp)
                    )
                }

                if (saved) {
                    InfoText(
                        text = getString(id = R.string.list_saved),
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier
                            .padding(end = 2.dp)
                    )
                }

                InfoText(
                    text = displayBitrate,
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier
                        .width(40.dp)
                )


            }


        }



        Icon(
            Icons.Rounded.MoreVert,
            contentDescription = "MoreVert",
            modifier = Modifier.padding(end = 2.dp)
        )


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
        modifier = modifier.padding(start = 2.dp, end = 2.dp),
        color = color,
        fontWeight = fontWeight,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        style = style
    )
}

@Preview(showBackground = true, widthDp = 200, heightDp = 80)
@Composable
fun DefPreView() {
    MusicItem(
        index = 0,
        name = "Name",
        album = "Album",
        artists = "Artists",
        artPainter = painterResource(id = R.drawable.ic_launcher_background),
        artBackground = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
        incomplete = false,
        deleted = false,
        saved = false,
        info = false,
        displayBitrate = "192 k",
        musicItemAlertDialog = { _, _ ->
            Text("Todo")
        },
        musicItemDropdownMenu = { _, _, _ ->
            Text("Todo")
        })
}