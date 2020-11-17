package com.owo.recall.music.core

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment


class AvoidOnResultFragment : Fragment() {
    //private val mSubjects: MutableMap<Int, PublishSubject<ActivityResultInfo>> = HashMap()
    private val mCallbacks: MutableMap<Int, AvoidOnResult.Callback> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    /*fun startForResult(intent: Intent?, requestCode: Int): Observable<ActivityResultInfo> {
        val subject: PublishSubject<ActivityResultInfo> = PublishSubject.create()
        mSubjects[requestCode] = subject
        return subject.doOnSubscribe(object : Consumer<Disposable?>() {
            @Throws(Exception::class)
            fun accept(disposable: Disposable?) {
                startActivityForResult(intent, requestCode)
            }
        })
    }*/

    fun startForResult(intent: Intent?, requestCode: Int, callback: AvoidOnResult.Callback) {
        mCallbacks[requestCode] = callback
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //rxjava方式的处理
        /*val subject: PublishSubject<ActivityResultInfo>? = mSubjects.remove(requestCode)
        if (subject != null) {
            subject.onNext(ActivityResultInfo(requestCode, resultCode, data))
            subject.onComplete()
        }*/

        //callback方式的处理
        val callback = mCallbacks.remove(requestCode)
        callback?.onActivityResult(requestCode, resultCode, data)
    }
}