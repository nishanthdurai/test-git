package com.whiture.apps.tamil.thousand.nights.dialogs

import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.whiture.apps.tamil.thousand.nights.R
import com.whiture.apps.tamil.thousand.nights.dialogs.AppDialog
import kotlinx.android.synthetic.main.dialog_common_list.*

class CommonListViewDialog(val mContext: Activity): AppDialog(mContext, R.layout.dialog_common_list) {
    fun setDialog(header: String, titles: Array<String>,
                  drawable: Drawable, listener: (Int) -> Unit) {
        dialog_common_list_title_txt.text = header
        dialog_common_list_title_txt.background = drawable
        dialog_common_list.adapter = object : BaseAdapter() {
            override fun getCount(): Int = titles.size
            override fun getItem(position: Int) = position
            override fun getItemId(position: Int) = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view: View
                val holder: ViewHolder
                if(convertView == null) {
                    view = layoutInflater.inflate(R.layout.view_common_list_view_dialog, null)
                    holder = ViewHolder()
                    holder.textView = view.findViewById(R.id.textName)
                    view.tag = holder
                }
                else {
                    view = convertView
                    holder = view.tag as ViewHolder
                }
                holder.textView.text = titles[position]
                return view
            }
        }

        dialog_common_list.setOnItemClickListener{ _, _, position, _ ->
            listener(position)
            dismiss()
        }
    }

    class ViewHolder {
        lateinit var textView: TextView
    }
}
