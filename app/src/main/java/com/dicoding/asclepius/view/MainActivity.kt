package com.dicoding.asclepius.view

import android.app.Activity
import android.content.Intent
import android.icu.text.NumberFormat
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.dicoding.asclepius.R
import com.dicoding.asclepius.database.HistoryData
import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper
import com.dicoding.asclepius.view.history.HistoryActivity
import com.dicoding.asclepius.view.history.HistoryViewModel
import com.dicoding.asclepius.view.result.ResultActivity
import com.yalantis.ucrop.UCrop
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var imageClassifierHelper: ImageClassifierHelper
    private lateinit var viewModel: HistoryViewModel
    private var currentImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[HistoryViewModel::class.java]

        binding.galleryButton.setOnClickListener { startGallery() }
        binding.analyzeButton.setOnClickListener {
            currentImageUri?.let {
                analyzeImage()
            } ?: run {
                showToast(getString(R.string.empty_image_warning))
            }
        }
    }

    private fun startGallery() {
        // TODO: Mendapatkan gambar dari Gallery.
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            uCropContract(uri)
        } else {
            Log.d("Photo Picker", "No media selected")
        }
    }

    private fun showImage() {
        // TODO: Menampilkan gambar sesuai Gallery yang dipilih. DONE
        currentImageUri?.let {
            Log.d("Image URI", "showImage: $it")
            binding.previewImageView.setImageURI(it)
        }
    }

    private fun uCropContract(sourceUri: Uri) {

        val baseFileName = "cropped_image"
        val fileExtension = ".jpg"

        var index = 0
        var destinationUri: Uri

        while (true) {
            val fileName = if (index == 0) {
                "$baseFileName$fileExtension"
            } else {
                "$baseFileName$index$fileExtension"
            }

            destinationUri = Uri.fromFile(File(cacheDir, fileName))

            if (!File(destinationUri.path!!).exists()) {
                break
            }

            index++
        }
        UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri: Uri? = UCrop.getOutput(data!!)
            // Process the cropped image URI here
            resultUri?.let {
                currentImageUri = it
                showImage()
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError: Throwable? = UCrop.getError(data!!)
            // Handle the crop error here
            showToast(cropError.toString())
        }
    }

    private fun analyzeImage() {
        // TODO: Menganalisa gambar yang berhasil ditampilkan.
        currentImageUri?.let {uri ->
            imageClassifierHelper = ImageClassifierHelper(
                context = this,
                classifierListener = object : ImageClassifierHelper.ClassifierListener {
                    override fun onError(error: String) {
                        showToast(error)
                    }

                    override fun onResults(results: List<Classifications>?) {
                        var classifyResult = ""
                        var classifyLabel = ""
                        results?.forEach { result ->
                            val sortedCategories = result.categories.sortedByDescending { it.score }

                            sortedCategories.forEach{
                                classifyLabel = it.label
                            }
                            classifyResult = sortedCategories.joinToString("\n") {
                                "${it.label} " + NumberFormat.getPercentInstance().format(it.score)
                                    .trim()
                            }
                        }
                        viewModel.saveHistory(
                            HistoryData(
                                result = classifyResult,
                                image = currentImageUri.toString(),
                                label = classifyLabel
                            )
                        )
                        Log.d("move to result", "masa g bisa")
                        moveToResult(classifyResult, classifyLabel)
                    }
                })
            imageClassifierHelper.classifyStaticImage(uri)
        } ?: showToast("Tidak ada gambar yang diinput")
    }

    private fun moveToResult(classifyResult: String, classifyLabel: String ) {
        val intent = Intent(this@MainActivity, ResultActivity::class.java)
        intent.putExtra(ResultActivity.EXTRA_IMAGE_URI, currentImageUri.toString())
        intent.putExtra(ResultActivity.EXTRA_RESULT, classifyResult)
        intent.putExtra(ResultActivity.EXTRA_LABEL, classifyLabel)
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate menu resource
        menuInflater.inflate(R.menu.menu_bar, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.history_btn -> {
                val intent = Intent(this, HistoryActivity::class.java)
                startActivity(intent)
                true
            }else -> super.onOptionsItemSelected(item)
        }
    }
}