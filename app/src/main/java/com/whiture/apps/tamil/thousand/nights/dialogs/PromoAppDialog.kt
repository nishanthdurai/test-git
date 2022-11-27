package com.whiture.apps.tamil.thousand.nights.dialogs

import android.app.Activity
import android.text.method.ScrollingMovementMethod
import com.whiture.apps.tamil.thousand.nights.R
import com.whiture.apps.tamil.thousand.nights.showImage
import kotlinx.android.synthetic.main.dialog_promo_app.*

class PromoAppDialog(val mContext: Activity, private val appTitle: String, private val appDescription: String,
                     private val appImgUrl: String): AppDialog(mContext, R.layout.dialog_promo_app) {
        fun setDialog(okBtnHandler: () -> Unit) {
            dialog_promo_app_title.text = appTitle
            txtPromoAppDesc.text = appDescription
            txtPromoAppDesc.movementMethod = ScrollingMovementMethod()
            mContext.showImage(appImgUrl, imgPromoApp)
            btnPromoInstall.setOnClickListener {
                okBtnHandler()
                dismiss()
            }
            btnDialogCancel.setOnClickListener { dismiss() }
        }
}

