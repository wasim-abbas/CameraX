package com.example.mycamerax

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    lateinit var cameraProvider: ProcessCameraProvider
    var camraOn_Off = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        btnCapture.setOnClickListener {
            takePhoto()
        }
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
        btnSwitch.setOnClickListener(this)
        btnRoundFlash.setOnClickListener(this)

    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return
        // Create time-stamped output file to hold the image
        val photFile =
            File(
                outputDirectory,
                SimpleDateFormat(
                    FILENAME_FORMAT,
                    Locale.US
                ).format(System.currentTimeMillis()) + ".jpg"
            )
        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback(),
                ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val saveFile = Uri.fromFile(photFile)
                    val msg = "Photo Capture Sucessfully: $saveFile"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }

            }
        )


    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
             cameraProvider = cameraProviderFuture.get()
            // Preview
            val preview = Preview.Builder()
                .build().also {
                    it.setSurfaceProvider(cameraPreview.surfaceProvider)
                }
            imageCapture = ImageCapture.Builder()
                .build()

            // Select back camera as a default
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                val camera=cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
                camera.cameraInfo.hasFlashUnit()
                camera.cameraControl.enableTorch(camraOn_Off)



            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))


    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull().let {
            File(it, resources.getString(R.string.app_name)).apply {
                mkdir()
            }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraX"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

    }

    fun switchCamera() {
        if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            startCamera()
        } else if(cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA){
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            startCamera()
        }

    }
    fun  btnFash(){
        if(camraOn_Off == true)
        {
            camraOn_Off = false
            val camera=cameraProvider.bindToLifecycle(
                this, cameraSelector, imageCapture)
            camera.cameraInfo.hasFlashUnit()
            camera.cameraControl.enableTorch(camraOn_Off)
        }else if(camraOn_Off == false)
        {
            camraOn_Off=true
            val camera=cameraProvider.bindToLifecycle(
                this, cameraSelector, imageCapture
            )
            camera.cameraInfo.hasFlashUnit()
            camera.cameraControl.enableTorch(camraOn_Off)
        }

    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.btnSwitch -> switchCamera()
            R.id.btnRoundFlash -> btnFash()
        }
    }

}