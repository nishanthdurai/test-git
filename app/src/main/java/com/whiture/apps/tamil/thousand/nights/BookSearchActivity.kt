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
import kotlinx.android.synthetic.main.activity_book_search.*
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

class BookSearchActivity: AppCompatActivity() {

    private lateinit var app: App

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_search)

        // app preparation
        app = application as App
        // turns off night mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // load Admob ad request
        adBannerBookSearch.loadAd(AdRequest.Builder().build())

        intent.getStringExtra("keyword")?.let { keyword ->
            val bookId = intent.getIntExtra(IntentBookId, -1)
            // prepare toolbar
            val accentColor = ContextCompat.getColor(this, R.color.colorAccent)
            searchBookToolbar.apply {
                title = "Results for '$keyword'"
                setTitleTextColor(accentColor)
                navigationIcon = ContextCompat.getDrawable(this@BookSearchActivity,
                    R.drawable.btn_back_material)?.apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        colorFilter = BlendModeColorFilter(accentColor, BlendMode.SRC_ATOP)
                    }
                    else {
                        setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP)
                    }
                }
                setSupportActionBar(this)
            }
            searchBookList.emptyView = searchBookTxt
            if (bookId > 0) search(keyword, bookId)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // method to search for the given keyword in the given book
    private fun search(keyword: String, bookId: Int) {
        Handler(Looper.getMainLooper()).post {
            var bookMeta: JSONObject? = null
            if (bookId == AC.bookId) { // load from the assets directory
                loadJSONObjectFromAssets("$bookId.json")?.let { bookMeta = it }
            }
            else { // load from the local directory
                loadJSONObject("$bookId/$bookId.json")?.let { bookMeta = it }
            }
            bookMeta?.let { meta ->
                val results = mutableListOf<BookSearchData>()
                meta.objectArray("sections").forEachIndexed { id, section ->
                    var searchResultedForAChapter = false // to pick one line for each chapter
                    var chapterId = 1
                    if (bookId == AC.bookId) {
                        assets.open("section${id + 1}.bml")
                    }
                    else {
                        FileInputStream(File("${filesDir.absolutePath}/$bookId/section${id + 1}.bml"))
                    }.bufferedReader().forEachLine { line ->
                        if (line.contains("~~chapter~~")) {
                            chapterId = line.split("~~")[2].toInt()
                            searchResultedForAChapter = false
                        }
                        else {
                            if (!searchResultedForAChapter) {
                                if (!line.startsWith("~~") && line.isNotEmpty()
                                    && line.lowercase().contains(keyword!!)) {
                                    searchResultedForAChapter = true
                                    results.add(BookSearchData(section.getString("title"),
                                        section.stringArray("chapters")[chapterId-1], line.trim(), id + 1,
                                        chapterId))
                                }
                            }
                        }
                    }
                }
                // check the result for ads and result text
                if (results.isEmpty()) {
                    searchBookTxt.text = "No results found!.."
                }
                // set the adapter
                searchBookList.adapter = object: ArrayAdapter<String>(this, R.layout.view_book_search_item) {
                    override fun getCount() = results.size
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val search = results[position]
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
                        showImage("${AC.ContentURL}/books/store/tamil/${bookId}.png", holder.thumbnail)
                        holder.sectionView.text = search.sectionTitle
                        holder.chapterView.text = search.chapterTitle
                        holder.contentView.text = search.lineContent
                        return view
                    }
                }
                // handle the click event
                searchBookList.setOnItemClickListener { _, _, position, _ ->
                    startActivity(Intent(this@BookSearchActivity,
                        BookReaderActivity::class.java).apply {
                        putExtra(IntentBookId, bookId)
                        putExtra(IntentSectionId, results[position].sectionIndex)
                        putExtra(IntentChapterId, results[position].chapterIndex)
                        putExtra(IntentPageId, 0)
                    })
                }
            }
        }
    }

    private data class BookSearchData(val sectionTitle: String, val chapterTitle: String,
                              val lineContent: String, val sectionIndex: Int, val chapterIndex: Int)

    private class ViewHolder {
        lateinit var thumbnail: ImageView
        lateinit var sectionView: TextView
        lateinit var chapterView: TextView
        lateinit var contentView: TextView
        lateinit var layoutReadHeard: ConstraintLayout
    }

}

