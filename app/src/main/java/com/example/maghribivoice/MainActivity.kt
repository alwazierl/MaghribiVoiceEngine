package com.example.maghribivoice

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = TextView(this).apply {
            text = "Maghribi Voice Engine\n\nمحرك نطق مغربي تجريبي\n\nالصوت: Jamal Neural\nاللغات: العربية والدارجة المغربية\nيتطلب اتصالاً بالإنترنت وخادماً مضبوطاً."
            textSize = 18f
            setPadding(32, 48, 32, 32)
        }
        val settings = Button(this).apply {
            text = "إعدادات تحويل النص إلى كلام"
            setOnClickListener {
                startActivity(Intent("com.android.settings.TTS_SETTINGS"))
            }
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(title)
            addView(settings)
            setPadding(24, 24, 24, 24)
        }
        setContentView(layout)
    }
}
