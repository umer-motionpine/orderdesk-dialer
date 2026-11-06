package com.motionpine.orderdeskdialer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Order Desk Dialer
 * Opens the NEW LOOK Order Desk artifact in "phone mode" (#phone). The desk page watches
 * its own database; when the laptop taps "Phone se call karo" it calls AndroidDialer.dial(n)
 * through the JS bridge below and this app places the call.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        // The desk. Change this if the artifact link ever changes.
        const val DESK_URL = "https://claude.ai/artifact/C3AvaFqNEuUSBEKBNP5hD8#phone"
        const val REQ_CALL = 41
    }

    private lateinit var web: WebView
    private var pendingNumber: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        web = findViewById(R.id.web)
        val s = web.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.javaScriptCanOpenWindowsAutomatically = true
        s.setSupportMultipleWindows(false)
        s.mediaPlaybackRequiresUserGesture = false
        s.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        // Look like mobile Chrome so claude.ai sign-in behaves normally
        s.userAgentString = s.userAgentString.replace("; wv", "") + " OrderDeskDialer/1.0"
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)

        // JS bridge — available in every frame, including the artifact's sandboxed frame
        web.addJavascriptInterface(Bridge(), "AndroidDialer")

        web.webChromeClient = WebChromeClient()
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                if (url.scheme == "tel") { dial(url.schemeSpecificPart); return true }
                if (url.scheme == "https" && (url.host == "wa.me" || url.host?.endsWith("whatsapp.com") == true)) {
                    startActivity(Intent(Intent.ACTION_VIEW, url)); return true
                }
                return false
            }
            override fun onPageFinished(view: WebView, url: String) {
                findViewById<View>(R.id.loading).visibility = View.GONE
            }
        }

        if (savedInstanceState == null) web.loadUrl(DESK_URL) else web.restoreState(savedInstanceState)
        ensureCallPermission()
    }

    inner class Bridge {
        @JavascriptInterface
        fun dial(number: String) { runOnUiThread { dial(number) } }

        @JavascriptInterface
        fun ping(): String = "ok"
    }

    private fun dial(raw: String) {
        val n = raw.filter { it.isDigit() || it == '+' }
        if (n.isEmpty()) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            try {
                startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$n")))
                Toast.makeText(this, "Calling $n", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Fall back to opening the dialer with the number filled in
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$n")))
            }
        } else {
            pendingNumber = n
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), REQ_CALL)
        }
    }

    private fun ensureCallPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), REQ_CALL)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CALL) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (!granted) Toast.makeText(this, "Call permission ke bina auto-dial nahi hoga", Toast.LENGTH_LONG).show()
            pendingNumber?.let { if (granted) dial(it) else startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$it"))) }
            pendingNumber = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); web.saveState(outState) }
    override fun onBackPressed() { if (web.canGoBack()) web.goBack() else super.onBackPressed() }
}
