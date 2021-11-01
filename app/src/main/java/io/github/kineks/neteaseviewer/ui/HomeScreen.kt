package io.github.kineks.neteaseviewer.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
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
import io.github.kineks.neteaseviewer.R
import io.github.kineks.neteaseviewer.data.local.Music
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(songs: List<Music>, scope: CoroutineScope, scaffoldState: ScaffoldState) {

    SongsList(
        songs = songs,
        clickable = { index, song ->

            scope.launch {
                val result = scaffoldState.snackbarHostState
                    .showSnackbar(
                        message = "$index  ${song.name}",
                        actionLabel = "Dismissed",
                        duration = SnackbarDuration.Long
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



@Composable
fun SongsList(
    songs: List<Music>,
    available: Boolean = true,
    clickable: (index: Int, music: Music) -> Unit = { _, _ -> }
) {
    Log.d("MainActivity", "Call once")
    if (available && songs.isNotEmpty()) {
        LazyColumn {
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

    Surface(
        shape = MaterialTheme.shapes.large,
        elevation = 1.dp,
        modifier = Modifier
            .padding(start = 8.dp, top = 10.dp, end = 8.dp, bottom = 6.dp)
            .clickable { clickable.invoke(index, music) }
    ) {

        Row(modifier = Modifier.height(60.dp)) {

            Surface(
                shape = MaterialTheme.shapes.medium
            ) {
                Image(
                    painter = rememberImagePainter(
                        data = music.song?.album?.picUrl ?: "",
                        builder = {
                            crossfade(true)
                        }
                    ),
                    contentDescription = "Song Album Art",
                    modifier = Modifier
                        .size(60.dp)
                        .background(MaterialTheme.colors.onBackground.copy(alpha = 0.6f))
                )
            }


            Column {

                //val name = "$index ${music.name}"
                Text(
                    //text = name,
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
                        .padding(start = 10.dp, top = 8.dp, bottom = 2.dp, end = 10.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(start = 12.dp, top = 2.dp, bottom = 2.dp, end = 10.dp)
                ) {

                    Text(
                        //text = music.artists,
                        buildAnnotatedString {
                            append(music.artists)
                            append(" - ${music.song?.album?.name ?: music.id}   ")
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier
                            //.fillMaxWidth()
                            .weight(2f)
                            //.padding(start = 12.dp, top = 2.dp, bottom = 2.dp, end = 10.dp)
                            .alpha(0.9f)
                    )

                    Text(
                        //text = music.artists,
                        buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colors.primary.copy(alpha = 0.5f)
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
                            //.fillMaxWidth()
                            .weight(1f)
                            .padding(start = 12.dp, top = 2.dp, bottom = 2.dp, end = 10.dp)
                            .alpha(0.9f)
                    )
                }


            }
        }

    }
}