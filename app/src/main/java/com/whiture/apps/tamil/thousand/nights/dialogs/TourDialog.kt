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
import androidx.core.content.ContextCompat
import com.whiture.apps.tamil.thousand.nights.R
import com.whiture.apps.tamil.thousand.nights.buttonPress
import com.whiture.apps.tamil.thousand.nights.clickAnimation
import kotlinx.android.synthetic.main.dialog_tour.*

class TourDialog(private val activity: Activity): Dialog(activity) {

    override fun onCreate(savedInstanceState: Bundle?) {
        window?.requestFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_tour)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.let {
            val dialogLayoutParams = WindowManager.LayoutParams()
            dialogLayoutParams.copyFrom(it.attributes)
            dialogLayoutParams.width = context.resources.displayMetrics.widthPixels
            dialogLayoutParams.height = context.resources.displayMetrics.heightPixels
            it.attributes = dialogLayoutParams
            it.setGravity(Gravity.CENTER)
        }
        setCancelable(false)
    }

    override fun show() {
        if (!activity.isFinishing) { // for a safer call
            super.show()
        }
    }

    // call this after show method, this will prepare for button clicks
    fun setDialog(dismissHandler: ()->Unit) {
        closeBtn.clickAnimation(context) {
            dismiss()
            dismissHandler()
        }
        var pageIndex = 0
        paginationBtn.buttonPress(context) {
            pageIndex++
            if (pageIndex == 8) {
                dismiss()
                dismissHandler()
                return@buttonPress
            }
            paginationTextView.text = "${pageIndex + 1} of 8"
            when (pageIndex) {
                1 -> {
                    hintImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.hand_left))
                    hintDescTextView.text = context.resources.getString(R.string.tour_swipe_to_next_page)
                }
                2 -> {
                    hintImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.hand_right))
                    hintDescTextView.text = context.resources.getString(R.string.tour_swipe_to_previous_page)
                }
                3 -> {
                    hintImage.visibility = View.INVISIBLE
                    hintMenuNavDrawer.visibility = View.VISIBLE
                    hintDescTextView.text = context.resources.getString(R.string.tour_menu_click_message)
                }
                4 -> {
                    hintMenuNavDrawer.visibility = View.INVISIBLE
                    hintMenuSearch.visibility = View.VISIBLE
                    hintDescTextView.text = context.resources.getString(R.string.tour_search_book_message)
                }
                5 -> {
                    hintMenuSearch.visibility = View.INVISIBLE
                    hintMenuBookmark.visibility = View.VISIBLE
                    hintDescTextView.text = context.resources.getString(R.string.tour_bookmark_message)
                }
                6 -> {
                    hintMenuBookmark.visibility = View.INVISIBLE
                    hintMenuRotate.visibility = View.VISIBLE
                    hintDescTextView.text = context.resources.getString(R.string.tour_change_orientation_message)
                }
                7 -> {
                    hintMenuRotate.visibility = View.INVISIBLE
                    hintMenuTools.visibility = View.VISIBLE
                    hintDescTextView.text = context.resources.getString(R.string.tour_change_color_message)
                    paginationBtn.text = "FINISH"
                }
            }
        }
    }

    // override it to show the width and place it at the center
    override fun onWindowFocusChanged (hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Setting the width of the popup as 90% of screen
            window?.let {
                val dialogLayoutParams = WindowManager.LayoutParams()
                dialogLayoutParams.copyFrom(it.attributes)
                dialogLayoutParams.width = context.resources.displayMetrics.widthPixels
                dialogLayoutParams.height = context.resources.displayMetrics.heightPixels
                it.attributes = dialogLayoutParams
                it.setGravity(Gravity.CENTER)
            }
        }
    }

}

