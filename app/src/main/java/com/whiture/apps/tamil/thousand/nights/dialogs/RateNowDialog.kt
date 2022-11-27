package com.whiture.apps.tamil.thousand.nights.dialogs

import android.app.Activity
import com.whiture.apps.tamil.thousand.nights.R
import kotlinx.android.synthetic.main.dialog_rate_now.*

class RateNowDialog(mContext: Activity) :AppDialog(mContext, R.layout.dialog_rate_now) {

    fun setDialog(rateNowHandler: ()->Unit, rateLaterHandler: ()->Unit, notRatingHandler: ()->Unit) {
        rateNowDialogBtn.setOnClickListener {
            dismiss()
            rateNowHandler()
        }
        rateLaterDialogBtn.setOnClickListener {
            dismiss()
            rateLaterHandler()
        }
        notRatingDialogBtn.setOnClickListener {
            dismiss()
            notRatingHandler()
        }
    }

}