package io.github.kineks.neteaseviewer.ui.view

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.AlertDialog
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.kineks.neteaseviewer.App
import io.github.kineks.neteaseviewer.MainViewModel
import io.github.kineks.neteaseviewer.data.local.NeteaseCacheProvider
import io.github.kineks.neteaseviewer.ui.home.working

@Composable
fun CheckUpdate(model: MainViewModel) {

    if (model.hasAppUpdate) {
        AlertDialog(
            onDismissRequest = { model.hasAppUpdate = false },
            title = { Text("发现新版本[" + model.updateAppJSON.versionName + "]") },
            text = {
                Column(
                    modifier = Modifier
                        .padding(2.dp)
                        .fillMaxWidth()
                ) {
                    Text(text = model.updateAppJSON.updateInfo)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        model.hasAppUpdate = false
                        val uri: Uri = Uri.parse(model.updateAppJSON.updateLink)
                        val intent = Intent()
                        intent.action =
                            "android.intent.action.VIEW"
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.data = uri
                        App.context.startActivity(intent)
                    }
                ) {
                    Text("更新")
                }
            },
            dismissButton = {
                TextButton(onClick = { model.hasAppUpdate = false }) { Text("取消") }
            }
        )
    }

}

@Composable
fun SaveFilesAlertDialog(
    model: MainViewModel,
    openDialog: Boolean,
    onValueChange: (Boolean) -> Unit,
    snackbar: (message: String) -> Unit
) {
    var skipIncomplete by remember { mutableStateOf(true) }
    var skipMissingInfo by remember { mutableStateOf(true) }

    if (openDialog) {
        AlertDialog(
            onDismissRequest = { onValueChange.invoke(false) },
            title = {
                Text(text = "批量导出缓存文件")
            },
            text = {
                Column(
                    modifier = Modifier
                        .padding(2.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        "总缓存文件数: " + model.songs.size
                    )
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = skipIncomplete,
                                onCheckedChange = { skipIncomplete = it }
                            )
                            Text("跳过不完整缓存文件", textAlign = TextAlign.Start)
                        }

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = skipMissingInfo,
                                onCheckedChange = { skipMissingInfo = it }
                            )
                            Text(
                                "跳过丢失info文件缓存",
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }


            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValueChange.invoke(false)
                        working = true
                        NeteaseCacheProvider.decryptSongList(
                            model.songs, skipIncomplete, skipMissingInfo,
                            callback = { out, hasError, e ->
                                if (hasError) {
                                    Log.e("decrypt songs", e?.message, e)
                                    snackbar.invoke("导出歌曲失败 : ${e?.message} ${out?.toString()}")
                                }
                            },
                            isLastOne = { working = false }
                        )
                    }
                ) {
                    Text("开始导出")
                }
            },
            dismissButton = {
                TextButton(onClick = { onValueChange.invoke(false) }) { Text("取消") }
            }
        )
    }
}


@Composable
fun InfoBoxIcon(
    clickable: () -> Unit = {},
    color: Color = Color.Unspecified,
    icon: ImageVector,
    contentDescription: String = "",
    elevation: Dp = 0.dp,
    width: Dp = 50.dp,
    height: Dp = 50.dp,
    modifier: Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium, elevation = elevation,
        color = color.copy(alpha = 0.1f),
        modifier = modifier
            .size(width = width, height = height)
            .fillMaxHeight()
            .padding(start = 1.dp, end = 2.dp)
            .clickable { clickable() }
    ) {
        Box(
            modifier = Modifier
                .size(width)
                .fillMaxHeight()
                .padding(start = 1.dp, end = 1.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = color
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

@Composable
fun InfoBoxText(
    text: String,
    //modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = TextAlign.Center,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = 1,
    style: TextStyle = MaterialTheme.typography.caption
) {
    Surface(
        shape = MaterialTheme.shapes.medium, elevation = 0.dp,
        color = color.copy(alpha = 0.1f),
        modifier = Modifier
            .size(width = 60.dp, 25.dp)
            .fillMaxHeight()
            .padding(start = 1.dp, end = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(55.dp)
                .fillMaxHeight()
                .padding(start = 1.dp, end = 1.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                /*modifier = modifier
                    .size(50.dp)
                    .fillMaxHeight(),*/
                color = color,
                fontWeight = fontWeight,
                textAlign = textAlign,
                overflow = overflow,
                maxLines = maxLines,
                style = style
            )
        }

    }

}
