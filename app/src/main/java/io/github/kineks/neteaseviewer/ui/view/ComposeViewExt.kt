package io.github.kineks.neteaseviewer.ui.view

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
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
    val list = model.songs.collectAsLazyPagingItems()

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
                        "总缓存文件数: " + list.itemCount
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

