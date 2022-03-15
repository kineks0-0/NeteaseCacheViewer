package io.github.kineks.neteaseviewer.ui.home

import android.util.Log
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewModelScope
import coil.compose.rememberImagePainter
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshState
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.github.kineks.neteaseviewer.MainViewModel
import io.github.kineks.neteaseviewer.R
import io.github.kineks.neteaseviewer.data.local.Music
import io.github.kineks.neteaseviewer.data.local.NeteaseCacheProvider
import io.github.kineks.neteaseviewer.getString
import kotlinx.coroutines.*

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

    if (working) {
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
            refreshState.isRefreshing = true
            working = true
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
            snackbar = snackbar
        )
    }


}


@Composable
fun SongsList(
    songs: List<Music>,
    available: Boolean = !songs.isNullOrEmpty(),
    snackbar: (message: String) -> Unit,
    clickable: (index: Int, music: Music) -> Unit = { _, _ -> }
) {
    if (available && songs.isNotEmpty()) {
        LazyColumn(
            contentPadding = PaddingValues(top = 6.dp, bottom = 8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(songs) { index, music ->
                MusicItem(
                    index = index,
                    music = music,
                    clickable = clickable,
                    snackbar = snackbar
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

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun MusicItemDropdownMenu(
    index: Int,
    music: Music,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOpenDialog: (Boolean) -> Unit,
    snackbar: (message: String) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange.invoke(false) }) {
        DropdownMenuItem(onClick = {
            GlobalScope.launch {
                onExpandedChange.invoke(false)
                working = true
                delay(250)
                snackbar.invoke("导出中: index $index Song " + music.displayFileName)
                music.decryptFile { out, hasError, e ->
                    working = false
                    val text =
                        if (hasError) {
                            Log.e("decrypt songs", e?.message, e)
                            "保存失败! : ${e?.message} ${out?.toString()}"
                        } else
                            "保存成功:  ${music.displayFileName}"

                    snackbar.invoke(text)
                }
            }
        }) {
            Icon(
                Icons.Rounded.CloudDownload,
                contentDescription = "Delete",
                modifier = Modifier.padding(end = 16.dp)
            )
            Text("导出到音乐媒体库")
        }
        DropdownMenuItem(onClick = {
            GlobalScope.launch {
                onExpandedChange.invoke(false)
                music.delete()
                snackbar.invoke("已删除: index $index  " + music.file.name)
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
            onOpenDialog.invoke(true)
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

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun MusicItemAlertDialog(
    openDialog: Boolean,
    onOpenDialog: (Boolean) -> Unit,
    music: Music,
) {

    if (openDialog) {
        AlertDialog(
            onDismissRequest = { onOpenDialog.invoke(false) },
            title = { Text("缓存文件详细[${music.neteaseAppCache?.type ?: "Netease"},${music.id}]") },
            text = {
                Column(
                    modifier = Modifier
                        .padding(2.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        buildAnnotatedString {
                            append("\n")
                            append("歌曲名称: " + music.name + "\n\n")
                            append("歌曲专辑: " + music.album + "\n\n")
                            append("歌曲歌手: " + music.artists + "\n\n")
                            append("导出文件名: " + music.displayFileName + "\n\n")
                            append("\n\n")

                            append("完整缓存大小: " + (music.info?.fileSize ?: -1) + "\n\n")
                            append("该缓存比特率: " + music.displayBitrate + "\n\n")
                            append("完整缓存时长: " + (music.info?.duration ?: -1) + "\n\n")
                            append("完整缓存MD5: " + (music.info?.fileMD5 ?: -1) + "\n\n")
                            append("\n\n")

                            append("文件大小: " + music.file.length() + "\n\n")
                            append("文件名称: " + music.file.name + "\n\n")
                            append("文件路径: " + music.file.canonicalPath + "\n\n")
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { onOpenDialog.invoke(false) }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { onOpenDialog.invoke(false) }) { Text("取消") }
            }
        )
    }
}

@OptIn(DelicateCoroutinesApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MusicItem(
    index: Int,
    music: Music,
    snackbar: (message: String) -> Unit,
    clickable: (index: Int, music: Music) -> Unit = { _, _ -> }
) {

    //Log.d("SongListItem", "Call once")
    var expanded by remember {
        mutableStateOf(false)
    }
    var alpha by remember {
        mutableStateOf(1f)
    }

    LaunchedEffect(music.deleted) {
        alpha = if (music.deleted) 0.4f else 1f
    }


    var openDialog by remember { mutableStateOf(false) }
    MusicItemAlertDialog(
        openDialog = openDialog,
        onOpenDialog = { openDialog = it },
        music = music
    )

    Row(
        modifier = Modifier
            .height(64.dp)
            .fillMaxWidth()
            .combinedClickable(
                onLongClick = {
                    expanded = true
                },
                onClick = {
                    clickable.invoke(index, music)
                })
            .padding(top = 7.dp, bottom = 7.dp)
            .padding(start = 15.dp, end = 15.dp)
            .alpha(alpha),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {


        Surface(
            shape = MaterialTheme.shapes.medium,
            elevation = 4.dp
        ) {
            Image(
                painter = rememberImagePainter(
                    // 添加 250y250 参数限制宽高来优化加载大小,避免原图加载
                    data = music.smallAlbumArt ?: "",
                    builder = {
                        // 加载完淡入图片
                        //crossfade(true)
                    }
                ),
                contentDescription = "Song Album Art",
                modifier = Modifier
                    .size(50.dp)
                    .background(MaterialTheme.colors.onBackground.copy(alpha = 0.6f))
            )

            MusicItemDropdownMenu(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                onOpenDialog = { openDialog = it },
                index = index,
                music = music,
                snackbar = snackbar
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 6.dp, bottom = 4.dp)
            ) {
                Text(
                    text = (index + 1).toString(),
                    color = MaterialTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .alpha(0.85f)
                )
                Text(
                    text = music.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.subtitle1
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 9.dp, end = 8.dp, top = 3.dp, bottom = 1.dp)
            ) {

                InfoText(
                    buildAnnotatedString {
                        append(music.artists)
                        append(" - ${music.album}   ")
                    },
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .weight(0.75f)
                )

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .weight(0.25f)
                        .alpha(0.85f)
                ) {

                    if (music.incomplete) {
                        InfoText(
                            text = getString(id = R.string.list_incomplete),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.error,
                        )
                    }

                    if (!NeteaseCacheProvider.fastReader && music.info == null) {
                        InfoText(
                            text = getString(id = R.string.list_missing_info_file) + " " + NeteaseCacheProvider.infoExt,
                            color = MaterialTheme.colors.error,
                            modifier = Modifier
                                .padding(end = 2.dp)
                        )
                    }

                    if (music.deleted) {
                        InfoText(
                            text = getString(id = R.string.list_deleted),
                            color = MaterialTheme.colors.error,
                            modifier = Modifier
                                .padding(end = 2.dp)
                        )
                    }

                    if (music.saved) {
                        InfoText(
                            text = getString(id = R.string.list_saved),
                            color = MaterialTheme.colors.primary,
                            modifier = Modifier
                                .padding(end = 2.dp)
                        )
                    }

                    InfoText(
                        text = music.displayBitrate,
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier
                            .width(40.dp)
                    )
                }


            }


        }


    }
}

@Composable
fun InfoText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = TextAlign.Center,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = 1,
    style: TextStyle = MaterialTheme.typography.body2
) {
    Text(
        text = text,
        modifier = modifier.padding(start = 2.dp, end = 2.dp),
        color = color,
        fontSize = fontSize,
        fontFamily = fontFamily,
        fontStyle = fontStyle,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        style = style
    )
}

@Composable
fun InfoText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = TextAlign.Center,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = 1,
    style: TextStyle = MaterialTheme.typography.body2
) {
    Text(
        text = text,
        modifier = modifier.padding(start = 2.dp, end = 2.dp),
        color = color,
        fontSize = fontSize,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        style = style
    )
}