package io.github.kineks.neteaseviewer.ui.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.permissionx.guolindev.PermissionMediator
import io.github.kineks.neteaseviewer.App
import io.github.kineks.neteaseviewer.R
import io.github.kineks.neteaseviewer.data.local.fileUriUtils
import io.github.kineks.neteaseviewer.getString


@SuppressLint("StaticFieldLeak")
var permissionX: PermissionMediator? = null

@SuppressLint("StaticFieldLeak")
var activity: FragmentActivity? = null

@Composable
fun checkPermission(
    callback: (
        allGranted: Boolean
    ) -> Unit
) {
    PermissionComposeX(callback = callback)
}

@Composable
fun PermissionComposeX(

    callback: (
        allGranted: Boolean
    ) -> Unit,

    @SuppressLint("InlinedApi")
    permissions: List<String> =
        if (App.isAndroidRorAbove)
            listOf(
                //Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE
            )
        else
            listOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),

    requestDescription: String = stringResource(R.string.permission_request_description),

    request: (permissions: List<String>, function: (Boolean) -> Unit) -> Unit =
        { permissions: List<String>, function: (Boolean) -> Unit ->
            permissionX!!
                .permissions(permissions)
                .onExplainRequestReason { scope, deniedList ->
                    scope.showRequestReasonDialog(
                        deniedList, requestDescription,
                        getString(R.string.permission_allow),
                        getString(R.string.permission_deny)
                    )
                }
                .request { allGranted, _, _ ->
                    function(allGranted)
                }
        }


) {

    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
        callback(true)
        return
    }


    var whenPermissionDenied by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {
        App.context.apply {
            var allGranted = true
            permissions.forEach {
                if (allGranted) {
                    when (it) {
                        "RFile" -> {
                            if (!fileUriUtils.isGrant())
                                allGranted = false
                        }
                        Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                            if (!Environment.isExternalStorageManager())
                                allGranted = false
                        }
                        else -> if (checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED) {
                            allGranted = false
                        }
                    }

                }

            }
            if (allGranted) {
                whenPermissionDenied = false
                callback(true)
            }/* else {
            whenPermissionDenied = true
            callback(false)
        }*/
        }
    }



    if (whenPermissionDenied) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = requestDescription,
                style = MaterialTheme.typography.body2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = {
                request(permissions) {
                    val allGranted = it
                    callback(allGranted)
                    whenPermissionDenied = !allGranted
                }
            }) {
                Text(stringResource(id = R.string.permission_request_button))
            }
        }
    }
}

