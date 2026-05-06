package com.lenzbeyer.hathor.ui.nav

object Routes {
    const val HOME           = "home"
    const val TAG_PREVIEW    = "tagPreview"
    const val DOWNLOAD       = "download/{playlistId}"
    const val LIBRARY        = "library"
    const val LIBRARY_DETAIL = "libraryDetail/{playlistId}"
    const val SETTINGS       = "settings"

    fun download(playlistId: String) = "download/$playlistId"
    fun libraryDetail(playlistId: String) = "libraryDetail/$playlistId"
}
