package com.whiture.apps.tamil.thousand.nights.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.whiture.apps.tamil.thousand.nights.AC
import com.whiture.apps.tamil.thousand.nights.R
import com.whiture.apps.tamil.thousand.nights.liteClickAnimation
import com.whiture.apps.tamil.thousand.nights.models.AppData
import com.whiture.apps.tamil.thousand.nights.showImage
import kotlinx.android.synthetic.main.fragment_apps_list.*

class AppListFragment: Fragment() {

    private lateinit var apps: Array<AppData>
    private var clicked: ((AppData) -> Unit)? = null

    companion object {
        fun newInstance(apps: Array<AppData>, clicked: (AppData) -> Unit) =
            AppListFragment().apply {
                this.clicked = clicked
                this.apps = apps
            }
    }

    override fun onStart() {
        super.onStart()
        appsListRec.adapter = object: RecyclerView.Adapter<ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                return ViewHolder(LayoutInflater.from(context).inflate(
                    R.layout.view_apps_list, parent, false))
            }

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                holder.setBook(apps[position])
            }

            override fun getItemCount(): Int = apps.size
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_apps_list, container, false)
    }

    inner class ViewHolder(var rootView: View): RecyclerView.ViewHolder(rootView) {
        var thumbnail: ImageView = rootView.findViewById(R.id.promoAppIconImg)
        var appTitle: TextView = rootView.findViewById(R.id.promoAppTitle)
        var appDesc: TextView = rootView.findViewById(R.id.promoAppDesc)
        var rating: ImageView = rootView.findViewById(R.id.promoAppRating)

        fun setBook(data: AppData) {
            activity?.let { activity ->
                rootView.liteClickAnimation(activity) { clicked?.invoke(data) }
            }
            activity?.showImage("${AC.ContentURL}/promo/${data.img}", thumbnail)
            appTitle.text = data.title
            appDesc.text = data.desc
            rating.setImageResource(when (data.rating) {
                10 -> R.drawable.rating_ten
                9 -> R.drawable.rating_nine
                else -> R.drawable.rating_eight
            })
        }
    }

}



