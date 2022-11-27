package com.whiture.apps.tamil.thousand.nights.dialogs

import android.app.Activity
import android.view.View
import android.widget.AdapterView
import android.widget.SeekBar
import com.whiture.apps.tamil.thousand.nights.R
import kotlinx.android.synthetic.main.dialog_read_settings.*

class ReadSettingsDialog(mContext: Activity) :AppDialog(mContext, R.layout.dialog_read_settings) {

    fun setDialog(fontSizeIndex: Int, fontFaceIndex: Int, backgroundIndex: Int) {
        setCancelable(true)
        closeDialogBtn.setOnClickListener { dismiss() }
        spinnerFontSize.setSelection(fontSizeIndex)
        spinnerFontFace.setSelection(fontFaceIndex)
        spinnerMode.setSelection(backgroundIndex)
    }

    fun setHandlers(fontSizeHandler: ((Int)->Unit), fontFaceHandler: ((Int)->Unit),
                    backgroundHandler: ((Int)->Unit)) {
        spinnerFontSize.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(view: AdapterView<*>?) { }
            override fun onItemSelected(aView: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                fontSizeHandler(pos)
            }
        }
        spinnerFontFace.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(view: AdapterView<*>?) { }
            override fun onItemSelected(aView: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                fontFaceHandler(pos)
            }
        }
        spinnerMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(view: AdapterView<*>?) { }
            override fun onItemSelected(aView: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                backgroundHandler(pos)
            }
        }
    }

    fun setBrightness(progress: Int, handler: ((Int)->Unit), permissionHandler: ()->Unit) {
        seekbarBrightness.progress = progress
        seekbarBrightness.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progresValue: Int, fromUser: Boolean) {
                if (fromUser) {
                    handler(progresValue)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // do nothing
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                permissionHandler()
            }
        })
    }

}

