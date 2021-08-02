package com.example.ocrscanner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.ocrscanner.db.Result
import com.example.ocrscanner.db.HistoryPresenter
import com.example.ocrscanner.db.HistoryViewPresenter
import kotlinx.android.synthetic.main.activity_history.*
import kotlinx.android.synthetic.main.item_result.view.*

class HistoryActivity : AppCompatActivity(), HistoryViewPresenter {

    private lateinit var presenter: HistoryPresenter

    private val results = ArrayList<Result>()
    private lateinit var adapter: ResultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        presenter = HistoryPresenter(this)
        presenter.attachView(this)
        presenter.getAllResults()

        adapter = ResultAdapter(this)
        lvHistory?.adapter = adapter

        lvHistory?.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, ResultActivity::class.java)
            intent.putExtra("HISTORY_RESULT", results[position])
            startActivity(intent)
        }

        lvHistory?.setOnItemLongClickListener { _, _, position, _ ->
            AlertDialog.Builder(this)
                .setMessage("Bạn có muốn xoá kết quả này ?")
                .setPositiveButton("Xoá") { _, _ ->
                    Log.d("HistoryLog", "history: ${results[position].content}")
                    presenter.deleteResultById(results[position].id)
                    results.removeAt(position)
                    adapter.notifyDataSetChanged()

                }
                .setNegativeButton("Huỷ") { _, _ ->

                }
                .create().show()
            return@setOnItemLongClickListener true
        }

        ivBack?.setOnClickListener { onBackPressed() }
    }

    override fun onDestroy() {
        presenter.dispose()
        super.onDestroy()
    }

    // --------------------------------- HistoryViewPresenter --------------------------------------

    override fun showResults(results: ArrayList<Result>) {
        this.results.clear()
        this.results.addAll(results)
        adapter.notifyDataSetChanged()
    }

    override fun showResultDeleted() {
        Log.d("HistoryLog", "Successfully deleted")
    }

    override fun showError(message: String) {
        Log.d("HistoryLog", "error: $message")
    }

    // ------------------------------------ Inner Class --------------------------------------------
    inner class ResultAdapter(private val context: Context) : BaseAdapter() {

        override fun getCount(): Int {
            return results.size
        }

        override fun getItem(position: Int): Any {
            return results[position]
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_result, parent, false)

            view.tvResult.text = results[position].content
            view.tvTime.text = results[position].time

            return view
        }
    }
}