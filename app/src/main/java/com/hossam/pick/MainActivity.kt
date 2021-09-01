package com.hossam.pick

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.button_pickFile).setOnClickListener {
            buttonPickFile()
        }

        findViewById<View>(R.id.button_takeImage).setOnClickListener {
            buttonTakeImage()
        }

        findViewById<View>(R.id.button_pickImage).setOnClickListener {
            buttonPickImage()
        }
    }

    private val actionOpenDocument = actionOpenDocument { file ->
        if (ImageFileFilter().accept(file)){
            findViewById<ImageView>(R.id.imageView).setImageURI(Uri.fromFile(file))
        }
        else {
            print(file.toString())
        }
    }

    var latestTmpUri: Uri? = null

    private val actionTakePicture = actionTakePicture { isSuccess ->

        if (isSuccess) {
            latestTmpUri?.let { uri ->

                val file = uri.toFile(this).length()

                val image = uri.toImageFile(this).length()


                print("file : $file , image: $image")
                findViewById<ImageView>(R.id.imageView).setImageURI(uri)
            }
        }
    }


    private fun buttonPickFile() {
        actionOpenDocument.launch(arrayOf("*/*"))
    }

    private fun buttonPickImage() {
        actionOpenDocument.launch(arrayOf("image/*"))
    }

    private fun buttonTakeImage() {
        getTmpFileUri().let { uri ->
            latestTmpUri = uri
            actionTakePicture.launch(uri)
        }
    }


    private fun print(txt: String) {
        findViewById<TextView>(R.id.txt).text = txt
    }

    private fun AppCompatActivity.getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }

        return FileProvider.getUriForFile(
            applicationContext,
            "${BuildConfig.APPLICATION_ID}.provider",
            tmpFile
        )
    }
}