package com.whiture.apps.tamil.thousand.nights.dialogs

import android.app.Activity
import androidx.core.content.ContextCompat
import com.whiture.apps.tamil.thousand.nights.R
import kotlinx.android.synthetic.main.dialog_search.*

class SearchDialog(mContext: Activity) :AppDialog(mContext, R.layout.dialog_search) {

    fun setDialog(hint: String?, desc: String?, handler: (String)->Unit) {
        searchEditText.hint = hint ?: "அன்னை"
        descDialogTxt.text = desc ?: "தங்கள் தேடுதல் வார்த்தையை தமிழில் தரவும்."
        cancelDialogBtn.setOnClickListener {
            dismiss()
        }
        okDialogBtn.setOnClickListener {
            val searchText = searchEditText.text.toString()
            if (searchText.length < 4) {
                titleDialogTxt.text = context.resources.getString(R.string.search_not_good)
                descDialogTxt.text = context.resources.getString(R.string.enter_atleast_four)
                descDialogTxt.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
            }
            else {
                dismiss()
                handler(searchText)
            }
        }
    }

}