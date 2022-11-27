package com.whiture.apps.tamil.thousand.nights

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.whiture.apps.tamil.thousand.nights.models.MetaAlbum
import com.whiture.apps.tamil.thousand.nights.models.MetaBook
import kotlinx.android.synthetic.main.activity_audio_playlist.*

class AudioPlayListActivity: AppCompatActivity() {

    private lateinit var app: App

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_playlist)

        app = application as App // app reference

        val bookId = intent.getIntExtra(IntentBookId, AC.bookId)
        val index = intent.getIntExtra(IntentAlbumId, 0)
        val book = MetaBook.parse(if (bookId == AC.bookId) loadJSONObjectFromAssets("$bookId.json")
            else loadJSONObject("$bookId/$bookId.json"))
        val albums = book.getAlbums()
        // prepare the tool bar and set the back arrow color to accent color
        ContextCompat.getColor(this@AudioPlayListActivity, R.color.colorAccent).let { accentColor ->
            audioPlayListToolbar.apply {
                this.title = book.title
                setTitleTextColor(accentColor)
                navigationIcon = ContextCompat.getDrawable(this@AudioPlayListActivity,
                    R.drawable.btn_down_material)?.apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        colorFilter = BlendModeColorFilter(accentColor, BlendMode.SRC_ATOP)
                    }
                    else {
                        setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP)
                    }
                }
                setOnClickListener { finish() }
            }
        }

        // set adapter
        mpChaptersRcyView.adapter = object: RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val rootLayout: View
                return if (viewType == 0) {
                    rootLayout = layoutInflater.inflate(R.layout.view_mp_item_second_name,
                        parent, false)
                    TitleViewHolder(rootLayout)
                }
                else {
                    rootLayout = layoutInflater.inflate(R.layout.view_mp_item_chapter_name,
                        parent, false)
                    ChapterViewHolder(rootLayout)
                }
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val album = albums[position]
                if (album.isSection) {
                    (holder as TitleViewHolder).setData(album)
                }
                else {
                    var albumIndex = -1
                    for (a in albums) {
                        if (!a.isSection) {
                            albumIndex += 1
                            if (album == a) break
                        }
                    }
                    (holder as ChapterViewHolder).setData(album, albumIndex,
                        index == albumIndex)
                }
            }

            override fun getItemCount(): Int = albums.size

            // 0 - section, 1 - chapter
            override fun getItemViewType(position: Int) = if (albums[position].isSection) 0 else 1
        }
        mpChaptersRcyView.setHasFixedSize(true)
        mpChaptersRcyView.layoutManager = RecyclerViewCustomScroller(this)
        var i = -1
        var albumIndex = -1
        for (a in albums) {
            i += 1
            if (!a.isSection) {
                albumIndex += 1
                if (albumIndex == index) break
            }
        }
        mpChaptersRcyView.scrollToPosition(i - 1)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_from_top, R.anim.slide_in_top)
    }

    private fun playChapter(index: Int) {
        setResult(Activity.RESULT_OK, Intent().apply { putExtra(IntentAlbumId, index) })
        finish()
    }

    // This class handled smooth scrolling to given position in recycler view
    class RecyclerViewCustomScroller(context: Context?): LinearLayoutManager(context) {
        val millisecondsPerInch = 50.0f
        override fun smoothScrollToPosition(recyclerView: RecyclerView?, state: RecyclerView.State?,
                                            position: Int) {
            val smoothScroller = CenterSmoothScroller(recyclerView?.context)
            smoothScroller.targetPosition = position
            startSmoothScroll(smoothScroller)
        }

        inner class CenterSmoothScroller(context: Context?) : LinearSmoothScroller(context) {
            override fun calculateDtToFit(viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int,
                                          snapPreference: Int): Int {
                return (boxStart + (boxEnd - boxStart) / 4) - (viewStart + (viewEnd - viewStart) / 4)
            }

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
                return millisecondsPerInch / displayMetrics!!.densityDpi
            }
        }
    }

    inner class ChapterViewHolder(private val rootView: View): RecyclerView.ViewHolder(rootView) {
        private var chapterName: TextView = rootView.findViewById(R.id.chapterTitleTxt)
        private var duration: TextView = rootView.findViewById(R.id.chapterPlayDuration)
        private var playIcon: ImageView = rootView.findViewById(R.id.chapterPlayImg)

        fun setData(data: MetaAlbum, index: Int, isPlaying: Boolean) {
            chapterName.text = data.title
            duration.text = timeLabel(data.length)
            // if current playing index and rv position is same then show playing image
            playIcon.visibility = if (isPlaying) View.VISIBLE else View.INVISIBLE
            rootView.setOnClickListener {
                playChapter(index)
            }
        }
    }

    inner class TitleViewHolder(rootView: View): RecyclerView.ViewHolder(rootView) {
        private var sectionName: TextView = rootView.findViewById(R.id.sectionTitleTxt)
        fun setData(data: MetaAlbum) {
            sectionName.text = data.title
            sectionName.setTextColor(ContextCompat.getColor(this@AudioPlayListActivity,
                R.color.colorPrimaryLite))
        }
    }

}

