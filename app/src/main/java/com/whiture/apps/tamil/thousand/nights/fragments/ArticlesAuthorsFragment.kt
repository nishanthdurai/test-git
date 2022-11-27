package com.whiture.apps.tamil.thousand.nights.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.whiture.apps.tamil.thousand.nights.*
import com.whiture.apps.tamil.thousand.nights.models.ArticleAuthor
import kotlinx.android.synthetic.main.fragment_articles_authors.*

class ArticlesAuthorsFragment: Fragment() {
    private var clicked: ((ArticleAuthor)->Unit)? = null

    companion object {
        fun newInstance(clicked: (ArticleAuthor)->Unit)
        = ArticlesAuthorsFragment().apply { this.clicked = clicked }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? = inflater.inflate(
        R.layout.fragment_articles_authors, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let { activity ->
            activity.httpGetJSON("${AC.ServerURL}/articles/authors") {
                    success, code, data, _ ->
                runOnUiThread {
                    if (activity.isActive() && success && code == 200 && data != null) {
                        article_authors_progress.visibility = View.GONE
                        article_authors_progress_text.visibility = View.GONE
                        val authors = data.objectArray("authors").map {
                            ArticleAuthor.parse(it) }.toTypedArray()
                        fragment_articles_authors_list.adapter = object: RecyclerView.Adapter<ViewHolder>() {
                            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
                                LayoutInflater.from(context).inflate(R.layout.view_articles_authors, parent,
                                    false), authors)
                            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                                holder.setAuthor(authors[position])
                            }
                            override fun getItemCount() = authors.size
                        }
                    }
                    else {
                        article_authors_progress.visibility = View.GONE
                        article_authors_progress_text.text = "No internet, please try again.."
                    }
                }
            }
        }
    }

    private inner class ViewHolder(rootView: View, authors: Array<ArticleAuthor>)
        : RecyclerView.ViewHolder(rootView) {
        var authorImageView: ImageView = rootView.findViewById(R.id.view_article_view_author_img)
        var authorNameTxt: TextView = rootView.findViewById(R.id.view_article_view_author_name_txt)
        var authorDescTxt: TextView = rootView.findViewById(R.id.view_article_view_author_designation_txt)

        init {
            rootView.setOnClickListener { clicked?.let { it(authors[adapterPosition]) } }
        }

        fun setAuthor(author: ArticleAuthor) {
            activity?.showImage(author.profile, authorImageView)
            authorNameTxt.text = author.name
            authorDescTxt.text = author.title
        }
    }

}

