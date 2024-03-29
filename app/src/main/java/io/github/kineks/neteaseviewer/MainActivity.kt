package io.github.kineks.neteaseviewer

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.permissionx.guolindev.PermissionX
import io.github.kineks.neteaseviewer.ui.DefaultView
import io.github.kineks.neteaseviewer.ui.home.working
import io.github.kineks.neteaseviewer.ui.theme.NeteaseViewerTheme
import io.github.kineks.neteaseviewer.ui.view.CheckPermission
import io.github.kineks.neteaseviewer.ui.view.permissionX
import io.github.kineks.neteaseviewer.ui.welcome.WelcomeScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : FragmentActivity() {
    private val model: MainViewModel by viewModels()

    companion object {
        @SuppressLint("StaticFieldLeak")
        var activity: MainActivity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            permissionX = PermissionX.init(this@MainActivity)
            activity = this@MainActivity
        }

        setContent {

            NeteaseViewerTheme {
                if (model.displayWelcomeScreen) {
                    WelcomeScreen(
                        callback = {
                            lifecycleScope.launch {
                                repeatOnLifecycle(Lifecycle.State.STARTED) {
                                    withContext(Dispatchers.IO) {
                                        model.displayPermissionDialog = false
                                        if (model.songs.isEmpty())
                                            model.initList(
                                                init = false,
                                                updateInfo = true,
                                                callback = {
                                                    working = false
                                                })
                                    }
                                }
                            }
                            model.displayWelcomeScreen = false
                        },
                        checkPermission = {
                            CheckPermission { allGranted -> it(allGranted) }
                        },
                        display = model.displayWelcomeScreen
                    )
                } else DefaultView()

            }

        }

    }

    override fun onStart() {
        super.onStart()
        // 避免在进入引导页时先申请权限
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                // 注: 该函数仅在第一次调用会重新加载数据
                // 重载数据请用 model.refresh()
                model.initList(updateInfo = true, callback = {
                    working = false
                })
            }
        }
    }

    override fun onDestroy() {
        permissionX = null
        activity = null
        super.onDestroy()
    }

}