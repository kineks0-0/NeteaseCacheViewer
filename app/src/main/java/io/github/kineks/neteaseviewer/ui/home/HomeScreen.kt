package io.github.kineks.neteaseviewer.ui.home

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshState
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.github.kineks.neteaseviewer.MainViewModel
import io.github.kineks.neteaseviewer.R
import io.github.kineks.neteaseviewer.data.local.EmptyMusic
import io.github.kineks.neteaseviewer.data.local.Music
import io.github.kineks.neteaseviewer.data.local.NeteaseCacheProvider
import io.github.kineks.neteaseviewer.getString
import io.github.kineks.neteaseviewer.updateSongsInfo
import kotlinx.coroutines.*

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun HomeScreen(
    model: MainViewModel,
    scope: CoroutineScope = rememberCoroutineScope(),
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    clickable: (index: Int, music: Music) -> Unit = { _, _ -> }
) {

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
            GlobalScope.launch {
                val list = NeteaseCacheProvider.getCacheSongs()
                if (list != model.songs) {
                    model.reloadSongsList(list)
                    updateSongsInfo(model)
                }
                delay(1500)
                refreshState.isRefreshing = false
            }
        }
    ) {
        SongsList(
            songs = model.songs,
            clickable = clickable,
            scope = scope,
            scaffoldState = scaffoldState
        )
    }


}


@Composable
fun SongsList(
    songs: List<Music>,
    available: Boolean = !songs.isNullOrEmpty(),
    scope: CoroutineScope,
    scaffoldState: ScaffoldState,
    clickable: (index: Int, music: Music) -> Unit = { _, _ -> }
) {
    //Log.d("MainActivity", "Call once")
    if (available && songs.isNotEmpty()) {
        LazyColumn(
            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
        ) {
            itemsIndexed(songs) { index, music ->
                MusicItem(
                    index = index,
                    music = music,
                    clickable = clickable,
                    scaffoldState = scaffoldState,
                    scope = scope
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

@OptIn(DelicateCoroutinesApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Preview(showBackground = true)
@Composable
fun MusicItem(
    index: Int = 0,
    music: Music = EmptyMusic,
    scope: CoroutineScope = rememberCoroutineScope(),
    scaffoldState: ScaffoldState = rememberScaffoldState(),
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
            .alpha(alpha)
    ) {

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(onClick = {
                GlobalScope.launch {
                    expanded = false
                    delay(250)
                    scope.launch {
                        scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                        scaffoldState.snackbarHostState
                            .showSnackbar(
                                message = "Saving: index $index Song : " + music.displayFileName,
                                actionLabel = getString(R.string.snackbar_dismissed),
                                duration = SnackbarDuration.Indefinite
                            )
                    }
                    music.decryptFile { out, hasError, e ->
                        val text =
                            if (hasError) {
                                Log.e("decrypt songs", e?.message, e)
                                "Failure: DecryptSong was Failure : ${e?.message} ${out?.toString()}"
                            } else
                                "Saved: DecryptSong was Saved : ${music.displayFileName}"

                        scope.launch {
                            scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                            scaffoldState.snackbarHostState
                                .showSnackbar(
                                    message = text,
                                    actionLabel = getString(R.string.snackbar_dismissed),
                                    duration = SnackbarDuration.Short
                                )
                        }

                    }
                }
            }) {
                Icon(
                    Icons.Filled.CloudDownload,
                    contentDescription = "Delete"
                )
                Text("   ")
                Text("Download to Music Library")
            }
            DropdownMenuItem(onClick = {
                GlobalScope.launch {
                    music.delete()
                    scope.launch {
                        scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                        scaffoldState.snackbarHostState
                            .showSnackbar(
                                message = "Deleted: index $index Song was Deleted : " + music.file.name,
                                actionLabel = getString(R.string.snackbar_dismissed),
                                duration = SnackbarDuration.Short
                            )
                    }
                }
            }) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete"
                )
                Text("   ")
                Text("Delete the Cache File")
            }
        }

        Surface(
            shape = MaterialTheme.shapes.medium,
            elevation = 4.dp
        ) {
            Image(
                painter = rememberImagePainter(
                    // 添加 250y250 参数限制宽高来优化加载大小,避免原图加载
                    data = music.getAlbumPicUrl(80, 80) ?: "",
                    builder = {
                        crossfade(true)
                    }
                ),
                contentDescription = "Song Album Art",
                modifier = Modifier
                    .size(50.dp)
                    .background(MaterialTheme.colors.onBackground.copy(alpha = 0.6f))
            )
        }


        Column {

            Text(
                buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.primary.copy(alpha = 0.7f)
                        )
                    ) {
                        val indexDisplay = index + 1
                        append("$indexDisplay ")
                        when (true) {
                            indexDisplay < 10 -> {
                                append("  ")
                            }
                            indexDisplay < 100 -> {
                                append(" ")
                            }
                            else -> {}
                        }
                    }
                    append(music.name)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
                    .padding(start = 10.dp, top = 2.dp, bottom = 1.dp, end = 5.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 11.dp, top = 1.dp, bottom = 2.dp, end = 5.dp)
            ) {

                InfoText(
                    buildAnnotatedString {
                        append(music.artists)
                        append(" - ${music.album}   ")
                    },
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(2.8f)
                )

                if (music.incomplete) {
                    InfoText(
                        buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    //fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colors.error.copy(alpha = 0.6f)
                                )
                            ) {
                                append(
                                    getString(id = R.string.list_incomplete)
                                )
                            }
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.7f)
                    )
                }

                if (!NeteaseCacheProvider.fastReader && music.info == null) {
                    InfoText(
                        buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colors.error.copy(alpha = 0.6f)
                                )
                            ) {
                                append(getString(id = R.string.list_missing_info_file))
                                append(" ")
                                append(NeteaseCacheProvider.infoExt)
                            }
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.7f)
                            .padding(end = 2.dp)
                    )
                }

                if (music.deleted) {
                    InfoText(
                        buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colors.error.copy(alpha = 0.7f)
                                )
                            ) {
                                append(getString(id = R.string.list_deleted))
                            }
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.7f)
                            .padding(end = 2.dp)
                    )
                }

                if (music.saved) {
                    InfoText(
                        buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colors.primary.copy(alpha = 0.7f)
                                )
                            ) {
                                append(getString(id = R.string.list_saved))
                            }
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.7f)
                            .padding(end = 2.dp)
                    )
                }

                InfoText(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                //fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colors.primary.copy(alpha = 0.6f)
                            )
                        ) {
                            append(music.displayBitrate)
                        }
                    }, modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.7f)
                        .padding(bottom = 0.dp, end = 10.dp)
                )


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
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = TextAlign.End,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    softWrap: Boolean = true,
    maxLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = MaterialTheme.typography.body2
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontFamily = fontFamily,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        inlineContent = inlineContent,
        onTextLayout = onTextLayout,
        style = style
    )
}