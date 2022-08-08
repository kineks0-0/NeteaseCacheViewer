package io.github.kineks.neteaseviewer.data.local

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import io.github.kineks.neteaseviewer.App
import io.github.kineks.neteaseviewer.ui.view.activity


object fileUriUtils {
    var root = Environment.getExternalStorageDirectory().path + "/"
    var isGrant by mutableStateOf(false)

    init {
        isGrant()
    }

    fun treeToPath(path: String): String {
        var path2: String
        if (path.contains("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary")) {
            path2 = path.replace(
                "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3A",
                root
            )
            path2 = path2.replace("%2F", "/")
        } else {
            TODO()
            /*path2 = root + "".substring(path + "测试", "document/primary%3A", "测试")
                .replace("%2F", "/")*/
        }
        return path2
    }

    //判断是否已经获取了Data权限，改改逻辑就能判断其他目录，懂得都懂
    fun isGrant(context: Context = App.context): Boolean {
        for (persistedUriPermission: UriPermission in context.contentResolver
            .persistedUriPermissions) {
            if (persistedUriPermission.isReadPermission && persistedUriPermission.uri.toString() == "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata") {
                isGrant = true
                return true
            }
        }
        isGrant = false
        return false
    }

    //直接返回DocumentFile
    fun getDocumentFilePath(
        context: Context = App.context,
        path: String,
        sdCardUri: String?
    ): DocumentFile? {
        var document = DocumentFile.fromTreeUri(context, Uri.parse(sdCardUri))
        val parts = path.split("/").toTypedArray()
        for (i in 3 until parts.size) {
            document = document!!.findFile(parts[i])
        }
        return document
    }

    //转换至uriTree的路径
    fun changeToUri(path: String): String {
        var path = path
        if (path.endsWith("/")) {
            path = path.substring(0, path.length - 1)
        }
        val path2 = path.replace("/storage/emulated/0/", "").replace("/", "%2F")
        return "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3A$path2"
    }

    //转换至uriTree的路径
    fun getDoucmentFile(context: Context = App.context, path: String): DocumentFile? {
        var path = path
        if (path.endsWith("/")) {
            path = path.substring(0, path.length - 1)
        }
        val path2 = path.replace("/storage/emulated/0/", "").replace("/", "%2F")
        return DocumentFile.fromSingleUri(
            context,
            Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3A$path2")
        )
    }

    //转换至uriTree的路径
    fun changeToUri2(path: String): String {
        val paths =
            path.replace("/storage/emulated/0/Android/data".toRegex(), "").split("/").toTypedArray()
        val stringBuilder =
            StringBuilder("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3AAndroid%2Fdata")
        for (p: String in paths) {
            if (p.length == 0) continue
            stringBuilder.append("%2F").append(p)
        }
        return stringBuilder.toString()
    }

    //转换至uriTree的路径
    fun changeToUri3(path: String): String {
        var path = path
        path = path.replace("/storage/emulated/0/", "").replace("/", "%2F")
        return "content://com.android.externalstorage.documents/tree/primary%3A$path"
    }

    //获取指定目录的权限
    fun startFor(
        path: String,
        context: Activity = activity!!,
        REQUEST_CODE_FOR_DIR: Int = App.REQUEST_CODE_FOR_DIR
    ) {
        //statusHolder.path = path
        val uri = changeToUri(path)
        val parse = Uri.parse(uri)
        val intent = Intent("android.intent.action.OPEN_DOCUMENT_TREE")
        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, parse)
        }
        context.startActivityForResult(intent, REQUEST_CODE_FOR_DIR)
    }

    //直接获取data权限，推荐使用这种方案
    fun startForRoot(context: FragmentActivity = activity!!, callback: () -> Unit) {
        AndroidDataFileUtils(activity = context, callback = callback).requestAndroidDateRootNow()
    }

}

class AndroidDataFileUtils(
    @SuppressLint("StaticFieldLeak")
    private val activity: FragmentActivity,
    @SuppressLint("StaticFieldLeak")
    private val fragment: Fragment? = null,
    private val callback: () -> Unit
) {

    companion object {
        private const val FRAGMENT_TAG = "FileUriFragment"
    }

    private val fragmentManager: FragmentManager
        get() {
            return fragment?.childFragmentManager ?: activity.supportFragmentManager
        }

    private val invisibleFragment: FileUriFragment
        get() {
            val existedFragment = fragmentManager.findFragmentByTag(Companion.FRAGMENT_TAG)
            return if (existedFragment != null) {
                existedFragment as FileUriFragment
            } else {
                val invisibleFragment = FileUriFragment(this)
                fragmentManager.beginTransaction()
                    .add(invisibleFragment, Companion.FRAGMENT_TAG)
                    .commitNowAllowingStateLoss()
                invisibleFragment
            }
        }

    fun requestAndroidDateRootNow() {
        invisibleFragment.requestNow()
    }

    fun removeInvisibleFragment() {
        callback()
        val existedFragment = fragmentManager.findFragmentByTag(Companion.FRAGMENT_TAG)
        if (existedFragment != null) {
            fragmentManager.beginTransaction().remove(existedFragment).commit()
        }
    }


}


class FileUriFragment(private val androidDataFileUtils: AndroidDataFileUtils) : Fragment() {

    private val requestDataLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val uri: Uri = result.data!!.data!!
                // 保存目录的访问权限
                App.context.contentResolver
                    .takePersistableUriPermission(
                        uri, result.data!!.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION
                                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    )
                fileUriUtils.isGrant()
                androidDataFileUtils.removeInvisibleFragment()
            }
        }

    fun requestNow(context: Activity = activity!!) {
        val uri =
            Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata")
        val documentFile = DocumentFile.fromTreeUri(context, uri)
        val intent1 = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent1.flags = (Intent.FLAG_GRANT_READ_URI_PERMISSION
                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        intent1.putExtra(DocumentsContract.EXTRA_INITIAL_URI, documentFile!!.uri)
        requestDataLauncher.launch(intent1)
    }


}


