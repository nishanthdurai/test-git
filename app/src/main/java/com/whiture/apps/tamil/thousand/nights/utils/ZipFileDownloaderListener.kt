package com.whiture.apps.tamil.thousand.nights.utils

// listener for zip file downloader utility
abstract class ZipFileDownloaderListener() {
    // the zip file is getting downloaded with the progress in percentage
    abstract fun zipFileDownloading(percentage: Int)
    // event method for zip file download process completion - it could be SUCCESS or FAIL
    abstract fun zipFileDownloadCompleted()
    // event method for failure zip file download
    abstract fun zipFileFailed()
}