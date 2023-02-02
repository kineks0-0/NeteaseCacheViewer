package io.github.kineks.neteaseviewer.ui.play

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.lzx.starrysky.StarrySky
import io.github.kineks.neteaseviewer.MainViewModel
import io.github.kineks.neteaseviewer.data.local.cacheFile.EmptyMusicState
import io.github.kineks.neteaseviewer.data.local.cacheFile.MusicState
import io.github.kineks.neteaseviewer.ui.view.InfoBoxIcon


@Preview(showBackground = true)
@Composable
fun PreviewPlay() {
    PlayScreen(EmptyMusicState, false)
}

@Composable
fun PlayScreen(
    song: MusicState,
    isPlaying: Boolean = viewModel(modelClass = MainViewModel::class.java).isPlaying
) {

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
    ) {


        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .width(240.dp)
                .padding(top = 60.dp)
        ) {

            Surface(
                shape = MaterialTheme.shapes.small,
                elevation = 4.dp
            ) {
                Image(
                    painter = rememberAsyncImagePainter(song.getAlbumPicUrl(800, 800)),
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
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 3.dp)
            )
            Text(
                text = song.artists,
                style = MaterialTheme.typography.body2,
                maxLines = 1,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, start = 3.dp)
            )


            Spacer(
                Modifier
                    .padding(top = 2.dp)
                    .fillMaxHeight(0.25f)
            )

            InfoBoxIcon(
                icon = if (isPlaying)
                    Icons.Filled.Pause
                else
                    Icons.Filled.PlayArrow,
                color = MaterialTheme.colors.primary,
                modifier = Modifier
                    .padding(top = 8.dp, start = 1.dp),
                clickable = {
                    if (StarrySky
                            .with()
                            .isPlaying()
                    )
                        StarrySky
                            .with()
                            .pauseMusic()
                    else
                        StarrySky
                            .with()
                            .restoreMusic()
                }
            )

        }


    }


}

