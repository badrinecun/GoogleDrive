package com.example.googledrivesignindemo

import retrofit2.Call
import retrofit2.http.DELETE
import retrofit2.http.Path

interface DriveService {
    @DELETE("drive/v3/files/{fileId}")
    fun deleteFile(@Path("fileId") fileId: String): Call<Void>
}