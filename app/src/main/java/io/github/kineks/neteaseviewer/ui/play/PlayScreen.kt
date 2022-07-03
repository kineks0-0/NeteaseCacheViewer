package io.github.kineks.neteaseviewer.ui.play

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import io.github.kineks.neteaseviewer.data.local.cacheFile.EmptyMusicState
import io.github.kineks.neteaseviewer.data.local.cacheFile.MusicState


@Preview(showBackground = true)
@Composable
fun PreviewPlay() {
    PlayScreen(EmptyMusicState)
}

@Composable
fun PlayScreen(song: MusicState) {

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
                    painter = rememberAsyncImagePainter(song.getAlbumPicUrl(500, 500)),
                    contentDescription = "Song Album Art",
                    modifier = Modifier
                        .size(240.dp)
                        .background(
                            MaterialTheme.colors.onBackground
                                .copy(alpha = 0.5f)
                        )
                )
            }

            Text(
                text = song.name,
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp, start = 3.dp)
            )
            Text(
                text = song.artists,
                style = MaterialTheme.typography.body2,
                maxLines = 1,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, start = 3.dp)
            )
        }


    }


}