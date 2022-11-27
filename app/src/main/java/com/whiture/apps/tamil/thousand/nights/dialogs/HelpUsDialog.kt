package com.whiture.apps.tamil.thousand.nights.dialogs

import android.app.Activity
import com.whiture.apps.tamil.thousand.nights.R
import kotlinx.android.synthetic.main.dialog_help_us.*

class HelpUsDialog(mContext: Activity) :AppDialog(mContext, R.layout.dialog_help_us) {

    fun setDialog(rateNowHandler: ()->Unit, mailNowHandler: ()->Unit, moreAppsHandler: ()->Unit) {
        rateNowDialogBtn.setOnClickListener {
            dismiss()
            rateNowHandler()
        }
        mailNowDialogBtn.setOnClickListener {
            dismiss()
            mailNowHandler()
        }
        otherAppsDialogBtn.setOnClickListener {
            dismiss()
            moreAppsHandler()
        }
    }

}

