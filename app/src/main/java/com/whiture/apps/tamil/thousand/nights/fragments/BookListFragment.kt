package com.whiture.apps.tamil.thousand.nights.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.whiture.apps.tamil.thousand.nights.AC
import com.whiture.apps.tamil.thousand.nights.R
import com.whiture.apps.tamil.thousand.nights.liteClickAnimation
import com.whiture.apps.tamil.thousand.nights.models.BookData
import com.whiture.apps.tamil.thousand.nights.showImage
import kotlinx.android.synthetic.main.fragment_books_list.*

class BookListFragment: Fragment() {

    private lateinit var books: Array<BookData>
    private var clicked: ((BookData, Boolean) -> Unit)? = null
    var isShelf: Boolean = false

    companion object {
        fun newInstance(books: Array<BookData>, isShelf: Boolean, clicked: (BookData, Boolean) -> Unit) =
            BookListFragment().apply {
                this.books = books
                this.isShelf = isShelf
                this.clicked = clicked
            }
    }

    fun setBooks(books: Array<BookData>) {
        this.books = books
        booksListRec.adapter?.notifyDataSetChanged()
    }

    override fun onStart() {
        super.onStart()
        booksListRec.adapter = object: RecyclerView.Adapter<ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                return ViewHolder(LayoutInflater.from(context).inflate(
                    R.layout.view_books_list, parent, false))
            }

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                holder.setBook(books[position])
            }

            override fun getItemCount(): Int = books.size
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_books_list, container, false)
    }

    inner class ViewHolder(private val rootView: View): RecyclerView.ViewHolder(rootView) {
        var thumbnail: ImageView = rootView.findViewById(R.id.imgBookThumbnail)
        var bookTitle: TextView = rootView.findViewById(R.id.txtBookTitle)
        var authorName: TextView = rootView.findViewById(R.id.txtAuthorName)
        var bookDesc: TextView = rootView.findViewById(R.id.txtBookDesc)
        var btnLayout: ConstraintLayout = rootView.findViewById(R.id.layoutBookReadHear)
        var readBtn: Button = rootView.findViewById(R.id.btnBookRead)
        var hearBtn: Button = rootView.findViewById(R.id.btnBookHear)

        fun setBook(data: BookData) {
            activity?.let { activity ->
                readBtn.liteClickAnimation(activity) { clicked?.invoke(data, true) }
                hearBtn.liteClickAnimation(activity) { clicked?.invoke(data, false) }
                rootView.liteClickAnimation(activity) { clicked?.invoke(data, true) }
            }
            activity?.showImage("${AC.ContentURL}/books/store/tamil/${data.id}.png", thumbnail)
            bookTitle.text = data.title
            authorName.text = data.author
            bookDesc.text = data.desc
            btnLayout.visibility = if (data.audio) View.VISIBLE else View.GONE
        }
    }

}

