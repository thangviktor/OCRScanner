package com.example.ocrscanner.db

interface HistoryViewPresenter {
    fun showResults(results: ArrayList<Result>) {}
    fun showResultInserted() {}
    fun showResultDeleted() {}

    fun showError(message: String)
}