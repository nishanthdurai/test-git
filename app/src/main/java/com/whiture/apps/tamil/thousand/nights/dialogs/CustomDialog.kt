package com.whiture.apps.tamil.thousand.nights.dialogs

import android.app.Activity
import android.view.View
import com.whiture.apps.tamil.thousand.nights.AppButton
import com.whiture.apps.tamil.thousand.nights.R
import kotlinx.android.synthetic.main.dialog_custom.*

class CustomDialog(mContext: Activity): AppDialog(mContext, R.layout.dialog_custom) {

    fun setDialog(title: String? = null, message: String? = null,
                  btn1: AppButton? = null, btn2: AppButton? = null, img: Int? = null) {
        title?.let { titleDialogTxt.text = it }
        message?.let { descDialogTxt.text = it }
        img?.let {
            imgDialog.visibility = View.VISIBLE
            imgDialog.setImageResource(img)
        }

        val totalButtons = (if (btn1 != null) 1 else 0) + (if (btn2 != null) 1 else 0)
        if (totalButtons == 1) {
            btn1?.let { prepareButton(maybeDialogBtn, it.first, it.second) }
            btn2?.let { prepareButton(maybeDialogBtn, it.first, it.second) }
        }
        else {
            btn1?.let { prepareButton(btn_dialog_ok, it.first, it.second) }
            btn2?.let { prepareButton(btn_dialog_cancel, it.first, it.second) }
        }
    }

}
