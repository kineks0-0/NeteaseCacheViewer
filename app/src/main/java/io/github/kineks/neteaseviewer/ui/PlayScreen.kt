package io.github.kineks.neteaseviewer.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import io.github.kineks.neteaseviewer.R
import io.github.kineks.neteaseviewer.data.local.EmptyMusic
import io.github.kineks.neteaseviewer.data.local.Music


@Preview(showBackground = true)
@Composable
fun PreviewPlay() {
    PlayScreen(EmptyMusic)
}

const val TAG = "PlayScreen"
@Composable
fun PlayScreen(music: Music? = null) {

    val song = music ?: EmptyMusic

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .offset(y = (-95).dp)
    ) {


        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.width(240.dp)
        ) {

            Surface(
                shape = MaterialTheme.shapes.small,
                elevation = 4.dp
            ) {
                Image(
                    painter = rememberImagePainter(
                        data = song.getAlbumPicUrl(500,500) ?: "",
                        builder = { crossfade(true) }
                    ),
                    contentDescription = "Song Album Art",
                    modifier = Modifier
                        .size(240.dp)
                        .background(
                            MaterialTheme.colors.onBackground
                                .copy(alpha = 0.5f))
                )
                Log.d(TAG,"Song Album Art : " + song.getAlbumPicUrl(700,700))
            }

            Text(
                text = song.name,
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp,start = 3.dp)
            )
            Text(
                text = song.artists,
                style = MaterialTheme.typography.body2,
                maxLines = 1,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp,start = 3.dp)
            )
        }


    }


}