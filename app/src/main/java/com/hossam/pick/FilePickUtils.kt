package com.hossam.pick

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import java.util.*


/**
 * Created by Hossam Elsharkawy
0201099197556
on 8/27/2018.  time :15:40


private fun checkActivityResult(requestCode: Int, data: Intent) {
// Log.d("data", data.toString() + "")
if (requestCode == FILE_REQ_CODE) {
progressUi.visible()
progressUi.progress = 0

data.data?.toImageFile({ progress ->
progressUi.progress = progress
})
{ file ->
progressUi.progress = 0
progressUi.gone()
img_image.load(file)
onFiledPicked(file)

//  updateFileField(file)
Log.d("pickFile", "finalFile size : " + file.length() / 1204 + " k ")
}
}
}

usage :
1 -
uri.toFile {progress ->
Log.d("pickFile", "progress $progress ")
}

2-
or for small file  :
val  file =  uri.toFile()

3-
or for large file  :
uri.toFile({ progress ->
Log.d("pickFile", "progress $progress ")

}, { file ->
Log.d("pickFile", "finalFile size : " + file.length() / 1204 + " k - time:  " +
(System.currentTimeMillis() - s))
})


 */

fun AppCompatActivity.actionOpenDocument(action: (File) -> Unit) =
    registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        lifecycleScope.launch { action.invoke(uri.toFile(this@actionOpenDocument)) }
    }


fun AppCompatActivity.actionTakePicture(
    onComplete: (Boolean) -> Unit,
    //onComplete: (Uri, File) -> Unit,
   // onProgress: ((Int) -> Unit)? = null
) =
    registerForActivityResult(ActivityResultContracts.TakePicture()) { isSaved ->
        onComplete.invoke(isSaved)
    }


class MyTakePicture : ActivityResultContract<Uri, Boolean>() {
    @CallSuper
    override fun createIntent(context: Context, input: Uri): Intent {
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, input)
    }

    override fun getSynchronousResult(
        context: Context,
        input: Uri
    ): SynchronousResult<Boolean>? {
        return null
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }
}




fun AppCompatActivity.actionOpenDocumentUri(action: (Uri) -> Unit) =
    registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        lifecycleScope.launch { action.invoke(uri) }
    }


var READ_MODE = "r"
internal var FOLDER_SEPARATOR = "/"
var APPLICATION_PDF = "application/pdf"
var PDF_EXTENSION = "pdf"


fun Uri.toFile(context: Context, progress: ((Int) -> Unit)? = null) =
    FilePickUtils.fileFromUri(context, this, progress)

fun Uri.toImageFile(context: Context, progress: ((Int) -> Unit)? = null) =
    FilePickUtils.imageFileFromUri(context, this, progress)

suspend fun Uri.toFile(context: Context, onProgress: ((Int) -> Unit), onFile: (File) -> Unit) {
    withContext(Dispatchers.IO) {
        onFile.invoke(FilePickUtils.fileFromUri(context, this@toFile, onProgress))
    }
}

object FilePickUtils {

    private fun compressImage(imageFile: File) {

        val options = BitmapFactory.Options()
        //   options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
//2249
        var bitmap = BitmapFactory.decodeStream(imageFile.inputStream(), null, options)
        bitmap!!
        // 960

        val maxSize = 1280
        val outWidth: Int
        val outHeight: Int
        val inWidth = bitmap.width
        val inHeight = bitmap.height
        if (inWidth > inHeight) {
            outWidth = maxSize
            outHeight = inHeight * maxSize / inWidth
        } else {
            outHeight = maxSize
            outWidth = inWidth * maxSize / inHeight
        }

        bitmap = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, false)
        // val bitmap = BitmapFactory.decodeStream(imageFile.inputStream())


        //   Log.d("compressImage", "start compressed : " + imageFile.length() / 1024)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 20, imageFile.outputStream())
        // Log.d("compressImage", "end compressed : " + imageFile.length() / 1024)

    }

    /*****************************************/
    private fun fastChannelCopyWithProgress(
        src: ReadableByteChannel,
        dest: WritableByteChannel,
        length: Long,
        progressFun: (Int) -> Unit?
    ) {
        val bufferSize = 100 * 1024
        val buffer = ByteBuffer.allocateDirect(bufferSize)
        var progress = 0.0
        while (src.read(buffer) != -1) {
            buffer.flip()
            dest.write(buffer)
            progress += bufferSize

            buffer.compact()
            //      var r = length - progress

            val result = ((progress / length) * 100).toInt()
            progressFun.invoke(result)


            // Log.d("FilePickUtils", " !-1: ${result} ")
        }
        buffer.flip()
        while (buffer.hasRemaining()) {
            dest.write(buffer)
        }
    }

    private fun fastChannelCopy(
        src: ReadableByteChannel,
        dest: WritableByteChannel
    ) {
        val buffer = ByteBuffer.allocateDirect(100 * 1024)

        while (src.read(buffer) != -1) {
            buffer.flip()
            dest.write(buffer)
            buffer.compact()
        }
        buffer.flip()
        while (buffer.hasRemaining()) {
            dest.write(buffer)
        }
    }

    /******************************************/
    /*private fun getMimeType(fileName: String): String {
        return URLConnection.guessContentTypeFromName(fileName)
    }*/

    private fun createFile(path: String): Boolean {
        return if (!checkExistence(path)) {
            val temp = File(path)
            temp.createNewFile()
        } else
            false

    }


    private fun checkExistence(path: String) = File(path).exists()

    /*****************************************************************/

    fun fileFromUri(context: Context, data: Uri, progress: ((Int) -> Unit)?): File {
        val file = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, data)!!

        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(
            data,
            READ_MODE
        )!!

        parcelFileDescriptor.statSize
        val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
        val filePath = (context.getExternalFilesDir(null)?.absolutePath
                + FOLDER_SEPARATOR
                + file.name)

        /* if ( file.type == APPLICATION_PDF) {
             filePath += ".$PDF_EXTENSION"

         }*/

        if (!createFile(filePath)) {
            return File(filePath)
        }

        val from = Channels.newChannel(inputStream)
        val to = Channels.newChannel(FileOutputStream(filePath))

        if (progress != null) {
            fastChannelCopyWithProgress(
                from,
                to,
                file.length(),
                progress
            )
        } else {
            fastChannelCopy(from, to)
        }

        from.close()
        to.close()

        return File(filePath)
    }

    fun imageFileFromUri(context: Context, data: Uri, progress: ((Int) -> Unit)?): File {

        val originalFile = fileFromUri(context, data, progress)


        var newFile = File(
            context.getExternalFilesDir(null),
            "temp_" + SystemClock.currentThreadTimeMillis() + "_" + originalFile.name
        )

        //newFile.createNewFile()
        //newFile.deleteOnExit()
        if (!newFile.exists()) {


            newFile = originalFile.copyTo(newFile, overwrite = false)

            //  newFile.createNewFile()

            Log.d("pickFile", "originalFile  : ${originalFile.path} ")
            Log.d("pickFile", "newFile  : ${newFile.path} ")


            //  val lengthInKb = originalFile.length() / (1024 * 1024) //in m
            //    Log.d("pickFile", "lengthInKb  : $lengthInKb k ")

//, 1280, 720, 50
            Log.d("pickFile", "newFile size : " + newFile.length() / 1204 + " k ")
            compressImage(newFile)
            Log.d("pickFile", " compressed size : " + newFile.length() / 1204 + " k ")

        }
        return newFile
    }


}
class ImageFileFilter : FileFilter {
    private val okFileExtensions = arrayOf(
        "jpg",
        "png",
        "gif",
        "jpeg"
    )

    override fun accept(file: File): Boolean {
        for (extension in okFileExtensions) {
            if (file.name.lowercase(Locale.ENGLISH).endsWith(extension)) {
                return true
            }
        }
        return false
    }
}



