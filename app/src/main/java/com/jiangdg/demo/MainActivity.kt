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
import android.content.IntentFilter
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
 import androidx.fragment.app.Fragment
import com.gyf.immersionbar.ImmersionBar
import com.jiangdg.ausbc.utils.ToastUtils
import com.jiangdg.ausbc.utils.Utils
 import java.io.File
import android.hardware.Camera
import android.hardware.usb.UsbManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.content.getSystemService
import com.jiangdg.ausbc.R
import com.jiangdg.ausbc.databinding.ActivityMainBinding


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

/*     fun getScreenResolution(context: Context): Pair<Int, Int> {
        val displayMetrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        return Pair(width, height)
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStatusBar()
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        //val (screenWidth, screenHeight) = getScreenDimensions(this)
//        val (width, height) = getScreenResolution(this)
      //  Log.d("ScreenResolution"," with "+width+"   height "+height);




        try {


               /* if (!allPermissionsGranted()) {
                    Log.d("FilePath"," permisno not granded ");
                    //ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
                    if (isManageExternalStoragePermissionGranted()) {
                        // Permission is granted, proceed with your file operations
                    } else {
                        // Request permission
                        requestManageExternalStoragePermission()
                    }
                } else {
                    Log.d("FilePath"," permisison grande ");
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

                }*/


            if (arePermissionsGranted()) {
                Log.d("FilePermisison"," all granded ");
                // Permissions are granted, proceed with your logic
            } else {
                requestPermissionsNew()
            }


            //}
        } catch (e: Exception) {
            Log.d("FilePath", "Error checking USB debugging status", e)
            Toast.makeText(this,  e.message, Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }


     }

    private fun requestPermissionsNew() {
        Log.d("FilePermisison"," request file permission ");
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO),
            REQUEST_CODE_PERMISSIONS)
    }
    private fun arePermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }


    private fun isManageExternalStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            // For Android versions below 11, manage external storage is not applicable
            true
        }
    }


    private fun requestManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            try {
                startActivityForResult(intent, 1200)
            } catch (e: Exception) {
                // Handle the case where the intent action is not supported
                // This might happen on certain devices or custom ROMs
                // You might need to inform the user or handle this gracefully
            }
        } else {
            // For versions below Android 11, request regular storage permissions if needed
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("requestPernmiss"," on activity result "+requestCode+" res "+resultCode+" data "+data)
        if (requestCode ==100) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Environment.isExternalStorageEmulated()) {
                    replaceDemoFragment(DemoFragment())
                } else {
                    Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
                }
            }
        }else if (requestCode == REQUEST_CODE_PERMISSIONS) {
            Log.d("FilePath"," on activit "+allPermissionsGranted());
            if (allPermissionsGranted()) {
                createDCIMFolder()
            } else {
                Log.d("FilePath"," permission not granded ");
                // Handle the case where permissions are not granted
            }
        }else if (requestCode == 1200) {
            if (isManageExternalStoragePermissionGranted()) {
                // Permission granted, proceed with your file operations
            } else {
                // Permission denied, handle accordingly
            }
        }
    }
    private fun createDCIMFolder() {
        val dcimPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val newFolder = File(dcimPath, "ProzUSBCamera")
        Log.d("FilePathrror"," crewate fol ");
        if (!newFolder.exists()) {
            val wasSuccessful = newFolder.mkdir()
            if (wasSuccessful) {
                // Folder created successfully
                Log.d("FilePathrror"," cretar ");
            } else {
                // Failed to create folder
                Log.d("FilePathrror", " no ");
            }
        } else {
            Log.d("FilePathrror"," already ");
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
       /* val hasCameraPermission = PermissionChecker.checkSelfPermission(this, CAMERA)
        val hasStoragePermission =
            PermissionChecker.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
        Log.d("requestPernmiss"," replace has cam " +hasCameraPermission+" storage "+hasStoragePermission);

        if (hasCameraPermission != PermissionChecker.PERMISSION_GRANTED || hasStoragePermission != PermissionChecker.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA)) {
                ToastUtils.show(R.string.permission_tip)
            }else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                        addCategory(Intent.CATEGORY_DEFAULT)
                        data = Uri.parse("package:$packageName")
                    }
                    startActivityForResult(intent, 150)
                }
            }
            Log.d("requestPernmiss"," replace Demo Frag" );
             ActivityCompat.requestPermissions(
                this,
                arrayOf(CAMERA, WRITE_EXTERNAL_STORAGE, RECORD_AUDIO),
                REQUEST_CAMERA
            )
            return
        }*/
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commitAllowingStateLoss()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.d("requestPernmiss"," resul" +requestCode+"  camera "+ REQUEST_CAMERA+"  storage "+
        REQUEST_STORAGE);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("FilePermisison"," permission granded ")
                createDCIMFolder()
                replaceDemoFragment(DemoFragment())
                // Permission granted, proceed with your logic
            } else {
                // Permission denied, show a message to the user
                Log.d("FilePermisison"," permission not granded "+grantResults.size);
                createDCIMFolder()
                replaceDemoFragment(DemoFragment())
            }
        }else{
        when (requestCode) {
            REQUEST_CAMERA -> {
                Log.d("requestPernmiss", " request came ");
                val hasCameraPermission = PermissionChecker.checkSelfPermission(this, CAMERA)
                if (hasCameraPermission == PermissionChecker.PERMISSION_DENIED) {
                    ToastUtils.show(R.string.permission_tip)
                    return
                }
                replaceDemoFragment(DemoFragment())
            }

            REQUEST_STORAGE -> {
                Log.d("requestPernmiss", " storage ");
                val hasCameraPermission =
                    PermissionChecker.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
                if (hasCameraPermission == PermissionChecker.PERMISSION_DENIED) {
                    ToastUtils.show(R.string.permission_tip)
                    return
                }
                replaceDemoFragment(DemoFragment())
                // todo
            }

            else -> {
            }
        }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        immersionBar= null
        //unregisterReceiver(usbReceiver)


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