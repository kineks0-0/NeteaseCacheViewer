package com.owo.recall.music.core




import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment


class AvoidOnResult(activity: AppCompatActivity) {
    private var mAvoidOnResultFragment: AvoidOnResultFragment = getAvoidOnResultFragment(activity)

    constructor(fragment: Fragment) : this(fragment.activity as AppCompatActivity) {}

    private fun getAvoidOnResultFragment(activity: AppCompatActivity): AvoidOnResultFragment {
        var avoidOnResultFragment: AvoidOnResultFragment? = findAvoidOnResultFragment(activity)
        if (avoidOnResultFragment == null) {
            avoidOnResultFragment = AvoidOnResultFragment()
            avoidOnResultFragment.let{
                val fragmentManager = activity.supportFragmentManager
                fragmentManager
                    .beginTransaction()
                    .add((it as Fragment), TAG)
                    .commitAllowingStateLoss()
                fragmentManager.executePendingTransactions()
            }
        }
        return avoidOnResultFragment
    }

    private fun findAvoidOnResultFragment(activity: AppCompatActivity): AvoidOnResultFragment? {
        activity.supportFragmentManager.findFragmentByTag(TAG)?.let {
            return it as AvoidOnResultFragment
        }
        return null
    }

    /*fun startForResult(intent: Intent?, requestCode: Int): Observable<ActivityResultInfo> {
        return mAvoidOnResultFragment.startForResult(intent, requestCode)
    }*/

    /*fun startForResult(clazz: Class<*>?, requestCode: Int): Observable<ActivityResultInfo> {
        val intent = Intent(mAvoidOnResultFragment.getActivity(), clazz)
        return startForResult(intent, requestCode)
    }*/

    fun startForResult(intent: Intent, requestCode: Int, callback: Callback) {
        mAvoidOnResultFragment.startForResult(intent, requestCode, callback)
    }

    fun startForResult(clazz: Class<*>, requestCode: Int, callback: Callback) {
        val intent = Intent(mAvoidOnResultFragment.activity, clazz)
        startForResult(intent, requestCode, callback)
    }

    interface Callback {
        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    }

    companion object {
        private const val TAG = "AvoidOnResult"
    }

}