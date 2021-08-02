package com.example.ocrscanner.db

import android.content.Context
import android.util.Log
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class HistoryPresenter(private val context: Context) {
    private var viewPresenter: HistoryViewPresenter? = null
    private var compositeDisposable: CompositeDisposable? = CompositeDisposable()

    fun attachView(view: HistoryViewPresenter) {
        viewPresenter = view
    }

    // --------------------------------------- HistoryDao ------------------------------------------

    fun getAllResults() {
        compositeDisposable?.add(ScannerDatabase.getInstance(context).resultDao().getAllResults()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .take(1)
            .subscribe(this::onReceiveResults) { error -> onError("getAllResults", error)})
    }

    fun insertResult(result: Result) {
        compositeDisposable?.add(Single.fromCallable { ScannerDatabase.getInstance(context).resultDao().insert(result) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::onInsertResult) { error -> onError("insertResult", error)})
    }

    fun deleteResultById(id: String) {
        compositeDisposable?.add(Single.fromCallable { ScannerDatabase.getInstance(context).resultDao().deleteById(id) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::onDeleteResult) { error -> onError("deleteResultById", error)})
    }


    // --------------------------------------- onSuccess -------------------------------------------

    private fun onReceiveResults(results: List<Result>) {
        viewPresenter?.showResults(results as ArrayList<Result>)
    }

    private fun onInsertResult(result: Long) {
        viewPresenter?.showResultInserted()
    }

    private fun onDeleteResult(result: Int) {
        viewPresenter?.showResultDeleted()
    }

    private fun onError(message: String, throwable: Throwable) {
        Log.d("ERRORLOG", "$message: ${throwable.message}")
        viewPresenter?.showError(throwable.message ?: "")
    }

    fun dispose() {
        compositeDisposable?.dispose()
    }
}