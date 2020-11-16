package com.owo.recall.music.core.setting

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.owo.recall.music.R

class SettingListAdapter(val settingList: ArrayList<SettingItem>):RecyclerView.Adapter<SettingListAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: View = view//view.findViewById(R.id.imageView)
        val title: TextView = view.findViewById(R.id.textView)
        val onLitener: View = view
    }

    private fun onClick(it: View, position: Int, viewType: Int) {
        val settingItem: SettingItem = settingList[position]
        if (settingItem.itemType == SettingItem.TYPE_ITEM) {
            settingItem.keyValueEditor.onListener()
        }
    }

    private fun onLongClick(it: View, position: Int, viewType: Int) {

    }

    override fun getItemViewType(position: Int): Int {
        return settingList[position].itemType
    }

    override fun onCreateViewHolder( parent: ViewGroup, viewType: Int ): ViewHolder {
        val holder: ViewHolder =
        when(viewType) {
            SettingItem.TYPE_ITEM_HEAD -> { ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.recycler_setting_item_head_layout,parent,false))}
            SettingItem.TYPE_ITEM -> { ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.recycler_setting_item_layout,parent,false)) }
            SettingItem.TYPE_ITEM_NOTHING -> { ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.recycler_song_item_layout,parent,false)) }
            else -> { TODO() }
        }

        holder.itemView.setOnClickListener {
            val position: Int = holder.adapterPosition
            onClick(it,position, viewType)
        }

        holder.itemView.setOnLongClickListener {
            val position: Int = holder.adapterPosition
            onLongClick(it,position, viewType)
            true
        }

        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val settingItem: SettingItem = settingList[position]
        holder.title.text = settingItem.title
        if (settingItem.itemType == SettingItem.TYPE_ITEM) {
            holder.itemView.findViewById<TextView>(R.id.textView2).text = settingItem.summary
        }
    }

    override fun getItemCount(): Int {
        return settingList.size
    }



}