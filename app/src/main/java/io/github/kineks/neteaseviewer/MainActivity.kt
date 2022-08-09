package io.github.kineks.neteaseviewer

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.permissionx.guolindev.PermissionX
import io.github.kineks.neteaseviewer.data.setting.Setting
import io.github.kineks.neteaseviewer.ui.DefaultView
import io.github.kineks.neteaseviewer.ui.theme.NeteaseViewerTheme
import io.github.kineks.neteaseviewer.ui.view.activity
import io.github.kineks.neteaseviewer.ui.view.checkPermission
import io.github.kineks.neteaseviewer.ui.view.permissionX
import io.github.kineks.neteaseviewer.ui.welcome.WelcomeScreen


class MainActivity : FragmentActivity() {
    private val model: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionX = PermissionX.init(this)
        activity = this
        setContent {

            NeteaseViewerTheme {
                if (model.displayWelcomeScreen) {
                    WelcomeScreen(
                        callback = {
                            lifecycleScope.launchWhenCreated {
                                Setting.setFirstTimeLaunch(false)
                            }
                            model.displayWelcomeScreen = false
                        },
                        checkPermission = {
                            checkPermission { allGranted -> it(allGranted) }
                        },
                        display = model.displayWelcomeScreen
                    )
                } else DefaultView(model)

            }


        }
    }

    override fun onStart() {
        super.onStart()
        // 避免在进入引导页时先申请权限
        lifecycleScope.launchWhenStarted {
            Setting.firstTimeLaunch.collect { firstTimeLaunch ->
                model.displayPermissionDialog = !firstTimeLaunch
            }
        }
    }

}