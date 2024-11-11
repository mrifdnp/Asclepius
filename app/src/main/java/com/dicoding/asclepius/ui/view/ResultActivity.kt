package com.dicoding.asclepius.ui.view

import android.annotation.SuppressLint
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {

    private lateinit var resultBinding: ActivityResultBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        resultBinding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(resultBinding.root)

        // TODO: Menampilkan hasil gambar, prediksi, dan confidence score.

        val label = intent.getStringExtra("NAME") ?: "Unknown"
        val score = intent.getFloatExtra("SCORE", 0f)
        val inferenceTime = intent.getLongExtra("INFERENCE_TIME", 0)
        val imageUriString = intent.getStringExtra("IMAGE_URI")
        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            val imageView = findViewById<ImageView>(R.id.result_image)
            imageView.setImageURI(imageUri)
            resultAnalize(label, score, inferenceTime)
        }
    }



    @SuppressLint("DefaultLocale")
    private fun resultAnalize(label: String, score: Float, inferenceTime: Long) {
        val resultText = """
            Prediksi Menghasilkan $label
            Dengan Inference Time $inferenceTime ms
            Hasil Score ${String.format("%.2f%%", score * 100)}
        """.trimIndent()

        resultBinding.resultText.text = resultText
    }



}