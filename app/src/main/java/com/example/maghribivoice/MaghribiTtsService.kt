package com.example.maghribivoice

import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

class MaghribiTtsService : TextToSpeechService() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(100, TimeUnit.SECONDS)
        .build()

    @Volatile private var stopped = false

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        if (lang.equals("ar", true) && (country.isNullOrBlank() || country.equals("MA", true))) {
            return TextToSpeech.LANG_COUNTRY_AVAILABLE
        }
        return TextToSpeech.LANG_NOT_SUPPORTED
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        return onIsLanguageAvailable(lang, country, variant)
    }

    override fun onGetLanguage(): Array<String> = arrayOf("ar", "MA", "")

    override fun onStop() {
        stopped = true
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        stopped = false
        val text = request.charSequenceText?.toString()?.trim().orEmpty()
        if (text.isEmpty()) {
            callback.error()
            return
        }

        try {
            val mode = if (text.any { it in '\u0600'..'\u06FF' }) "arabic_or_darija" else "arabic_or_darija"
            val payload = JSONObject()
                .put("text", text)
                .put("mode", mode)
                .put("locale", "ar-MA")
                .toString()

            val body = payload.toRequestBody("application/json; charset=utf-8".toMediaType())
            val httpRequest = Request.Builder()
                .url(BuildConfig.API_BASE_URL + "tts")
                .header("Authorization", "Bearer " + BuildConfig.API_TOKEN)
                .post(body)
                .build()

            client.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    callback.error()
                    return
                }
                val wav = response.body?.bytes() ?: run {
                    callback.error()
                    return
                }
                val pcm = extractPcmFromWav(wav)
                if (pcm.isEmpty() || stopped) {
                    callback.error()
                    return
                }

                callback.start(24000, android.media.AudioFormat.ENCODING_PCM_16BIT, 1)
                val chunkSize = 8192
                var offset = 0
                while (offset < pcm.size && !stopped) {
                    val end = minOf(offset + chunkSize, pcm.size)
                    val chunk = pcm.copyOfRange(offset, end)
                    val result = callback.audioAvailable(chunk, 0, chunk.size)
                    if (result != TextToSpeech.SUCCESS) break
                    offset = end
                }
                if (stopped) callback.error() else callback.done()
            }
        } catch (_: Exception) {
            callback.error()
        }
    }

    // Azure RIFF output is a WAV container. Android SynthesisCallback needs raw PCM.
    private fun extractPcmFromWav(wav: ByteArray): ByteArray {
        if (wav.size < 44) return ByteArray(0)
        val input = DataInputStream(ByteArrayInputStream(wav))
        val header = ByteArray(12)
        input.readFully(header)
        if (String(header, 0, 4) != "RIFF" || String(header, 8, 4) != "WAVE") return ByteArray(0)

        var sampleRate = 0
        var channels = 0
        var bits = 0
        while (input.available() >= 8) {
            val idBytes = ByteArray(4)
            input.readFully(idBytes)
            val id = String(idBytes)
            val size = Integer.reverseBytes(input.readInt())
            if (size < 0 || size > input.available()) return ByteArray(0)
            val data = ByteArray(size)
            input.readFully(data)
            if (id == "fmt " && size >= 16) {
                fun u16(i: Int) = (data[i].toInt() and 0xff) or ((data[i + 1].toInt() and 0xff) shl 8)
                fun u32(i: Int) = u16(i).toLong() or (u16(i + 2).toLong() shl 16)
                channels = u16(2)
                sampleRate = u32(4).toInt()
                bits = u16(14)
            } else if (id == "data") {
                if (sampleRate != 24000 || channels != 1 || bits != 16) return ByteArray(0)
                return data
            }
            if (size % 2 == 1 && input.available() > 0) input.readByte()
        }
        return ByteArray(0)
    }
}
