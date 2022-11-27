package com.whiture.apps.tamil.thousand.nights.dialogs

import android.app.Activity
import com.whiture.apps.tamil.thousand.nights.R
import kotlinx.android.synthetic.main.dialog_share.*

class ShareDialog(mContext: Activity): AppDialog(mContext, R.layout.dialog_share) {

    fun setDialog(textShareHandler: ()->Unit, imageShareHandler: () -> Unit) {
        shareText.setOnClickListener {
            dismiss()
            textShareHandler()
        }
        imageShareText.setOnClickListener {
            dismiss()
            imageShareHandler()
        }
    }

}