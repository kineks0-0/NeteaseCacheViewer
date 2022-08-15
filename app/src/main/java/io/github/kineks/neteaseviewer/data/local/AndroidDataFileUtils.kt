package io.github.kineks.neteaseviewer.data.local

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import io.github.kineks.neteaseviewer.App
import io.github.kineks.neteaseviewer.MainActivity.Companion.activity


object FileUriUtils {
    var isGrant by mutableStateOf(false)

    init {
        isGrant()
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

    //直接获取data权限，推荐使用这种方案
    @RequiresApi(Build.VERSION_CODES.R)
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
            val existedFragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG)
            return if (existedFragment != null) {
                existedFragment as FileUriFragment
            } else {
                val invisibleFragment = FileUriFragment(this)
                fragmentManager.beginTransaction()
                    .add(invisibleFragment, FRAGMENT_TAG)
                    .commitNowAllowingStateLoss()
                invisibleFragment
            }
        }

    @RequiresApi(Build.VERSION_CODES.R)
    fun requestAndroidDateRootNow() {
        invisibleFragment.requestNow()
    }

    fun removeInvisibleFragment() {
        callback()
        val existedFragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG)
        if (existedFragment != null) {
            fragmentManager.beginTransaction().remove(existedFragment).commit()
        }
    }


}


class FileUriFragment(private val androidDataFileUtils: AndroidDataFileUtils) : Fragment() {

    private val requestDataLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                if (result.data?.data != null && result.data?.flags != null) {
                    val uri: Uri = result.data!!.data!!
                    // 保存目录的访问权限
                    App.context.contentResolver
                        .takePersistableUriPermission(
                            uri, result.data!!.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        )
                    FileUriUtils.isGrant()
                }
                androidDataFileUtils.removeInvisibleFragment()
            }
        }

    @RequiresApi(Build.VERSION_CODES.R)
    fun requestNow(context: Activity = requireActivity()) {
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


