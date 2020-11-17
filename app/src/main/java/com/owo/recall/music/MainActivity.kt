package com.owo.recall.music




import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.owo.recall.music.core.MusicFileProvider
import com.owo.recall.music.core.NeteaseMusicSong
import com.owo.recall.music.core.net.HttpUtil
import com.owo.recall.music.core.play.PlayUtil
import com.owo.recall.music.core.setting.SettingList
import com.owo.recall.music.core.setting.SettingListAdapter
import com.owo.recall.music.ui.NeteaseMusicSongAdapter
import com.owo.recall.music.ui.ViewPage2Apter
import com.owo.recall.music.ui.animations.DepthPageTransformer
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_layout.view.*
import kotlinx.android.synthetic.main.main_view_page_home.view.*
import kotlinx.android.synthetic.main.main_view_page_playing.view.*
import kotlinx.android.synthetic.main.main_view_page_setting.view.*
import okhttp3.Call
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.reflect.KProperty


class MainActivity : AppCompatActivity() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var ThisActivity: AppCompatActivity
    }

    private val coreRun: MusicFileProvider = MusicFileProvider
    private val songsList : ArrayList<NeteaseMusicSong> by lazy { coreRun.getMusicCacheFileList() }
    private val rdp by lazy { 10 }
    private val options: RequestOptions by lazy {
        RequestOptions
            .bitmapTransform(RoundedCorners(rdp))
            .placeholder(R.drawable.unknown)
            .error(R.drawable.unknown)
    }

    private val requestManager: RequestManager by lazy {
        Glide.with(this)
    }
    private var initED = false

    private val mainViewPageHome by lazy {
        val mainViewPageHome = LayoutInflater.from(this).inflate(R.layout.main_view_page_home,viewPage,false)
        mainViewPageHome.recyclerView.layoutManager = LinearLayoutManager(this)
        mainViewPageHome.recyclerView.setHasFixedSize(true)
        mainViewPageHome.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener(){

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when(newState) {
                    //由于用户的操作，屏幕产生惯性滑动，停止加载图片
                    RecyclerView.SCROLL_STATE_SETTLING -> Glide.with(this@MainActivity).pauseRequests()
                    //当屏幕滚动且用户使用的触碰或手指还在屏幕上，停止加载图片
                    RecyclerView.SCROLL_STATE_DRAGGING -> Glide.with(this@MainActivity).pauseRequests()

                    RecyclerView.SCROLL_STATE_IDLE -> Glide.with(this@MainActivity).resumeRequests()
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
            }
        })
        mainViewPageHome.swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimaryDark)
        mainViewPageHome.swipeRefreshLayout.setOnRefreshListener {
            refreshFruits(mainViewPageHome.recyclerView.adapter as NeteaseMusicSongAdapter)
        }
        mainViewPageHome
    }
    private val mainViewPagePlaying by lazy {
        val mainViewPagePlaying = LayoutInflater.from(this).inflate(R.layout.main_view_page_playing,viewPage,false)
        mainViewPagePlaying.playImageView2.setOnClickListener {
            PlayUtil.playMode.play()
            //if (PlayUtil.isPlaying) PlayUtil.playMode.pausePlay() else PlayUtil.playMode.continuesPlay()
        }
        mainViewPagePlaying.nextImageView3.setOnClickListener {
            PlayUtil.playMode.nextSong(true,0)
        }
        mainViewPagePlaying.perImageView4.setOnClickListener {
            PlayUtil.playMode.previousSong(true,0)
        }
        mainViewPagePlaying
    }
    private val mainViewPageSetting by lazy {
        val mainViewPageSetting = LayoutInflater.from(this).inflate(R.layout.main_view_page_setting,viewPage,false)
        mainViewPageSetting.recyclerView_setting.layoutManager = LinearLayoutManager(this)
        mainViewPageSetting.recyclerView_setting.setHasFixedSize(true)
        mainViewPageSetting
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ThisActivity = this




        //reportFullyDrawn()

    }


    override fun onStart() {
        super.onStart()

        if (initED) return
        //
        thread {

            //设置线程的优先级，不与主线程抢资源
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)

            nav_view.setOnNavigationItemSelectedListener { item ->
                when(item.itemId) {
                    R.id.navigation_home -> viewPage.currentItem = 0
                    R.id.navigation_playing -> viewPage.currentItem = 1
                    R.id.navigation_setting -> viewPage.currentItem = 2
                }
                true
            }

            val viewList: ArrayList<View> = ArrayList()
            viewList.add(mainViewPageHome)
            viewList.add(mainViewPagePlaying)
            viewList.add(mainViewPageSetting)
            val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    nav_view.menu.getItem(position).isChecked = true
                }
            }

            viewPage.setPageTransformer(DepthPageTransformer())
            viewPage.registerOnPageChangeCallback(onPageChangeCallback)

            runOnUiThread {
                //viewPage.setPageTransformer(ZoomOutPageTransformer())
                viewPage.adapter = ViewPage2Apter(viewList)
                //ArrayList()
            }

            PlayUtil.onPlayListener = object : PlayUtil.OnPlayListener {
                override fun onPlayBegins(song: NeteaseMusicSong, songList: ArrayList<NeteaseMusicSong>, index: Int ) {
                    runOnUiThread {
                        if (song.songInfo.id == -1L) {
                            coreRun.getNeteaseSongIDRawJSON(song.musicId,object : MusicFileProvider.RawJSONDataCallBack{
                                override fun callback(response: String, isCache: Boolean) {

                                    if (!coreRun.checkJSONisAvailable(response)) {
                                        runOnUiThread {
                                            mainViewPagePlaying.songTitleTextView.text = song.songFile.name
                                            mainViewPagePlaying.songTitle2TextView.text = getString(R.string.unknown)
                                            requestManager
                                                .load(R.drawable.unknown)
                                                .apply(options)
                                                .into(mainViewPagePlaying.settingImageView)
                                            mainViewPagePlaying.playImageView2.setImageResource(R.drawable.ic_pause_black_48dp)
                                        }
                                        return
                                    }

                                    //val iIndex = songList.indexOf(song)
                                    if (!isCache) {
                                        val thsSong = songList[songList.indexOf(song)]
                                        thsSong.songInfo = coreRun.getNeteaseSongInfo(response)
                                        mainViewPageHome.recyclerView.post {
                                            (mainViewPageHome.recyclerView.adapter as NeteaseMusicSongAdapter).update(thsSong,true)
                                        }
                                    }
                                    //PlayUtil.playMode.play(songList,index)
                                    runOnUiThread {
                                        mainViewPagePlaying.songTitleTextView.text = song.songInfo.name
                                        mainViewPagePlaying.songTitle2TextView.text = song.songInfo.getArtistsName()
                                        requestManager
                                            .load(song.songInfo.albums.picUrl)
                                            .apply(options)
                                            .into(mainViewPagePlaying.settingImageView)
                                        mainViewPagePlaying.playImageView2.setImageResource(R.drawable.ic_pause_black_48dp)
                                    }
                                }

                            })
                        } else {
                            mainViewPagePlaying.songTitleTextView.text = song.songInfo.name
                            mainViewPagePlaying.songTitle2TextView.text = song.songInfo.getArtistsName()
                            requestManager
                                .load(song.songInfo.albums.picUrl)
                                .apply(options)
                                .into(mainViewPagePlaying.settingImageView)
                            mainViewPagePlaying.playImageView2.setImageResource(R.drawable.ic_pause_black_48dp)
                        }
                    }

                }

                override fun onPlayStop() {
                    runOnUiThread {
                        mainViewPagePlaying.playImageView2.setImageResource(R.drawable.ic_play_arrow_black_48dp)
                    }
                }

                override fun onPlayEnd() {
                    runOnUiThread {
                        mainViewPagePlaying.playImageView2.setImageResource(R.drawable.ic_play_arrow_black_48dp)
                    }
                }

                override fun onPlayPause() {
                    runOnUiThread {
                        mainViewPagePlaying.playImageView2.setImageResource(R.drawable.ic_play_arrow_black_48dp)
                    }
                }

                override fun onPlayContinues() {
                    runOnUiThread {
                        mainViewPagePlaying.playImageView2.setImageResource(R.drawable.ic_pause_black_48dp)
                    }
                }

                override fun onRest() {
                    runOnUiThread {
                        mainViewPagePlaying.playImageView2.setImageResource(R.drawable.ic_play_arrow_black_48dp)
                    }
                }

                override fun onError() {
                    PlayUtil.playMode.nextSong(true,0)
                    runOnUiThread {
                        mainViewPagePlaying.playImageView2.setImageResource(R.drawable.ic_play_arrow_black_48dp)
                    }
                }

            }
        }
        thread {

            //设置线程的优先级，不与主线程抢资源
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)

            if (songsList.size == 0) {
                //songsList.clear()
                songsList.addAll(coreRun.getMusicCacheFileList())
            }
            val showMenu = object : NeteaseMusicSongAdapter.ShowPopupMenu{
                override fun showMenu(anchor: View, song: NeteaseMusicSong): Boolean {
                    return this@MainActivity.showMenu(song)
                }
            }
            runOnUiThread {
                mainViewPageHome.recyclerView.adapter = NeteaseMusicSongAdapter(songsList,coreRun,requestManager,options,showMenu)
                mainViewPageSetting.recyclerView_setting.adapter = SettingListAdapter(SettingList.MainSettingList)
            }
            PlayUtil.playMode.updata(songsList,-1)
            initED = true
        }

    }

    override fun onResume() {
        super.onResume()

        // 判断是否需要运行时申请权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 判断是否需要对用户进行提醒，用户点击过拒绝&&没有勾选不再提醒时进行提示
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // 给用于予以权限解释, 对于已经拒绝过的情况，先提示申请理由，再进行申请
                CoreApplication.toast("程序需要读写权限来读取缓存和导出歌曲")
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), CoreApplication.REQUEST_PERMISSION_CODE_WRITE_EXTERNAL_STORAGE)
            } else {
                // 无需说明理由的情况下，直接进行申请。如第一次使用该功能（第一次申请权限），用户拒绝权限并勾选了不再提醒
                // 将引导跳转设置操作放在请求结果回调中处理
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), CoreApplication.REQUEST_PERMISSION_CODE_WRITE_EXTERNAL_STORAGE)
            }
        }/* else {
            // 拥有权限直接进行功能调用
            //refreshFruits(mainViewPageHome.recyclerView.adapter as NeteaseMusicSongAdapter)
           val s = StringBuilder()
            MusicFileProvider.NeteaseMusicCacheFolder.listFiles().forEach {
                if (it!=null) s.append(it.name).append("\n")
            }
            CoreApplication.toast("Size: " + s)
        }*/

        thread {

            //设置线程的优先级，不与主线程抢资源
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            Thread.sleep(1500)

            //检查更新
            HttpUtil.checkUpdate("https://gitee.com/kineks0_0/Recall-Music/raw/master/NewUpdate.json",object : okhttp3.Callback{
                override fun onFailure(call: Call, e: IOException) {

                }

                override fun onResponse(call: Call, response: Response) {
                    val sResponse: String? = response.body?.string()
                    if (sResponse != null) {
                        val rootJSONObject = JSONObject(sResponse)

                        Log.d(this::class.toString(),sResponse)

                        val versionCode = rootJSONObject.getLong("code")
                        if (CoreApplication.getAppVersionCode() < versionCode) {
                            val versionCodeName = rootJSONObject.getString("codeName")
                            val updateText = rootJSONObject.getString("updateText")
                            val updateLink = rootJSONObject.getString("updateLink")
                            val downloadLink = rootJSONObject.getString("downloadLink")

                            val theTitle = CoreApplication.getAppVersionName() + " " + CoreApplication.getAppVersionCode() + " -> $versionCodeName $versionCode"
                            runOnUiThread {
                                AlertDialog.Builder(this@MainActivity,R.style.Theme_MaterialComponents_Light_Dialog_Alert).apply {
                                    setTitle("New Update!")
                                    setMessage("$theTitle \n  $updateText")
                                    setCancelable(true)
                                    setPositiveButton("Update") {_,_->
                                        CoreApplication.toast("TodoDownload: $updateLink")
                                        val intent = Intent(Intent.ACTION_VIEW)
                                        intent.data = Uri.parse(downloadLink)
                                        startActivity(intent)
                                    }
                                    setNegativeButton("OK") {_,_->}
                                    show()
                                }
                            }
                        }
                    }


                }

            })
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode) {
            CoreApplication.REQUEST_PERMISSION_CODE_WRITE_EXTERNAL_STORAGE -> {
                // 判断用户是否同意了请求
                if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    refreshFruits(mainViewPageHome.recyclerView.adapter as NeteaseMusicSongAdapter)
                } else {
                    // 未同意的情况
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.CALL_PHONE
                        )
                    ) {
                        // 给用于予以权限解释, 对于已经拒绝过的情况，先提示申请理由，再进行申请
                        CoreApplication.toast("程序需要读写权限来读取缓存和导出歌曲")
                    }/* else {
                        // 用户勾选了不再提醒，引导用户进入设置界面进行开启权限
                        /*Snackbar.make(view, "需要打开权限才能使用该功能，您也可以前往设置->应用。。。开启权限",
                            Snackbar.LENGTH_INDEFINITE)
                            .setAction("确定") {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.data = Uri.parse("package:$packageName")
                                startActivityForResult(intent,REQUEST_SETTINGS_CODE)
                            }
                            .show()*/
                    }*/
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId) {
            R.id.search -> CoreApplication.toast("Todo")

            R.id.all_update -> thread {
                CoreApplication.toast("开始获取中")
                var exitHere = false
                for ( i in songsList.indices ) {

                    if (exitHere) break
                    if (songsList[i].songInfo.id == -1L) {
                        //update
                        coreRun.getNeteaseSongIDRawJSON(songsList[i].musicId,object : MusicFileProvider.RawJSONDataCallBack{

                            override fun callback(response: String, isCache: Boolean) {

                                if (response == "{\"msg\":\"Cheating\",\"code\":-460,\"message\":\"Cheating\"}") {
                                    CoreApplication.toast("警告：被 NetEase Api 返回 Cheating ，请停止获取歌曲信息")
                                    exitHere = true
                                    return
                                }

                                if ( !coreRun.checkJSONisAvailable(response) ) {

                                    CoreApplication.toast("Wrong: CallBack Not Available")
                                    //Todo:对获取失败进行处理

                                } else {

                                    Log.e(this.javaClass.toString(),"警告：不稳定的操作，不应该对此外部操作 rawJSON：\n$response")

                                    runOnUiThread {
                                        songsList[i].songInfo = coreRun.getNeteaseSongInfo(response)
                                        (mainViewPageHome.recyclerView.adapter as NeteaseMusicSongAdapter).update(songsList[i],true)
                                    }

                                }

                            }


                        })

                        Thread.sleep(Random.nextLong(1000,4000))//延迟几秒 ,防止被判定爬取
                    }
                }
                CoreApplication.toast("已完成获取 " + songsList.size + " 个歌曲信息")
            }

            else -> CoreApplication.toast("Unknown")
        }
        return true
    }

    fun showMenu(song: NeteaseMusicSong): Boolean {
        /*val popup = PopupMenu(anchor.context, anchor)
        popup.menuInflater.inflate(R.menu.bottom_nav_menu, popup.menu)
        popup.show()

        // 通过上面这几行代码，就可以把控件显示出来了
        popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener {
            // 控件每一个item的点击事件
            true
        })
        popup.setOnDismissListener(PopupMenu.OnDismissListener {
            // 控件消失时的事件
        })*/
        val dialog = BottomSheetDialog(this@MainActivity)
        val view = View.inflate(this@MainActivity, R.layout.dialog_layout, null)

        view.line1.setOnClickListener {

            PlayUtil.getDecodeNeteaseFile(song,object : PlayUtil.DecodeCompleteCallBack {

                override fun completeCallBack(decodeFile: File,incompleteFile: Boolean) {

                    CoreApplication.toast("导出中")

                    if (incompleteFile) {
                        CoreApplication.toast("警告: 文件不完整,无法通过校对")
                        //todo 将硬编码文本转移string.xml 对不完整缓存采用联网下载
                    }
                    if (song.songInfo.id != -1L) {
                        val dir_Music: File = coreRun.DIR_Music//getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                        dir_Music.mkdirs()
                        val outFile = File(dir_Music,song.songInfo.name + " - " + song.songInfo.getArtistsName() + ".mp3")

                        //Todo: 对文件类型判断 对音乐写入标签信息
                        outFile.writeBytes(decodeFile.readBytes())

                        PlayUtil.scanFile(this@MainActivity,outFile.absolutePath)
                        CoreApplication.toast("已保存在: " + outFile.absolutePath)
                    } else {

                        CoreApplication.toast("正在下载歌曲信息")
                        coreRun.getNeteaseSongIDRawJSON(song.musicId,object : MusicFileProvider.RawJSONDataCallBack{
                            override fun callback(response: String, isCache: Boolean) {
                                it.post {
                                    if (!isCache) {
                                        song.songInfo = coreRun.getNeteaseSongInfo(response)
                                        it.post {
                                            (mainViewPageHome.recyclerView.adapter as NeteaseMusicSongAdapter).update(song)
                                        }
                                    }
                                    completeCallBack(decodeFile,incompleteFile)
                                }
                            }

                        })
                    }

                }
            })

            dialog.dismiss()
        }

        view.line2.setOnClickListener {
            dialog.dismiss()
            CoreApplication.toast("Todo")
        }

        view.line3.setOnClickListener {
            CoreApplication.toast("Todo")
        }

        dialog.setContentView(view)
        dialog.show()
        return true
    }

    private fun refreshFruits(adapter: NeteaseMusicSongAdapter) {
        thread {
            adapter.update(coreRun.getMusicCacheFileList())
            Thread.sleep(500)
            runOnUiThread {
                mainViewPageHome.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    /*
    override fun onStop() {
        super.onStop()
        PlayUtil.onStop()
        //Todo
    }*/
}

private operator fun Any.setValue(companion: MainActivity.Companion, property: KProperty<*>, appCompatActivity: AppCompatActivity) {

}

