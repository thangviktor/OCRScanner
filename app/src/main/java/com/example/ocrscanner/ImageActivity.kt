package com.example.ocrscanner

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_image.*

class ImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)

        val imageUrl = intent.getStringExtra("ImageUrl")

        Glide.with(this)
            .load(imageUrl)
            .into(ivResult)

        ivBack?.setOnClickListener { onBackPressed() }
    }
}