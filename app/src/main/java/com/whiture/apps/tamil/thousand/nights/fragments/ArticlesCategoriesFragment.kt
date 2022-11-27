package com.whiture.apps.tamil.thousand.nights.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.whiture.apps.tamil.thousand.nights.*
import com.whiture.apps.tamil.thousand.nights.models.ArticleCategory
import kotlinx.android.synthetic.main.fragment_articles_categories_title.*

class ArticlesCategoriesFragment: Fragment() {
    private lateinit var categories: Array<ArticleCategory>
    private var clicked: ((ArticleCategory)->Unit)? = null

    companion object {
        fun newInstance(clicked: (ArticleCategory)->Unit) =
            ArticlesCategoriesFragment().apply { this.clicked = clicked }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? = inflater.inflate(
        R.layout.fragment_articles_categories_title, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let { activity ->
            activity.httpGetJSON("${AC.ServerURL}/articles/categories") {
                    isSuccess, responseCode, data, _ ->
                runOnUiThread {
                    if (activity.isActive() && isSuccess && responseCode == 200 && data != null) {
                        categories = data.objectArray("categories").map {
                            ArticleCategory.parse(it) }.toTypedArray()
                        article_categories_progress.visibility = View.GONE
                        article_categories_progress_text.visibility = View.GONE
                        fragment_articles_categories_list.adapter = object: RecyclerView.Adapter<ViewHolder>() {
                            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
                                LayoutInflater.from(context).inflate(R.layout.view_article_category, parent,
                                    false))
                            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                                holder.setCategory(categories[position])
                            }
                            override fun getItemCount() = categories.size
                        }
                    }
                    else {
                        article_categories_progress.visibility = View.GONE
                        article_categories_progress_text.text = "No internet, please try again.."
                    }
                }
            }
        }
    }

    private inner class ViewHolder(rootView: View): RecyclerView.ViewHolder(rootView) {
        var imageView: ImageView = rootView.findViewById(R.id.view_article_category_img)
        init { rootView.setOnClickListener { clicked?.let { it(categories[adapterPosition]) } } }
        fun setCategory(category: ArticleCategory) { activity?.showImage(category.banner, imageView) }
    }

}

