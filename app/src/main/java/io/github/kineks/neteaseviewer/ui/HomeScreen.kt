package io.github.kineks.neteaseviewer.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import io.github.kineks.neteaseviewer.MainViewModel
import io.github.kineks.neteaseviewer.R
import io.github.kineks.neteaseviewer.data.local.Music
import io.github.kineks.neteaseviewer.data.local.NeteaseCacheProvider
import io.github.kineks.neteaseviewer.getString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    model: MainViewModel,
    scope: CoroutineScope = rememberCoroutineScope(),
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    clickable: (index: Int, music: Music) -> Unit = { _, _ -> }
) {


    Scaffold(
        scaffoldState = scaffoldState,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {

                    model.updateSongsInfo(
                        onUpdateComplete = {
                            Log.d("MainActivity", "All data update")

                            scope.launch {
                                scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                                scaffoldState.snackbarHostState
                                    .showSnackbar(
                                        message = getString(R.string.list_updated),
                                        actionLabel = getString(R.string.snackbar_dismissed),
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
                text = { Text(stringResource(id = R.string.list_update)) }

            )
        }
    ) {
        SongsList(
            songs = model.songs,
            clickable = clickable
        )
    }


}


@Composable
fun SongsList(
    songs: List<Music>,
    available: Boolean = !songs.isNullOrEmpty(),
    clickable: (index: Int, music: Music) -> Unit = { _, _ -> }
) {
    Log.d("MainActivity", "Call once")
    if (available && songs.isNotEmpty()) {
        LazyColumn(
            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
        ) {
            itemsIndexed(songs) { index, music ->
                MusicItem(index, music, clickable)
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

@Preview(showBackground = true)
@Composable
fun MusicItem(
    index: Int = 0,
    music: Music = Music(-1, "Test", "N/A"),
    clickable: (index: Int, music: Music) -> Unit = { _, _ -> }
) {

    Log.d("SongListItem", "Call once")


    Row(
        modifier = Modifier
            .height(64.dp)
            .fillMaxWidth()
            .clickable { clickable.invoke(index, music) }
            .padding(top = 8.dp, bottom = 6.dp)
            .padding(start = 15.dp, end = 15.dp)
    ) {

        Surface(
            shape = MaterialTheme.shapes.medium,
            elevation = 4.dp
        ) {
            Image(
                painter = rememberImagePainter(
                    // 添加 250y250 参数限制宽高来优化加载大小,避免原图加载
                    data = music.getAlbumPicUrl(150, 150) ?: "",
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

                Text(
                    buildAnnotatedString {
                        append(music.artists)
                        append(" - ${music.song?.album?.name ?: music.id}   ")
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(2.8f)
                )

                if (music.incomplete) {
                    Text(
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.7f)
                    )
                }

                if (!NeteaseCacheProvider.fastReader && music.info == null) {
                    Text(
                        buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    //fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colors.error.copy(alpha = 0.6f)
                                )
                            ) {
                                append(getString(id = R.string.list_missing_info_file))
                                append(" ")
                                append(NeteaseCacheProvider.infoExt)
                            }
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.7f)
                    )
                }

                Text(
                    buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                //fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colors.primary.copy(alpha = 0.6f)
                            )
                        ) {
                            append(
                                when (music.bitrate) {
                                    1000 -> {
                                        "N/A kbps"
                                    }
                                    else -> "${music.bitrate / 1000} kbps"
                                }
                            )
                        }
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.7f)
                        .padding(bottom = 0.dp, end = 10.dp)
                )


            }


        }


    }
}