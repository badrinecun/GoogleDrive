package com.example.googledrivesignindemo

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DriveServiceHelper {
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val driveService: DriveService = retrofit.create(DriveService::class.java)

    // Assuming you have the fileId you want to delete
    fun deleteFile(fileId: String, callback: DriveCallback) {
        val call = driveService.deleteFile(fileId)

        // Execute the call asynchronously
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    // File deletion successful
                    callback.onSuccess()
                } else {
                    // Handle the error
                    callback.onError(response.message())
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Handle network or unexpected errors
                callback.onError(t.message ?: "Unknown error")
            }
        })
    }

    interface DriveCallback {
        fun onSuccess()
        fun onError(errorMessage: String)
    }
}