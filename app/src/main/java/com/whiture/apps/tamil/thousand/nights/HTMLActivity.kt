package com.whiture.apps.tamil.thousand.nights

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import kotlinx.android.synthetic.main.activity_webview.*

class HTMLActivity: AppCompatActivity() {

    override fun onCreate (savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        // load Admob ad request
        adBannerHTML.loadAd(AdRequest.Builder().build())

        intent.getStringExtra("notification_url")?.let {
            webView.loadUrl(it)
            return
        }
        intent.getStringExtra("html_url")?.let { url ->
            webView.loadUrl("file:///android_asset/htm/$url.html")
            return
        }
        informUser("மன்னிக்கவும்", "தங்கள் தேடிய தகவல் எங்களிடம் தற்போது இல்லை",
            AppButton("OK") {
                runOnUiThread { finish() }
            })
    }

}

