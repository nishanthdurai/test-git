package com.whiture.apps.tamil.thousand.nights

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AudioBroadcastReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let { context ->
            context.startService(Intent(context, AudioService::class.java).apply {
                putExtra(AudioService.actionKey, intent?.action)
            })
        }
    }

}

