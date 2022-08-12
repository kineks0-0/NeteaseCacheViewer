package io.github.kineks.neteaseviewer.data.repository

class ResultBuilder<T> {
    var onSuccess: (data: T?) -> Unit = {}
    var onDataEmpty: () -> Unit = {}
    var onFailed: (errorCode: Int?, errorMsg: String?) -> Unit = { _, _ -> }
    var onError: (e: Throwable) -> Unit = { }
    var onComplete: () -> Unit = {}
}