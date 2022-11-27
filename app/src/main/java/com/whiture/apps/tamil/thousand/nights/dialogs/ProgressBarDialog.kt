package com.whiture.apps.tamil.thousand.nights.dialogs

import android.app.Activity
import androidx.core.content.ContextCompat
import com.whiture.apps.tamil.thousand.nights.R
import kotlinx.android.synthetic.main.dialog_progress_bar.*

class ProgressBarDialog (mContext: Activity): AppDialog(mContext, R.layout.dialog_progress_bar) {

    fun setDialog(title: String, message: String) {
        setMessage(title, message)
        setCancelable(false)
        progressBarDialog.progressDrawable.setColorFilter(
            ContextCompat.getColor(activity, R.color.colorPrimaryLite),
            android.graphics.PorterDuff.Mode.SRC_IN)
        setProgress(0) // start from 0%
    }

    fun setMessage(title: String, message: String) {
        titleDialogTxt.text = title
        descDialogTxt.text = message
    }

    fun setProgress(value: Int) {
        progressBarDialog.progress = value
        percTextView.text = "$value%"
        countTextView.text = "$value/100"
    }

}

