package com.owo.recall.music.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import com.owo.recall.music.CoreApplication
import com.owo.recall.music.R
import com.owo.recall.music.core.MusicFileProvider
import com.owo.recall.music.core.NeteaseMusicSong
import com.owo.recall.music.core.play.PlayUtil
import com.owo.recall.music.toast


class NeteaseMusicSongAdapter(
    val songList: ArrayList<NeteaseMusicSong>,
    val coreRun: MusicFileProvider,
    private val requestManager: RequestManager,
    private val options: RequestOptions,

    private val showPopupMenu: ShowPopupMenu? = null ) :

    RecyclerView.Adapter<NeteaseMusicSongAdapter.ViewHolder>() {



    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val title: TextView = view.findViewById(R.id.textView)
        val text2: TextView = view.findViewById(R.id.textView2)
        val text3: TextView = view.findViewById(R.id.textView3)
        val text4: TextView = view.findViewById(R.id.textView4)
    }

    interface ShowPopupMenu {
        fun showMenu(anchor: View, song: NeteaseMusicSong): Boolean
    }


    private fun onClick(it: View, position: Int) {

        val songInfo: NeteaseMusicSong = songList[position]
        val songID: Long = songInfo.musicId

        /*if (songInfo.songInfo.id != -1L) {
            return@setOnClickListener
        }*/

        coreRun.getNeteaseSongIDRawJSON(songID,object : MusicFileProvider.RawJSONDataCallBack{
            override fun callback(response: String, isCache: Boolean) {

                Log.e(this.javaClass.toString(),"警告：不稳定的操作，不应该对此外部操作 rawJSON：\n$response")

                if ( !coreRun.checkJSONisAvailable(response) ) {
                    PlayUtil.playMode.play(songList,position)
                    if (response == "{\"msg\":\"Cheating\",\"code\":-460,\"message\":\"Cheating\"}") {
                        toast("警告：被 NetEase Api 返回 Cheating ，请停止获取歌曲信息")
                    }
                    //CoreApplication.toast("Wrong: Null CallBack")
                    //Todo:对获取失败进行处理

                } else {

                    //songList[position].songInfo = coreRun.getNeteaseSongInfo(response)

                    it.post {
                        if (!isCache) {
                            songList[position].songInfo = coreRun.getNeteaseSongInfo(response)
                            it.post {
                                this@NeteaseMusicSongAdapter.notifyItemChanged(position)
                            }
                        }
                        PlayUtil.playMode.play(songList,position)
                    }

                }

            }

        })

        toast("Playing " + songInfo.songInfo.name)
    }

    private fun onLongClick(it: View, position: Int) {
        val songInfo: NeteaseMusicSong = songList[position]
        val songID: Long = songInfo.musicId

        if (showPopupMenu == null) {
            coreRun.getNeteaseSongIDRawJSON(songID,object : MusicFileProvider.RawJSONDataCallBack{
                override fun callback(response: String, isCache: Boolean) {
                    it.post {
                        AlertDialog.Builder(it.context).apply {
                            setTitle("ViewData")
                            setMessage(response)
                            setCancelable(true)
                            setPositiveButton("Copy") {_,_->


                                //获取剪贴板管理器：
                                val cm: ClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager // 创建普通字符型ClipData
                                val mClipData = ClipData.newPlainText("ThisLabel", response) // 将ClipData内容放到系统剪贴板里。
                                cm.setPrimaryClip(mClipData)
                            }
                            setNegativeButton("OK") {_,_->}
                            show()
                        }
                    }
                }

            })
        } else {
            showPopupMenu.showMenu(it,songInfo)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val holder = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.recycler_song_item_layout,parent,false))

        holder.itemView.setOnClickListener {
            val position: Int = holder.adapterPosition
            onClick(it,position)
        }

        holder.itemView.setOnLongClickListener {
            val position: Int = holder.adapterPosition
            onLongClick(it,position)
            true
        }

        return holder
    }


    override fun getItemCount(): Int = songList.size



    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val songInfo: NeteaseMusicSong = songList[position]
        if (songInfo.songInfo.id != -1L) {

            holder.title.text = songInfo.songInfo.name
            holder.text2.text = songInfo.songInfo.getArtistsName() + " - " + songInfo.songInfo.name
            holder.text4.text = coreRun.ms2String(songInfo.duration)
            val kbps: Long = songInfo.bitrate/1000L
            if (kbps < 10L) holder.text3.text = "试听" else holder.text3.text = "$kbps kbps"

            requestManager
                .load(songInfo.songInfo.albums.picUrl)
                .apply(options)
                .into(holder.imageView)
        } else {
            holder.title.text = songInfo.songFile.name
            holder.text2.text = songInfo.musicId.toString()
            holder.text4.text = coreRun.ms2String(songInfo.duration)
            val kbps: Long = songInfo.bitrate/1000L
            if (kbps < 10L) holder.text3.text = "试听" else holder.text3.text = "$kbps kbps"

            requestManager
                .load(R.drawable.unknown)
                .apply(options)
                .into(holder.imageView)
        }
    }

    fun update(list: ArrayList<NeteaseMusicSong>) {
        CoreApplication.post {
            songList.clear()
            songList.addAll(list)
            notifyDataSetChanged()
        }
    }

    fun update(song: NeteaseMusicSong, update: Boolean = false, index: Int = songList.indexOf(song)){
        if (index < 0 || index > songList.lastIndex) return
        if (songList[index] != song || update) {
            CoreApplication.post {
                songList[index] = song
                notifyItemChanged(index)
            }
        }
    }

}