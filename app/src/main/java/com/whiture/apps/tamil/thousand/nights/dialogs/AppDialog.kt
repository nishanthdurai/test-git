package com.whiture.apps.tamil.thousand.nights.dialogs

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button

open class AppDialog(protected val activity: Activity, private val resId: Int): Dialog(activity) {

    // override it to show the width and place it at the center
    override fun onWindowFocusChanged (hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Setting the width of the popup as 90% of screen
            window?.let {
                val dialogLayoutParams = WindowManager.LayoutParams()
                dialogLayoutParams.copyFrom(it.attributes)
                dialogLayoutParams.width = (context.resources.displayMetrics.widthPixels * 0.90).toInt()
                it.attributes = dialogLayoutParams
                it.setGravity(Gravity.CENTER)
            }
        }
    }

    // create the dialog with specific window style
    override fun onCreate (savedInstanceState: Bundle?) {
        window?.requestFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(resId)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    // method to prepare the button with title and button click handler method
    protected fun prepareButton(button: Button, title: String, handler: (()->Unit)? = null) {
        button.visibility = View.VISIBLE
        button.text = title
        button.setOnClickListener {
            this.dismiss()
            handler?.let { it() }
        }
    }

    override fun show() {
        if (!activity.isFinishing) { // for a safer call
            super.show()
        }
    }

}