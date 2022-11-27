package com.whiture.apps.tamil.thousand.nights.dialogs

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.View
import com.whiture.apps.tamil.thousand.nights.R
import com.whiture.apps.tamil.thousand.nights.createBitmapFromView
import com.whiture.apps.tamil.thousand.nights.getImageUri
import kotlinx.android.synthetic.main.dialog_share_preview.*

class SharePreviewDialog(val mContext: Activity, private val bitmap: Bitmap): AppDialog(mContext,
    R.layout.dialog_share_preview) {
    fun setDialog(header: String) {
        share_header.text = header
        show()
        share_bitmap.setImageBitmap(bitmap)
        share_btn.setOnClickListener {
            Handler(Looper.getMainLooper()).postDelayed({
                share_btn.visibility = View.GONE
                val uri = getImageUri(mContext.createBitmapFromView(root_layout), mContext)
                uri?.let {
                    mContext.startActivity(Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                    })
                }
                dismiss()
            }, 1500)
        }
    }
}

