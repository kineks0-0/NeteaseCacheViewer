package com.owo.recall.music.ui

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.owo.recall.music.R

class ViewPage2Apter(val viewList: ArrayList<View>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
    /*interface BindView {
        fun onBindView(holder: ViewHolder, position: Int , viewList: ArrayList<View>)
    }*/

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(viewList[viewType])
    }

    override fun getItemCount(): Int = viewList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        //bindView.onBindView(holder,position,viewList)
    }


}