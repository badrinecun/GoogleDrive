package com.example.googledrivesignindemo

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date


class MainActivity : AppCompatActivity() {
var actualFolderName:String? = null
     var mDrive: Drive? = null
    private lateinit var auth: FirebaseAuth
    lateinit var selectFile: Button

    lateinit var deleteFile: Button
    lateinit var renameFolder: Button
    var folderId: String? = null
    var folderName: String? = null
    @SuppressLint("MissingInflatedId", "SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mDrive = getDriveService(this@MainActivity)
        deleteFile = findViewById(R.id.deleteFile)
        renameFolder = findViewById(R.id.renameFolder)
        // Initialize the DriveClient

        selectFile = findViewById(R.id.selectFile)
        auth = FirebaseAuth.getInstance()
        var text = findViewById<TextView>(R.id.email)
        text.setText(auth.currentUser?.email)
        selectFile.setOnClickListener {

            showFilePicker()
            //uploadFileToGDrive(this@MainActivity, filePath)
        }

        deleteFile.setOnClickListener {
            val sharedPreferences: SharedPreferences =
                getSharedPreferences("file id", Context.MODE_PRIVATE)

            val fileId = sharedPreferences.getString("id_key", null)
            val folder = sharedPreferences.getString("name_key",null)
            println("fileId fileId :$fileId")
            println("folderNameefolderNamee:$folder")
            if(fileId != null && folder != null) {
                lifecycleScope.launch {
                    deleteFile(mDrive!!, fileId,folder)
                }
            }

        }
        renameFolder.setOnClickListener {
            folderId?.let {
                lifecycleScope.launch {

                    folderId = renameFolderInGoogleDrive(folderId!!, "chill123")
                     if(folderId != null && folderName != null) {
                         saveFileIdToSharedPreferences(folderId!!, "chill123")
                     }
                }
            }
        }

    }

    suspend fun renameFolderInGoogleDrive(folderId: String, newFolderName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val existingFolders = mDrive!!.files()
                    .list()
                    .setQ("mimeType='application/vnd.google-apps.folder' and name='$newFolderName'")
                    .execute()
                    .files

                if (existingFolders.isNotEmpty()) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "${newFolderName} folder already exists",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    Log.d(
                        "Google Drive",
                        "Folder already exists: ${existingFolders[0].name} (${existingFolders[0].id})"
                    )
                    folderName = existingFolders[0].name
                    return@withContext existingFolders[0].id
                }else{
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "the folder which you are trying to rename does not exist",
                            Toast.LENGTH_LONG
                        ).show()



                    }
                    return@withContext "the folder which you are trying to rename does not exist"
                }
                val driveService =
                    getDriveService(this@MainActivity) // Replace with your actual activity name
                val file = com.google.api.services.drive.model.File().setName(newFolderName)

                val updatedFile = driveService?.files()?.update(folderId, file)!!.execute()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "$actualFolderName folder renamed with ${newFolderName} ", Toast.LENGTH_LONG)
                        .show()
                }
                System.out.println("Folder ID: ${updatedFile.id}")
                Log.d("Google Drive", "Folder created: ${updatedFile.name} (${updatedFile.id})")
                folderName = updatedFile.name
                return@withContext updatedFile.id
            } catch (e: GoogleJsonResponseException) {
                // Handle the exception appropriately
              runOnUiThread {
                  Toast.makeText(
                      this@MainActivity,
                      "error in   renaming folder, please try again after sometime",
                      Toast.LENGTH_LONG
                  ).show()
              }
                Log.e("rename folder error", "Unable to rename folder: ${e.details}")
                return@withContext null
            }
        }
    }

    fun getDriveService(context: Context): Drive? {
        try {
            GoogleSignIn.getLastSignedInAccount(context).let { googleAccount ->
                val credential = GoogleAccountCredential.usingOAuth2(
                    this, setOf(DriveScopes.DRIVE_FILE)
                )
                credential.selectedAccount = googleAccount!!.account!!
                return Drive.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    JacksonFactory.getDefaultInstance(),
                    credential
                )
                    .setApplicationName(getString(R.string.app_name))
                    .build()
            }
            var tempDrive: Drive
            return tempDrive
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    "some error occured, please try again after some time",
                    Toast.LENGTH_LONG
                ).show()
            }
                return null

        }
    }


    fun uploadFileToGDrive(context: Context, filePath: String) {
        mDrive?.let { googleDriveService ->
            lifecycleScope.launch {
                try {

                    val raunit = File(filePath)
//                    val fileName = "Ticket"
                    //val raunit = File(filePath)
                    //val raunit = File("storage/emulated/0/Download", "sample.pdf")
                    val gfile = com.google.api.services.drive.model.File()
                    gfile.name = raunit.name
                    println("raunit.name:${raunit.name}")
                    val mimetype = "*/*"
                    val fileContent = FileContent(mimetype, raunit)
                    var fileid = ""


                    withContext(Dispatchers.Main) {

                        withContext(Dispatchers.IO) {
                            launch {
                                val mFilee =
                                    googleDriveService!!.Files().create(gfile, fileContent).execute()
                                println("mFile:" + mFilee!!.id)
                                // delete(mFile.id)
//                                println("12345:"+mFilee!!.fullFileExtension)
//                                println("54321:"+mFilee!!.fileExtension)
//                                println("000000:"+mFilee!!.originalFilename)

                                //get original file name along with extension
                                println("999999:"+mFilee!!.name)
                                //saveFileIdToSharedPreferences(mFilee!!.id)
                                //deleteFileById(mFilee!!.id)
                                // deleteFile(googleDriveService,mFilee.id)

                            }
                        }


                    }


                } catch (userAuthEx: UserRecoverableAuthIOException) {
                    startActivity(
                        userAuthEx.intent
                    )
                } catch (e: Exception) {
                   // e.printStackTrace()
                    Log.d("asdf", e.toString())
                    runOnUiThread {
                        Toast.makeText(
                            context,
                            "some error occured in uploading file, please try again after sometime",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

    }


    suspend fun createFolderInGoogleDrive(folderName:String): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Check if a folder with the same name already exists
                val existingFolders = mDrive!!.files()
                    .list()
                    .setQ("mimeType='application/vnd.google-apps.folder' and name='$folderName'")
                    .execute()
                    .files

                if (existingFolders.isNotEmpty()) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "${existingFolders[0].name} folder already exists",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    Log.d("Google Drive", "Folder already exists: ${existingFolders[0].name} (${existingFolders[0].id})")
                    return@withContext existingFolders[0].id
                }

                // Create a file metadata for the folder
                val folderMetadata = com.google.api.services.drive.model.File()
                    .setName(folderName)
                    .setMimeType("application/vnd.google-apps.folder")

                // Create the folder
                val folder = mDrive?.files()?.create(folderMetadata)?.execute()
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "${folder!!.name} folder created",
                        Toast.LENGTH_LONG
                    ).show()
                }
                Log.d("Google Drive", "Folder created: ${folder!!.name} (${folder!!.id})")
                return@withContext folder.id
            } catch (e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "some error occured in creating ${folderName} folder, please try again after sometime",
                        Toast.LENGTH_LONG
                    ).show()
                }
                    Log.e("Google Drive", "Error creating folder", e)
                return@withContext null
            }
        }
    }


    // Function to run code on the main thread
    private fun runOnUiThread(action: () -> Unit) {
        Handler(Looper.getMainLooper()).post(action)
    }



    suspend fun uploadFileToFolder(folderId: String, filePath: String, fileName: String,folderName:String): com.google.api.services.drive.model.File? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                val fileMetadata = com.google.api.services.drive.model.File()
                fileMetadata.name = fileName
                fileMetadata.parents = listOf(folderId)

                val mediaContent = FileContent("*/*", file)
                val uploadedFile = mDrive!!.files().create(fileMetadata, mediaContent)
                    .setFields("id, parents")
                    .execute()
                runOnUiThread {
                    val filee = File(filePath)
                    Toast.makeText(
                        this@MainActivity,
                        "${filee.name} file successfully uploaded in ${folderName} folder successfully ",
                        Toast.LENGTH_LONG
                    ).show()
                }
                Log.d("Google Drive", "File uploaded: ${uploadedFile.name} (${uploadedFile.id})")
                return@withContext uploadedFile
            } catch (e: IOException) {
                val file = File(filePath)
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "error occured in uploading ${file.name} file, please try again after sometime ",
                        Toast.LENGTH_LONG
                    ).show()

                }
                Log.e("Google Drive", "Error uploading file", e)
                return@withContext null
            }
        }
    }



    override fun onResume() {
        super.onResume()

    }

    private fun saveFileIdToSharedPreferences(fileId: String,folderName: String) {
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("file id", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString("id_key", fileId)
        editor.putString("name_key", folderName)
        editor.apply()
        editor.commit()
        Log.d("SharedPreferences", "File ID saved: $fileId and Folder Name saved: $folderName")

        // Optionally, you can also handle the file deletion here using the saved file ID

    }

    private fun showFilePicker() {

        val intent = Intent()
            .setType("*/*")
            .setAction(Intent.ACTION_GET_CONTENT)

        startActivityForResult(Intent.createChooser(intent, "Select a file"), 111)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 111 && resultCode == RESULT_OK) {
            val uri: Uri? = data?.data

            data?.data?.let { uri ->
                handleContentUri(uri)
            }
//
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun handleContentUri(contentUri: Uri) {
        val contentResolver = contentResolver

        // Get file name
        val fileName = getFileName(contentResolver, contentUri)
        Log.d("FileName", "File Name: $fileName")

        // Get file path
        val filePath = getPathFromUri(this@MainActivity, contentUri, fileName)

        println("filePath:$filePath")

        //uploadFileToGDrive(this@MainActivity, filePath!!)
        // Do further processing with file information
//        lifecycleScope.launch {
//            folderId = createFolderInGoogleDrive("test2020")
//            Log.d("folder details123", "folder details:$folderId")
//        }
//
//        folderId?.let {
//            lifecycleScope.launch {
//                val folderDetails = uploadFileToFolder(this@MainActivity,folderId!!, filePath!!)
//                Log.d("uploaded folder details", "folder details:$folderDetails")
//            }
//
//        }
        lifecycleScope.launch {
            actualFolderName = "Hurray123"
            actualFolderName?.let {

                folderId = createFolderInGoogleDrive(actualFolderName!!)
            }


            if (folderId != null && actualFolderName != null) {
                saveFileIdToSharedPreferences(folderId!!,actualFolderName!!)
                val filePathh = filePath
                // Replace with the actual file path
                val file = File(filePathh)
                val fileNamee = file.name// Replace with the desired file name

                val uploadedFile = uploadFileToFolder(folderId!!, filePathh!!, fileNamee,actualFolderName!!)

                if (uploadedFile != null) {
                    Log.d("Google Drive", "File ID: ${uploadedFile.id}")
                    // Continue with your logic here
                } else {
                    Log.e("Google Drive", "Failed to upload file.")
                }
            } else {
                Log.e("Google Drive", "Failed to create folder.")
            }
        }
    }

    private fun getFileName(contentResolver: ContentResolver, fileUri: Uri): String? {
        var name: String? = null
        val returnCursor = contentResolver.query(fileUri, null, null, null, null)
        returnCursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            name = it.getString(nameIndex)
        }
        return name
    }

    fun getPathFromUri(context: Context, uri: Uri, fileName: String?): String? {
        var filePath: String? = null

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, fileName!!)

            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            filePath = file.absolutePath
        } catch (e: Exception) {
           // e.printStackTrace()
            e.toString()
        }

        return filePath
    }


    private suspend fun deleteFile(driveService: Drive, fileId: String,folderNamee: String) {
        withContext(Dispatchers.IO) {
            // Attempt to delete the file
            try {

                driveService.files().delete(fileId).execute()
                runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    "${folderNamee}  deleted successfully",
                    Toast.LENGTH_LONG
                ).show()
            }
                Log.d("delete file or folder","deleted successfully")
                //download file code
//                val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
//                val fileName = simpleDateFormat.format(Date())
//                val file = File(
//                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
//                    "Dwnld"
//                )
//                // Check if the directory exists
//                if (!file.exists()) {
//                    file.mkdirs() // Create the directory if it doesn't exist
//                }
//
//                val filee = File(file, "aaa.jpg")

//                try {
//                    // Check if the file already exists
//                    if (!filee.exists()) {
//                        filee.createNewFile()
//                    } else {
//                        // Handle the situation where the file already exists
//                        // For example, generate a unique filename or overwrite the existing file
//                    }
//                    // Create a FileOutputStream for the file
//                    val fileOutputStream = FileOutputStream(filee)
//                    driveService.files().get(fileId).executeMediaAndDownloadTo(fileOutputStream)
//                    // File deletion successful
//                    // Add your success logic here
//                    println("Successfully deleted file")
//                } catch (e: Exception) {
//                    // Handle IOException or other exceptions
//                    println("An error occurred: $e")
//
//                }
//
            } catch (e: Exception) {
                runOnUiThread {}
                Toast.makeText(
                    this@MainActivity,
                    "error occured in deleting ${folderNamee}, please try again after some time",
                    Toast.LENGTH_LONG
                ).show()
            }
            }
        }
    }



