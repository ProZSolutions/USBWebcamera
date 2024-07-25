/*
 * Copyright 2017-2022 Jiangdg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jiangdg.demo

import android.Manifest
import android.Manifest.permission.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import com.gyf.immersionbar.ImmersionBar
import com.jiangdg.ausbc.utils.ToastUtils
import com.jiangdg.ausbc.utils.Utils
import com.jiangdg.demo.databinding.ActivityMainBinding
import java.io.File
import android.hardware.Camera
import android.hardware.usb.UsbManager


/**
 * Demos of camera usage
 *
 * @author Created by jiangdg on 2021/12/27
 */
class MainActivity : AppCompatActivity() {
    private var mWakeLock: PowerManager.WakeLock? = null
    private var immersionBar: ImmersionBar? = null
    private lateinit var viewBinding: ActivityMainBinding

    private val REQUEST_CODE_PERMISSIONS = 1001
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStatusBar()
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        val (screenWidth, screenHeight) = getScreenDimensions(this)
        try {
            // Check if USB debugging is enabled
            val adbEnabled = Settings.Global.getInt(
                this.contentResolver,
                Settings.Global.ADB_ENABLED,
                0
            ) == 1

            if (!adbEnabled) {
                // USB debugging is not enabled
                Toast.makeText(this, "USB / OTG Debugging is not enabled. Redirecting to settings...", Toast.LENGTH_LONG).show()

                // Create an intent to open the developer options settings page
                startActivity(Intent(Settings.ACTION_SETTINGS))

            } else {
                Toast.makeText(this, "USB Debugging is enabled.", Toast.LENGTH_SHORT).show()

                val camera = Camera.open()
                val previewSizes = camera.parameters.supportedPreviewSizes
                val optimalSize = getOptimalPreviewSize(previewSizes, screenWidth, screenHeight)

                optimalSize?.let {
                    Log.d("FilePath"," screen width "+optimalSize.width+"  height "+optimalSize.height)
                }


                if (!allPermissionsGranted()) {
                    Log.d("FilePath"," permisno no ");
                    ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
                } else {
                    createDCIMFolder()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Environment.isExternalStorageEmulated()) {
                        Log.d("FilePath"," permiss");
                        requestManageExternalStoragePermission()
                    } else {
                        Log.d("FilePath"," view image ")
                        replaceDemoFragment(DemoFragment())

                    }
                } else {
                    Log.d("FilePath"," view image ")
                    replaceDemoFragment(DemoFragment())

                }
            }
        } catch (e: Exception) {
            Log.e("CheckUsbDebugging", "Error checking USB debugging status", e)
            Toast.makeText(this, "An error occurred while checking USB Debugging status.", Toast.LENGTH_LONG).show()
        }


     }


    public fun checkAndPromptForOTG(context: Context) {

    }


    private fun requestManageExternalStoragePermission() {
        val packageName = this?.packageName

        Log.d("FilePath"," read external  ");
        val intent = Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS)
        intent.data = Uri.parse("package:"+packageName)
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode ==100) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Environment.isExternalStorageEmulated()) {
                    replaceDemoFragment(DemoFragment())
                } else {
                    Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
                }
            }
        }else if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                createDCIMFolder()
            } else {
                // Handle the case where permissions are not granted
            }
        }
    }
    private fun createDCIMFolder() {
        val dcimPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val newFolder = File(dcimPath, "ProzUSBCamera")
        Log.d("FilePath"," crewate fol ");
        if (!newFolder.exists()) {
            val wasSuccessful = newFolder.mkdir()
            if (wasSuccessful) {
                // Folder created successfully
                Log.d("FilePath"," cretar ");
            } else {
                // Failed to create folder
                Log.d("FilePath", " no ");
            }
        } else {
            Log.d("FilePath"," already ");
            // Folder already exists
        }
    }
    override fun onStart() {
        super.onStart()
        mWakeLock = Utils.wakeLock(this)
    }

    fun getScreenDimensions(context: Context): Pair<Int, Int> {
        val displayMetrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }
    fun getOptimalPreviewSize(sizes: List<Camera.Size>, targetWidth: Int, targetHeight: Int): Camera.Size? {
        val aspectTolerance = 0.1
        val targetRatio = targetHeight.toDouble() / targetWidth

        var optimalSize: Camera.Size? = null
        var minDiff = Double.MAX_VALUE

        for (size in sizes) {
            val ratio = size.height.toDouble() / size.width.toDouble()
            if (Math.abs(ratio - targetRatio) > aspectTolerance) continue
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size
                minDiff = Math.abs(size.height - targetHeight).toDouble()
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE
            for (size in sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size
                    minDiff = Math.abs(size.height - targetHeight).toDouble()
                }
            }
        }

        return optimalSize
    }
    override fun onStop() {
        super.onStop()
        mWakeLock?.apply {
            Utils.wakeUnLock(this)
        }
    }

    private fun replaceDemoFragment(fragment: Fragment) {
        val hasCameraPermission = PermissionChecker.checkSelfPermission(this, CAMERA)
        val hasStoragePermission =
            PermissionChecker.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
        if (hasCameraPermission != PermissionChecker.PERMISSION_GRANTED || hasStoragePermission != PermissionChecker.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA)) {
                ToastUtils.show(R.string.permission_tip)
            }
            ActivityCompat.requestPermissions(
                this,
                arrayOf(CAMERA, WRITE_EXTERNAL_STORAGE, RECORD_AUDIO),
                REQUEST_CAMERA
            )
            return
        }
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commitAllowingStateLoss()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA -> {
                val hasCameraPermission = PermissionChecker.checkSelfPermission(this, CAMERA)
                if (hasCameraPermission == PermissionChecker.PERMISSION_DENIED) {
                    ToastUtils.show(R.string.permission_tip)
                    return
                }
//                replaceDemoFragment(DemoMultiCameraFragment())
                replaceDemoFragment(DemoFragment())
//                replaceDemoFragment(GlSurfaceFragment())
            }
            REQUEST_STORAGE -> {
                val hasCameraPermission =
                    PermissionChecker.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
                if (hasCameraPermission == PermissionChecker.PERMISSION_DENIED) {
                    ToastUtils.show(R.string.permission_tip)
                    return
                }
                // todo
            }
            else -> {
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        immersionBar= null
    }

    private fun setStatusBar() {
        immersionBar = ImmersionBar.with(this)
            .statusBarDarkFont(false)
            .statusBarColor(R.color.black)
            .navigationBarColor(R.color.black)
            .fitsSystemWindows(true)
            .keyboardEnable(true)
        immersionBar?.init()
    }

    companion object {
        private const val REQUEST_CAMERA = 0
        private const val REQUEST_STORAGE = 1
    }
}