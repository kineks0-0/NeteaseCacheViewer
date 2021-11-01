package io.github.kineks.neteaseviewer.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import io.github.kineks.neteaseviewer.R
import io.github.kineks.neteaseviewer.data.local.Music


@Preview(showBackground = true)
@Composable
fun PreviewPlay() {
    PlayScreen(Music(-1,"Name","N/A",-1))
}

@Composable
fun PlayScreen(music: Music? = null) {


    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {

        if (music == null) {
            Text(
                text = stringResource(id = R.string.list_no_data),
                textAlign = TextAlign.Center
            )
        } else {
            Surface(
                shape = MaterialTheme.shapes.medium
            ) {
                Image(
                    painter = rememberImagePainter(
                        data = music.getAlbumPicUrl() ?: "",
                        builder = { crossfade(true) }
                    ),
                    contentDescription = "Song Album Art",
                    modifier = Modifier
                        .size(250.dp)
                        .background(MaterialTheme.colors.onBackground.copy(alpha = 0.6f))
                        .offset(y = (80).dp)
                )
            }
        }

    }

}