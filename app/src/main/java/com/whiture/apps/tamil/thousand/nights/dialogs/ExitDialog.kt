package com.whiture.apps.tamil.thousand.nights.dialogs

import android.app.Activity
import com.whiture.apps.tamil.thousand.nights.R
import kotlinx.android.synthetic.main.dialog_exit.*

class ExitDialog(mContext: Activity): AppDialog(mContext, R.layout.dialog_exit) {

    fun setDialog(exitHandler: ()->Unit) {
        // handlers
        cancelDialogBtn.setOnClickListener { dismiss() }
        okDialogBtn.setOnClickListener {
            dismiss()
            exitHandler()
        }
    }

}
