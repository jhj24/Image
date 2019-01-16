package com.jhj.image

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.jhj.imageselector.ImageConfig
import com.jhj.imageselector.ImageModel

import com.jhj.imageselector.ImageViewPagerActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val list = arrayListOf<ImageModel>(
                ImageModel("http://47.94.173.253:8008/image/20181124/201811240513458764headimage.png"),
                ImageModel("http://47.94.173.253:8008/image/20180420/15286837836/20180420035517headimage.png"),
                ImageModel("http://47.94.173.253:8008/image/20180404/13926590001/20180404123347headimage.png"),
                ImageModel("http://47.94.173.253:8008/image/com/59/1804113524logo.png"),
                ImageModel("http://47.94.173.253:8008/image/20180404/13914071928/20180404094728headimage.png")
        )

        textView.setOnClickListener {
            val intent = Intent(this, ImageViewPagerActivity::class.java)
            intent.putExtra(ImageConfig.IMAGE_IS_EDITABLE, true)
            intent.putExtra(ImageConfig.IMAGE_LIST, list)
            startActivity(intent)
        }
    }
}
