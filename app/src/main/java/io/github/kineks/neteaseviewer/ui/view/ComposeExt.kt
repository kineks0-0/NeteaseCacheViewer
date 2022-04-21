package io.github.kineks.neteaseviewer.ui.view

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.kineks.neteaseviewer.App
import io.github.kineks.neteaseviewer.MainViewModel
import io.github.kineks.neteaseviewer.data.local.NeteaseCacheProvider
import io.github.kineks.neteaseviewer.ui.home.working

@Composable
fun CheckUpdate(model: MainViewModel) {

    if (model.hasUpdateApp) {
        AlertDialog(
            onDismissRequest = { model.hasUpdateApp = false },
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
                        model.hasUpdateApp = false
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
                TextButton(onClick = { model.hasUpdateApp = false }) { Text("取消") }
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
fun BottomNavigation(
    selectedItem: MutableState<Int>,
    navItemList: List<String>,
    whenIndexChange: (Int) -> Unit,
    backgroundColor: Color = MaterialTheme.colors.background
) {
    BottomNavigation(backgroundColor = backgroundColor) {
        for (index in navItemList.indices) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clickable(
                        onClick = {
                            whenIndexChange(index)
                        },
                        indication = null,
                        interactionSource = MutableInteractionSource()
                    ),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NavigationIcon(index, selectedItem.value)
                Spacer(Modifier.padding(top = 2.dp))
                AnimatedVisibility(visible = index == selectedItem.value) {
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier.size(5.dp),
                        color = MaterialTheme.colors.onSurface
                    ) { }
                }
            }
        }
    }
}

@Composable
private fun NavigationIcon(
    index: Int,
    selectedItem: Int
) {
    val alpha = if (selectedItem != index) 0.5f else 1f
    CompositionLocalProvider(LocalContentAlpha provides alpha) {
        when (index) {
            0 -> Icon(Icons.Outlined.Home, contentDescription = null)
            1 -> Icon(Icons.Outlined.MusicNote, contentDescription = null)
            else -> Icon(Icons.Outlined.Settings, contentDescription = null)
        }
    }
}