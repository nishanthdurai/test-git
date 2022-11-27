package com.whiture.apps.tamil.thousand.nights

import android.content.Intent
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdRequest
import com.whiture.apps.tamil.thousand.nights.models.BookData
import com.whiture.apps.tamil.thousand.nights.models.StoreData
import kotlinx.android.synthetic.main.activity_store_search.*

class StoreSearchActivity: AppCompatActivity() {
    private lateinit var app: App

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store_search)

        // app preparation
        app = application as App
        // turns off night mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // load Admob ad request
        adBannerStoreSearch.loadAd(AdRequest.Builder().build())

        intent.getStringExtra("keyword")?.let { keyword ->
            // prepare toolbar
            val accentColor = ContextCompat.getColor(this, R.color.colorAccent)
            searchToolbar.apply {
                title = "Results for '$keyword'"
                setTitleTextColor(accentColor)
                navigationIcon = ContextCompat.getDrawable(this@StoreSearchActivity,
                    R.drawable.btn_back_material)?.apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        colorFilter = BlendModeColorFilter(accentColor, BlendMode.SRC_ATOP)
                    }
                    else {
                        setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP)
                    }
                }
                setOnClickListener { finish() }  // finish it and go back
                setSupportActionBar(searchToolbar)
            }
            // prepare search results
            var store: StoreData? = null
            loadJSONObjectFromAssets("storev2.json")?.let { json -> // assets
                store = StoreData.parse(json)
            }
            loadJSONObject("storev2.json")?.let { json -> // local storage
                store = StoreData.parse(json)
            }
            store?.let { store ->
                Handler(Looper.getMainLooper()).post {
                    displayResults(store.search(keyword.lowercase()))
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun displayResults(books: Array<BookData>) = runOnUiThread {
        searchListView.adapter = object: ArrayAdapter<String>(this, R.layout.view_book_search_item) {
            override fun getCount() = books.size
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val book = books[position]
                val view: View
                val holder: ViewHolder
                if (convertView == null) {
                    view = layoutInflater.inflate(R.layout.view_book_search_item, parent, false)
                    holder = ViewHolder()
                    holder.thumbnail = view.findViewById(R.id.bookThumbnailImg)
                    holder.sectionView = view.findViewById(R.id.bookSectionTitleTxt)
                    holder.chapterView = view.findViewById(R.id.bookChapterTitleTxt)
                    holder.contentView = view.findViewById(R.id.bookContentTxt)
                    holder.layoutReadHeard = view.findViewById(R.id.layoutBookBtns)
                    view.tag = holder
                }
                else {
                    view = convertView
                    holder = view.tag as ViewHolder
                }
                holder.layoutReadHeard.visibility = View.GONE
                holder.contentView.maxLines = 4
                // set content and texts
                showImage("${AC.ContentURL}/books/store/tamil/${book.id}.png", holder.thumbnail)
                holder.sectionView.text = book.title
                holder.chapterView.text = book.author
                holder.contentView.text = book.desc
                return view
            }
        }
        // handle the click event
        searchListView.setOnItemClickListener { _, _, position, _ ->
            val book = books[position]
            if (app.getShelfBookIds().contains(book.id)) {
                startActivity(Intent(this@StoreSearchActivity,
                    BookReaderActivity::class.java).apply {
                    putExtra(IntentBookId, book.id)
                    putExtra("published", book.published)
                    // pass the last opened page detail
                    app.getLastOpenedDetail(book.id)?.let { bookmark ->
                        putExtra(IntentSectionId, bookmark.sectionId)
                        putExtra(IntentChapterId, bookmark.chapterId)
                        putExtra(IntentPageId, bookmark.pageId)
                    }
                })
            }
            else {
                startActivity(Intent(this@StoreSearchActivity,
                    BookDetailActivity::class.java).apply {
                    putExtra("book", book)
                })
            }
        }
        if (books.isNotEmpty()) {
            searchEmptyTxt.visibility = View.GONE
        }
    }

    private class ViewHolder {
        lateinit var thumbnail: ImageView
        lateinit var sectionView: TextView
        lateinit var chapterView: TextView
        lateinit var contentView: TextView
        lateinit var layoutReadHeard: ConstraintLayout
    }

}

