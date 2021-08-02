package com.example.ocrscanner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.ocrscanner.db.Result
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CAMERA = 2
        private const val REQUEST_STORAGE = 3
        private const val REQUEST_FILE = 5
        private const val TAG = "CameraXBasicLog"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    private lateinit var result: Result

    private lateinit var pref: SharedPreferences
    private var cropMode = false

    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var camera: Camera
    private var flashMode = ImageCapture.FLASH_MODE_OFF

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pref = getSharedPreferences("Pref", MODE_PRIVATE)
        cropMode = pref.getBoolean("CROP_MODE", false)
        flashMode = ImageCapture.FLASH_MODE_OFF

        // Request camera permissions
        checkAndRequestCamera()

        btnShoot?.setOnClickListener { takePhoto() }

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        ivDocument?.setOnClickListener {
            if (cropMode)
                CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .start(this)
            else
                checkAndRequestStorage()
        }

        ivHistory?.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        ivFlash?.setOnClickListener {
            flashMode = if (flashMode == ImageCapture.FLASH_MODE_ON) {
                ivFlash?.setImageResource(R.drawable.ic_flash_off)
                ImageCapture.FLASH_MODE_OFF
            }
            else {
                ivFlash?.setImageResource(R.drawable.ic_flash_on)
                ImageCapture.FLASH_MODE_ON
            }
        }

        ivSettings?.setOnClickListener { chooseLoadImageMode() }

        sbZoom?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                camera.cameraControl.setLinearZoom(progress / 100f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA -> {
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    startCamera()
                } else {
                    Log.d("PermissionLog", "granted = false")
                }
                return
            }
            REQUEST_STORAGE -> {
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    loadImageFile()
                } else {
                    Log.d("PermissionLog", "granted = false")
                }
                return
            }
            else -> {
                Log.d("PermissionLog", "onRequestPermissionsResult: else")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            result = Result()
            when (requestCode) {
                REQUEST_FILE -> {
                    val selectedImage = data?.data
                    result.pathUrl = selectedImage?.toString() ?:""
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImage)

                    val image: FirebaseVisionImage
                    try {
                        image = FirebaseVisionImage.fromBitmap(bitmap)
                        runTextRecognition(image)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {
                    result.pathUrl = CropImage.getActivityResult(data).uri.toString()
                    val image: FirebaseVisionImage
                    try {
                        image = FirebaseVisionImage.fromFilePath(this, Uri.parse(result.pathUrl))
                        runTextRecognition(image)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }

        } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
            Log.e("ErrorLog", "Error: ${CropImage.getActivityResult(data).error}")
        }
    }

    // ------------------------------------ Class Functions ----------------------------------------

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return
        imageCapture.flashMode = flashMode

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.getDefault()
            ).format(System.currentTimeMillis()) + ".jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    result = Result()
                    val savedUri = Uri.fromFile(photoFile)
                    result.pathUrl = savedUri.toString()
                    val msg = "Photo capture succeeded: $savedUri"

                    val image: FirebaseVisionImage
                    try {
                        image = FirebaseVisionImage.fromFilePath(this@MainActivity, savedUri)
                        runTextRecognition(image)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    Log.d(TAG, msg)
                }
            })
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                        Log.d("CameraXBasic", "Average luminosity: $luma")
                    })
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    private fun runTextRecognition(image: FirebaseVisionImage) {
        val detector = FirebaseVision.getInstance().onDeviceTextRecognizer
        detector.processImage(image)
            .addOnSuccessListener { firebaseVisionText ->
                getResult(firebaseVisionText.text)
                Log.d(TAG, "firebaseVisionText: ${firebaseVisionText.text}")
            }
            .addOnFailureListener { e ->
                Log.d(TAG, e.message.toString())
            }
    }

    private fun getResult(text: String) {
        val sdf = SimpleDateFormat("yyyy/MM/dd hh:mm:ss")
        val calendar = Calendar.getInstance()
        val date = Date()
        date.time = calendar.timeInMillis
        result.time = sdf.format(date)
        result.content = text

        Log.d("ResultLog", "Result: $result")

        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra("RESULT", result)
        startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkAndRequestCamera() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA
            ) -> {
                startCamera()
            }
            else -> {
                Log.d("PermissionLog", "else")
                requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkAndRequestStorage() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) -> {
                loadImageFile()
            }
            else -> {
                Log.d("PermissionLog", "else")
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_STORAGE)
            }
        }
    }

    private fun loadImageFile() {
        val intent = Intent()
        intent.type = "*/*"
        intent.action = Intent.ACTION_PICK
        startActivityForResult(Intent.createChooser(intent, "Chọn File"), REQUEST_FILE)
    }

    private fun chooseLoadImageMode() {
        val ivSettings = findViewById<ImageView>(R.id.ivSettings)
        val popupMenu = PopupMenu(this, ivSettings)
        popupMenu.menu.add(0, 0, 0, "No crop")
        popupMenu.menu.add(0, 1, 0, "Crop")
        popupMenu.setOnMenuItemClickListener { item ->
            cropMode = item.itemId != 0
            pref.edit().putBoolean("CROP_MODE", cropMode).apply()
            true
        }
        popupMenu.show()

    }

    //--------------------------------------- Inner Class ------------------------------------------

    private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        override fun analyze(image: ImageProxy) {

            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map { it.toInt() and 0xFF }
            val luma = pixels.average()

            listener(luma)

            image.close()
        }
    }
}