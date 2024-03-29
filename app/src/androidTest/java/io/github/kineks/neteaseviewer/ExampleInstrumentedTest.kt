package io.github.kineks.neteaseviewer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.kineks.neteaseviewer.data.update.Update
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    fun checkUpdate() {
        GlobalScope.launch {
            Update.checkUpdate { json, hasUpdate ->
                println(json)
                println(hasUpdate)
            }
            println()
        }

    }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("io.github.kineks.neteaseviewer", appContext.packageName)


    }
}