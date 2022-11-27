package com.whiture.apps.tamil.thousand.nights

import android.content.Intent
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.whiture.apps.tamil.thousand.nights.models.BookData
import kotlinx.android.synthetic.main.activity_book_detail.*

class BookDetailActivity: AppCompatActivity() {

    private lateinit var app: App

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_detail)

        app = application as App

        // turns off night mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // prepare toolbar
        bookDetailToolbar.title = ""
        val backArrow = ContextCompat.getDrawable(this, R.drawable.btn_back_material)
        val accentColor = ContextCompat.getColor(this, R.color.colorAccent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backArrow?.colorFilter = BlendModeColorFilter(accentColor, BlendMode.SRC_ATOP)
        }
        else {
            backArrow?.setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP)
        }
        bookDetailToolbar.navigationIcon = backArrow
        bookDetailToolbar.setOnClickListener {
            finish() // finish it and go back
        }

        showBanner(bookDetailAdBanner) // show the banner ad

        getSerializable("book", BookData::class.java).let { data ->
            bookDetailTitleTxt.text = data.title
            bookDetailAuthorTxt.text = data.author
            bookDetailPublisherTxt.text = "பதிவு: ${data.published}"
            bookDetailDescTxt.text = data.desc
            bookDetailKeywordTxt.text = data.tags.joinToString(", ")
            bookDetailCommentsTxt.text = data.comments.joinToString(", ")
            bookDetailSizeTxt.text = "${data.size}KB"
            showImage("${AppConstants.ContentURL}/books/store/tamil/${data.id}.png",
                bookDetailCoverImg)
            bookDetailDownloadBtn.setOnClickListener {
                // start the zip file downloader
                downloadZipFile("${data.id}.zip", "${data.id}",
                    data.size * 1024, getString(R.string.please_wait),
                    getString(R.string.book_download_message),
                    "${AppConstants.ContentURL}/books/store/tamil/${data.id}.zip",
                    onSuccess = {
                        bookDetailDownloadBtn.text = "COMPLETED"
                        bookDetailDownloadBtn.isEnabled = false
                        app.bookDownloaded(data.id)
                        showMessage("பதிவிறக்கம் முடிந்தது",
                            "புத்தக பதிவிறக்கம் வெற்றிகரமாக நிறைவுற்றது.") {
                            startActivity(Intent(this@BookDetailActivity,
                                BookReaderActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra("bookId", data.id) })
                        }
                    }, onFailure = { })
            }
        }
    }

}

