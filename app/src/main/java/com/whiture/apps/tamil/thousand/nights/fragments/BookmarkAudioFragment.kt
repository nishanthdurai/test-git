package com.whiture.apps.tamil.thousand.nights.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.whiture.apps.tamil.thousand.nights.*
import com.whiture.apps.tamil.thousand.nights.models.Audiomark
import kotlinx.android.synthetic.main.fragment_bookmark.*

class BookmarkAudioFragment: Fragment() {
    private var audiomarks = mutableListOf<Audiomark>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_bookmark, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        audiomarks.addAll((activity?.application as BookApplication).getAudiomarks())
        bookmarksListView.adapter = object: RecyclerView.Adapter<ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
                LayoutInflater.from(context).inflate(R.layout.view_bookmark, parent, false))
            override fun onBindViewHolder(holder: ViewHolder, position: Int) { holder.setData(
                audiomarks[position]) }
            override fun getItemCount(): Int = audiomarks.size
        }
        bookmarksListView.layoutManager = LinearLayoutManager(activity)
        bookmarksListView.setHasFixedSize(true)
        refreshListView()
    }

    private fun askUser(position: Int) {
        val bookmark = audiomarks[position]
        activity?.askUser("Please Select", "What do you want to do?", AppButton("Listen") {
            startActivity(Intent(activity, AudioPlayerActivity::class.java).apply {
                bookmark.setValuesToIntent(this)
            })
        }, AppButton("Delete") {
            (activity?.application as BookApplication).removeAudiomark(audiomarks[position])
            audiomarks.removeAt(position)
            bookmarksListView.adapter?.notifyDataSetChanged()
            activity?.showMessage(title = "Success",
                message = "Your bookmark has been deleted successfully!..") { }
            refreshListView()
        })
    }

    private fun refreshListView() {
        if (audiomarks.isEmpty()) {
            bookmarksNATxt.visibility = View.VISIBLE
            bookmarksListView.visibility = View.GONE
        }
        else {
            bookmarksNATxt.visibility = View.GONE
            bookmarksListView.visibility = View.VISIBLE
        }
    }

    inner class ViewHolder(rootView: View): RecyclerView.ViewHolder(rootView) {

        private val thumbnail: ImageView = rootView.findViewById(R.id.bookmarkThumbnailImg)
        private val sectionTitle: TextView = rootView.findViewById(R.id.bookmarkSectionTitle)
        private val chapterTitle: TextView = rootView.findViewById(R.id.bookmarkChapterTitle)
        private val content: TextView = rootView.findViewById(R.id.bookmarkContent)

        init {
            rootView.setOnClickListener { askUser(adapterPosition) }
            rootView.findViewById<ImageView>(R.id.fragment_reader_audio_image).setImageResource(
                R.drawable.ic_audio_book)
            content.maxLines = 4
        }

        fun setData(data: Audiomark) {
            sectionTitle.text = data.sectionTitle
            chapterTitle.text = data.chapterTitle
            content.text = "${timeLabel(data.duration)} முதல் கேட்கவும்"
            activity?.showImage("${AC.ContentURL}/books/store/tamil/${data.bookId}.png", thumbnail)
        }
    }
}

