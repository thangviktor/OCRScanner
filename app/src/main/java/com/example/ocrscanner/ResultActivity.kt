package com.example.ocrscanner

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.bumptech.glide.Glide
import com.example.ocrscanner.db.HistoryPresenter
import com.example.ocrscanner.db.Result
import com.example.ocrscanner.db.HistoryViewPresenter
import kotlinx.android.synthetic.main.activity_result.*

class ResultActivity : AppCompatActivity(), HistoryViewPresenter {

    private lateinit var presenter: HistoryPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        presenter = HistoryPresenter(this)
        presenter.attachView(this)

        val directResult = intent.getParcelableExtra<Result>("RESULT")
        val historyResult = intent.getParcelableExtra<Result>("HISTORY_RESULT")
        val result = directResult?: historyResult
        directResult?.let { presenter.insertResult(it) }

        tvResult?.text = result?.content
        Glide.with(this)
            .load(result?.pathUrl)
            .into(ivResult)

        ivBack?.setOnClickListener { onBackPressed() }
        ivResult?.setOnClickListener {
            val intent = Intent(this, ImageActivity::class.java)
            intent.putExtra("ImageUrl", result?.pathUrl)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        presenter.dispose()
        super.onDestroy()
    }

    override fun showResultInserted() {
        Log.d("HistoryLog", "Successfully inserted")
    }

    override fun showError(message: String) {
        Log.d("HistoryLog", "error: $message")
    }
}