package com.dicoding.asclepius.ui.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper
import com.dicoding.asclepius.ui.viewmodel.MainViewModel
import com.yalantis.ucrop.UCrop
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.io.File

class MainActivity : AppCompatActivity(), ImageClassifierHelper.ClassifierListener  {
    private lateinit var imageHelper: ImageClassifierHelper
    private var originalImageUri: Uri? = null
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var mainBinding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        mainBinding.analyzeButton.setOnClickListener { analyzeImage() }
        mainBinding.galleryButton.setOnClickListener { startGallery() }



        imageHelper = ImageClassifierHelper(
            threshold = 0.5f,
            maxResults = 1,
            modelName = "cancer_classification.tflite",
            context = this,
            classifierListener = this
        )


        mainViewModel.currentImageUri.observe(this, Observer { uri ->
            uri?.let {

                showImage(uri)
            }
        })
    }


    private fun startGallery() {
        // TODO: Mendapatkan gambar dari Gallery.
        launcherGallery.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            mainViewModel.setCurrentImageUri(uri)

            imageCropLaunch(uri)
        } else {
            showToast(getString(R.string.media_not_found))
        }
    }


    private fun imageCropLaunch(uri: Uri) {
        originalImageUri = uri

        val destinationUri = Uri.fromFile(File(cacheDir, "croppedImage.jpg"))
        val options = UCrop.Options()
            .apply {
                setCompressionQuality(100)
            }

        UCrop.of(uri, destinationUri)
            .withAspectRatio(1f, 1f).withMaxResultSize(1080, 1080)
            .withOptions(options)
            .start(this)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == UCrop.REQUEST_CROP) {
            when (resultCode) {
                RESULT_OK -> {
                    val resultUri = UCrop.getOutput(data!!)
                    resultUri?.let {
                        mainViewModel.setCurrentImageUri(resultUri)
                        showImage(resultUri)
                    }
                }
                RESULT_CANCELED -> {

                    mainViewModel.setCurrentImageUri(originalImageUri)

                    showToast(getString(R.string.crop_canceled))
                    showImage(originalImageUri)
                }
                UCrop.RESULT_ERROR -> {

                    val cropError = UCrop.getError(data!!)
                    cropError?.printStackTrace()
                    showToast(getString(R.string.crop_failed))
                }
            }
        }
    }



    private fun showImage(uri: Uri?) {
        mainBinding.previewImageView.setImageURI(uri)
    }

    private fun showToast(message: String) {
        // TODO: Menampilkan Toast
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun analyzeImage() {
        val uri = mainViewModel.currentImageUri.value
        if (uri != null) {

            mainBinding.progressIndicator.visibility = View.VISIBLE
            imageHelper.classifyStaticImage(uri)
        } else {

            showToast(getString(R.string.media_not_found))
        }
    }



    private fun moveToResult(name: String, score: Float, inferenceTime: Long) {
        // TODO: Memindahkan ke ResultActivity.
        mainViewModel.currentImageUri.value?.let { imageUri ->

            val intent = Intent(this, ResultActivity::class.java)

            intent.putExtra("NAME", name)
            intent.putExtra("SCORE", score)
            intent.putExtra("INFERENCE_TIME", inferenceTime)
            intent.putExtra("IMAGE_URI", imageUri.toString())

            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.enter, R.anim.exit)
        }

    }


    override fun onImageError(error: String) {
        runOnUiThread {

            mainBinding.progressIndicator.visibility = View.GONE
            showToast(error)
        }
    }


    @SuppressLint("SuspiciousIndentation")
    override fun onImageResults(result: List<Classifications>?, inferenceTime: Long) {
        runOnUiThread {
            mainBinding.progressIndicator.visibility = View.GONE

            result?.let { classifications ->
                if (classifications.isNotEmpty() && classifications[0].categories.isNotEmpty()) {
                    val analize = classifications[0].categories[0]
                    moveToResult(analize.label, analize.score, inferenceTime)

                } else {
                    showToast(getString(R.string.klasifikasi_not_found))
                }
            } ?: showToast("Hasil tidak ditemukan")
        }
    }
}