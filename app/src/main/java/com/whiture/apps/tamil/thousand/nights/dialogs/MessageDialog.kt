package com.whiture.apps.tamil.thousand.nights.dialogs

import android.app.Activity
import com.whiture.apps.tamil.thousand.nights.R
import kotlinx.android.synthetic.main.dialog_message.*

class MessageDialog(mContext: Activity) : AppDialog(mContext, R.layout.dialog_message) {

    fun setDialog(title: String, message: String, handler: ()->Unit) {
        titleDialogTxt.text = title
        descDialogTxt.text = message
        okDialogBtn.setOnClickListener {
            dismiss()
            handler()
        }
    }

}