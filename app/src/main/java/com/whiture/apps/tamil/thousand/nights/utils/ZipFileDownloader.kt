package com.whiture.apps.tamil.thousand.nights.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Utility class for downloading zip files, extracting them on a given path
 */
class ZipFileDownloader (private val context: Context,
                         private val zipFileName: String,
                         private val targetDir: String,
                         private val lengthOfFile: Int,
                         private val listener: ZipFileDownloaderListener,
                         private val isExternalDir: Boolean) {

    fun execute (urlString: String) {
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            var count: Int
            var status = "FAILURE"
            var inputStream: InputStream? = null
            var fileOutputStream: FileOutputStream? = null
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()
                val lengths = connection.headerFields["content-Length"]
                val fileLength = if (lengths != null && lengths.size > 0) lengths[0].toInt() else lengthOfFile
                val inPerc = 100.0 / fileLength
                inputStream = BufferedInputStream(url.openStream())
                fileOutputStream = context.openFileOutput(zipFileName, Context.MODE_PRIVATE)
                val data = ByteArray(524288) // 256 KB
                var total: Long = 0
                while (inputStream.read(data).also { count = it } != -1) {
                    total += count.toLong()
                    //pass the progress value
                    Handler(Looper.getMainLooper()).post {
                        listener.zipFileDownloading((total * inPerc).toInt())
                    }
                    fileOutputStream.write(data, 0, count)
                }
                fileOutputStream.flush()
                status = "SUCCESS"
                Log.d("WHILOGS", status)
            }
            catch (e: Exception) {
                listener.zipFileFailed()
                Log.e("WHILOGS", "Zip File Download Failure", e)
            }
            finally {
                try {
                    fileOutputStream!!.close()
                    inputStream!!.close()
                }
                catch (e: Exception) { // nothing done here
                }
            }
            //extracting zip file
            handler.post {
                if(status == "SUCCESS") {
                    if (status == "SUCCESS") {
                        extractZipFile()
                        listener.zipFileDownloadCompleted()
                    }
                    else {
                        listener.zipFileFailed()
                        Log.d("WHILOGS", "Zip File Download Failure")
                    }
                }
            }
        }

    }

    private fun extractZipFile() {
        var inStream: FileInputStream? = null
        var zipStream: ZipInputStream? = null
        try {
            inStream = FileInputStream(context.filesDir.absolutePath
                    + "/" + zipFileName)
            zipStream = ZipInputStream(BufferedInputStream(inStream))
            var size: Int
            val buffer = ByteArray(2048)
            //Creating Folders for storing Unzipped Data...
            val zipData = if(isExternalDir) File(context.filesDir.toString() + "/" + targetDir)
            else File(targetDir)
            zipData.mkdirs()
            var entry: ZipEntry?
            while (true) {
                entry = zipStream.nextEntry
                if (entry == null) {
                    break
                }
                else {
                    val zipEntryFile = File(zipData.absolutePath + "/"
                            + entry.name.replace("/".toRegex(), "_"))
                    val outStream = FileOutputStream(zipEntryFile)
                    val bufferOut = BufferedOutputStream(outStream, buffer.size)
                    while (zipStream.read(buffer, 0, buffer.size).also { size = it } != -1) {
                        bufferOut.write(buffer, 0, size)
                    }
                    bufferOut.flush()
                    bufferOut.close()
                    Log.d("WHILOGS", "Zip file: ${zipEntryFile.absolutePath}")
                }
            }
            zipStream.close()
            inStream.close()
        }
        catch (e: IOException) {
            listener.zipFileFailed()
            Log.e("WHILOGS", "Zip File Download Failure", e)
        }
        finally {
            try {
                zipStream?.close()
                inStream?.close()
            }
            catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }
}