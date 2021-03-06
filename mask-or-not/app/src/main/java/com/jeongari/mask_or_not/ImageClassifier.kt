package com.jeongari.mask_or_not

import android.app.Activity
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.Interpreter
import timber.log.Timber
import java.io.FileInputStream
import java.io.IOException
import java.lang.Long
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

abstract class ImageClassifier
/** Initializes an `FingerPoseClassifier`.  */
@Throws(IOException::class)
internal constructor(
    activity: Activity,
    val imageSizeX: Int, // Get the image size along the x axis.
    val imageSizeY: Int, // Get the image size along the y axis.
    private val modelPath: String, // Get the name of the model file stored in Assets.
    // Get the number of bytes that is used to store a single color channel value.
    numBytesPerChannel: Int
){

    /* Preallocated buffers for storing image data in. */
    private val intValues = IntArray(imageSizeX * imageSizeY)

    /** An instance of the driver class to run model inference with Tensorflow Lite.  */
    protected var tflite: Interpreter? = null

    /** A ByteBuffer to hold image data, to be feed into Tensorflow Lite as inputs.  */
    var imgData: ByteBuffer? = null

    var probMask : Float ?= 0.0f
    var probNonMask : Float ?= 0.0f


    init {
        tflite = Interpreter(loadModelFile(activity))
        imgData = ByteBuffer.allocateDirect(
            DIM_BATCH_SIZE
                    * imageSizeX
                    * imageSizeY
                    * DIM_PIXEL_SIZE
                    * numBytesPerChannel
        )
        imgData!!.order(ByteOrder.nativeOrder())
        Timber.d("Created a Tensorflow Lite Image Classifier.")
    }

    /** Classifies a frame from the preview stream.  */
    public fun classifyFrame(bitmap: Bitmap): String {
        if (tflite == null) {
            Timber.e("Image classifier has not been initialized; Skipped.")
            return "Uninitialized Classifier."
        }
        convertBitmapToByteBuffer(bitmap)
        // Here's where the magic happens!!!
        val startTime = SystemClock.uptimeMillis()
        runInference()
        val endTime = SystemClock.uptimeMillis()
        Timber.d("Timecost to run model inference: " + Long.toString(endTime - startTime))

        return Long.toString(endTime - startTime) + "ms"
    }


    /** Closes tflite to release resources.  */
    fun close() {
        tflite!!.close()
        tflite = null
    }

    /** Memory-map the model file in Assets.  */
    @Throws(IOException::class)
    private fun loadModelFile(activity: Activity): MappedByteBuffer {
        val fileDescriptor = activity.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /** Writes Image data into a `ByteBuffer`.  */
    private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
        if (imgData == null) {
            return
        }
        imgData!!.rewind()
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        // Convert the image to floating point.
        var pixel = 0
        val startTime = SystemClock.uptimeMillis()
        for (i in 0 until imageSizeX) {
            for (j in 0 until imageSizeY) {
                val v = intValues[pixel++]
                addPixelValue(v)
            }
        }
        val endTime = SystemClock.uptimeMillis()
        Timber.d("Timecost to put values into ByteBuffer: " + Long.toString(endTime - startTime))
    }

    /**
     * Add pixelValue to byteBuffer.
     *
     * @param pixelValue
     */
    protected abstract fun addPixelValue(pixelValue: Int)

    /**
     * Read the probability value for the specified label This is either the original value as it was
     * read from the net's output or the updated value after the filter was applied.
     *
     * @param labelIndex
     * @return
     */
    protected abstract fun runInference()

    companion object {

        /** Tag for the [Log].  */
        private const val TAG = "Pose"

        /** Number of results to show in the UI.  */
        private const val RESULTS_TO_SHOW = 3

        /** Dimensions of inputs.  */
        private const val DIM_BATCH_SIZE = 1

        private const val DIM_PIXEL_SIZE = 3

    }

}