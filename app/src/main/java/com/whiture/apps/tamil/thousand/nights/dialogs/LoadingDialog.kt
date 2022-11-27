package com.whiture.apps.tamil.thousand.nights.dialogs

import android.app.Activity
import androidx.core.content.ContextCompat
import com.whiture.apps.tamil.thousand.nights.R
import kotlinx.android.synthetic.main.dialog_loading.*

class LoadingDialog(mContext: Activity): AppDialog(mContext, R.layout.dialog_loading) {

    fun setDialog(title: String, message: String) {
        val drawable = progressBarDialog.indeterminateDrawable.mutate()
        drawable.setColorFilter(ContextCompat.getColor(activity, R.color.colorPrimaryLite),
            android.graphics.PorterDuff.Mode.SRC_IN)
        progressBarDialog.progressDrawable = drawable
        titleDialogTxt.text = title
        descDialogTxt.text = message
    }

}

