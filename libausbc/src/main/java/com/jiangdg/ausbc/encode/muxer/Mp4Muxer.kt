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
package com.jiangdg.ausbc.encode.muxer

import android.content.ContentValues
import android.content.Context
import android.hardware.Camera
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.format.DateUtils
import android.util.Log
import androidx.core.content.FileProvider
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.utils.Logger
import com.jiangdg.ausbc.utils.MediaUtils
import com.jiangdg.ausbc.utils.ToastUtils
import com.jiangdg.ausbc.utils.Utils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

/**
 * MediaMuxer for Mp4
 *
 * @property path mp4 saving path
 * @property durationInSec mp4 file auto divided in seconds
 *
 * @constructor
 * @param context context
 * @param callBack mp4 capture status, see [ICaptureCallBack]
 *
 * @author Created by jiangdg on 2022/2/10
 */
class Mp4Muxer(
    context: Context?,
    callBack: ICaptureCallBack,
    private var path: String? = null,
    private val durationInSec: Long = 0,
    private val isVideoOnly: Boolean = false
) {
    private var mContext: Context? = null
    private var mMediaMuxer: MediaMuxer? = null
    private var mFileSubIndex: Int = 0
    @Volatile
    private var mVideoTrackerIndex = -1
    @Volatile
    private var mAudioTrackerIndex = -1
    private var mVideoFormat: MediaFormat? = null
    private var mAudioFormat: MediaFormat? = null
    private var mBeginMillis: Long = 0
    private var mCaptureCallBack: ICaptureCallBack? = null
    private var mMainHandler: Handler = Handler(Looper.getMainLooper())
    private var mOriginalPath: String? = null
    private var mVideoPts: Long = 0L
    private var mAudioPts: Long = 0L
    private val mDateFormat by lazy {
        SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault())
    }
    private val mCameraDir by lazy {
        Log.d("FilePathrror"," mp4 fun calle ")
        "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)}/ProzUSBCamera"
    }

    init {
        Log.d("Keerthi","mp4muxer");
        this.mCaptureCallBack = callBack
        this.mContext= context
        try {
            if (path.isNullOrEmpty()) {
                val date = mDateFormat.format(System.currentTimeMillis())
                path = "$mCameraDir/VID_JJCamera_$date"
            }
            Log.d("FilePathrror"," mp4 mu "+path);
            mOriginalPath = path
            path = "${path}.mp4"
            ToastUtils.show(path!!);
            mMediaMuxer = MediaMuxer(path!!, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: Exception) {
            mCaptureCallBack?.onError(e.localizedMessage)
            Logger.e(TAG, "init media muxer failed, err = ${e.localizedMessage}", e)
        }
    }

    /**
     * Add tracker
     *
     * @param mediaFormat media format, see [MediaFormat]
     * @param isVideo media type, audio or video
     */
    @Synchronized
    fun addTracker(mediaFormat: MediaFormat?, isVideo: Boolean) {
        Log.d("Keerthi","djkdvsbdsvikdafjkdfjkadjkd");
        if (isMuxerStarter() || mediaFormat == null) {
            return
        }
        try {

            mMediaMuxer?.apply {
                val tracker = addTrack(mediaFormat)
                if (Utils.debugCamera) {
                    Logger.i(TAG, "addTracker index = $tracker isVideo = $isVideo")
                }
                if (isVideo) {
                    mVideoFormat = mediaFormat
                    mVideoTrackerIndex = tracker
                    if (mAudioTrackerIndex != -1 || isVideoOnly) {
                        start()
                        mMainHandler.post {
                            mCaptureCallBack?.onBegin()
                        }
                        mBeginMillis = System.currentTimeMillis()
                        if (Utils.debugCamera) {
                            Logger.i(TAG, "start media muxer")
                        }
                    }
                } else {
                    mAudioFormat = mediaFormat
                    mAudioTrackerIndex = tracker
                    if (mVideoTrackerIndex != -1) {
                        start()
                        mMainHandler.post {
                            mCaptureCallBack?.onBegin()
                        }
                        mBeginMillis = System.currentTimeMillis()
                        if (Utils.debugCamera) {
                            Logger.i(TAG, "start media muxer")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            release()
            mMainHandler.post {
                mCaptureCallBack?.onError(e.localizedMessage)
            }
            Logger.e(TAG, "addTracker failed, err = ${e.localizedMessage}", e)
        }
    }

    /**
     * write audio(aac) or video(h264) data to media muxer
     *
     * @param outputBuffer encode output buffer, see [MediaCodec]
     * @param bufferInfo encode output buffer info, see [MediaCodec.BufferInfo]
     * @param isVideo media data type, audio or video
     */
    @Synchronized
    fun pumpStream(outputBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo, isVideo: Boolean) {
        Log.d("Keerthi","16ffj sdk ks");
        try {
            if (!isMuxerStarter()) {
                return
            }
            if (bufferInfo.size <= 0) {
                return
            }
            val index = if (isVideo) {
                if (mVideoPts == 0L) {
                    mVideoPts = bufferInfo.presentationTimeUs
                }
                bufferInfo.presentationTimeUs = bufferInfo.presentationTimeUs - mVideoPts
                mVideoTrackerIndex
            } else {
                if (mAudioPts == 0L) {
                    mAudioPts = bufferInfo.presentationTimeUs
                }
                bufferInfo.presentationTimeUs = bufferInfo.presentationTimeUs - mAudioPts
                mAudioTrackerIndex
            }
            outputBuffer.position(bufferInfo.offset)
            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
            mMediaMuxer?.writeSampleData(index, outputBuffer, bufferInfo)
            saveNewFileIfNeed()
        } catch (e: Exception) {
            Logger.e(TAG, "pumpStream failed, err = ${e.localizedMessage}", e)
        }
    }

    private fun saveNewFileIfNeed() {
        Log.d("Keerthi","1kjbewiuubk vdaibbkj ew6");
        try {
            val endMillis = System.currentTimeMillis()
            if (durationInSec == 0L) {
                return
            }
            if (endMillis - mBeginMillis <= durationInSec * 1000) {
                return
            }

            mMediaMuxer?.stop()
            mMediaMuxer?.release()
            mMediaMuxer = null
            mAudioTrackerIndex = -1
            mVideoTrackerIndex = -1
            mAudioPts = 0L
            mVideoPts = 0L
            insertDCIM(mContext, path)

            path = "${mOriginalPath}_${++mFileSubIndex}.mp4"
            mMediaMuxer = MediaMuxer(path!!, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            addTracker(mVideoFormat, true)
            addTracker(mAudioFormat, false)
        } catch (e: Exception) {
            mMainHandler.post {
                mCaptureCallBack?.onError(e.localizedMessage)
            }
            Logger.e(TAG, "release media muxer failed, err = ${e.localizedMessage}", e)
        }
    }

    /**
     * Release mp4 muxer resource
     */
    @Synchronized
    fun release() {
        Log.d("Keerthi","mv skg , msdkj k v ");
        try {
             mMediaMuxer?.stop()
            mMediaMuxer?.release()
            insertDCIM(mContext, path, true)
            Logger.i(TAG, "stop media muxer")
        } catch (e: Exception) {
            mMainHandler.post {
                mCaptureCallBack?.onError(e.localizedMessage)
            }
            Logger.e(TAG, "release media muxer failed, err = ${e.localizedMessage}", e)
        } finally {
            mMediaMuxer = null
            mAudioTrackerIndex = -1
            mVideoTrackerIndex = -1
            mAudioPts = 0L
            mVideoPts = 0L
        }
    }

    fun getSavePath() = path

    private fun insertDCIM(context: Context?, videoPath: String?, notifyOut: Boolean = false) {
        Log.d("FilePathrror"," inssert DCIM ")
        context?.let { ctx ->
            if (videoPath.isNullOrEmpty()) {
                return
            }

            val videoFile = File(videoPath)
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, videoFile.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
                put(MediaStore.Video.Media.RELATIVE_PATH,"DCIM/ProzUSBCamera")
             }

            // Insert the new video into MediaStore
            val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val newUri = ctx.contentResolver.insert(uri, values)
            Log.d("FilePathrror"," new URI "+newUri)
            if (newUri != null) {
                // Copy the video file to the new URI
             /*   ctx.contentResolver.openOutputStream(newUri).use { outputStream ->
                    FileInputStream(videoFile).use { inputStream ->
                        inputStream.copyTo(outputStream ?: throw IOException("Failed to open output stream"))
                    }
                }*/

                // Notify completion
                     mMainHandler.post {
                         Log.d("FilePathrror"," path "+videoPath)
                        mCaptureCallBack?.onComplete(videoPath)
                    }

            } else {
                // Handle insertion failure
                Log.d("FilePathrror", "Failed to insert video into MediaStore")
            }
        }
    }

    /*private fun insertDCIM(context: Context?, videoPath: String?, notifyOut: Boolean = false) {
        context?.let { ctx ->
            if (videoPath.isNullOrEmpty()) {
                return
            }
            ctx.contentResolver.let { content ->
                val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                content.insert(uri, getVideoContentValues(videoPath))
                mMainHandler.post {
                    mCaptureCallBack?.onComplete(this.path)
                }
            }
        }
    }*/



    fun getUriFromFilePath(context: Context, filePath: String): Uri {
        val file = File(filePath)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }



    fun isMuxerStarter() = mVideoTrackerIndex != -1 && (mAudioTrackerIndex != -1 || isVideoOnly)

    private fun getLocalVideoDuration(filePath: String?): Long {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(filePath)
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?:0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }


    companion object {
        private const val TAG = "Mp4Muxer"
    }
}