package com.jeongari.mask_or_not

import android.app.Activity
import android.os.SystemClock
import android.util.Log
import timber.log.Timber


class ImageClassifierFloatException private constructor(
    activity: Activity,
    imageSizeX: Int,
    imageSizeY: Int,
    private val outputW: Int,
    private val outputFingerPoseDrawViewH: Int,
    modelPath: String,
    numBytesPerChannel: Int = 4 // a 32bit float value requires 4 bytes
) :ImageClassifier(activity, imageSizeX, imageSizeY, modelPath, numBytesPerChannel) {

    /**
     * An array to hold inference results, to be feed into Tensorflow Lite as outputs.
     * This isn't part of the super class, because we need a primitive array here.
     */
    private val heatMapArray: Array<FloatArray> = Array(1){ FloatArray(2) }


    override fun addPixelValue(pixelValue: Int) {
//        //bgr
//        imgData!!.putFloat(((pixelValue and 0xFF).toFloat()))
//        imgData!!.putFloat(((pixelValue shr 8 and 0xFF).toFloat()))
//        imgData!!.putFloat(((pixelValue shr 16 and 0xFF).toFloat()))
        //rgb
        imgData!!.putFloat(((pixelValue shr 16 and 0xFF).toFloat()))
        imgData!!.putFloat(((pixelValue shr 8 and 0xFF).toFloat()))
        imgData!!.putFloat(((pixelValue and 0xFF).toFloat()))

    }


    override fun runInference() {
        val modelStartTime = SystemClock.uptimeMillis()
        tflite?.run(imgData!!, heatMapArray)
        val modelEndTime = SystemClock.uptimeMillis()

        probMask = heatMapArray[0][0]
        probNonMask = heatMapArray[0][1]

        Log.d("Inference Result", heatMapArray.contentDeepToString())

//        Log.d("Inference Time", (modelEndTime - modelStartTime).toString())
    }


    companion object {
        val TAG = "FpeFloatException"


        /**
         * Create ImageClassifierFloatInception instance
         *
         * @param imageSizeX Get the image size along the x axis.
         * @param imageSizeY Get the image size along the y axis.
         * @param outputW The output width of model
         * @param outputH The output height of model
         * @param modelPath Get the name of the model file stored in Assets.
         * @param numBytesPerChannel Get the number of bytes that is used to store a single
         * color channel value.
         */
        fun create(
            activity: Activity,
            imageSizeX: Int = 224,
            imageSizeY: Int = 224,
            outputW: Int = 112,
            outputH: Int = 112,
            modelPath: String = "model_unquant.tflite",
            numBytesPerChannel: Int = 4
        ): ImageClassifierFloatException =
            ImageClassifierFloatException(
                activity,
                imageSizeX,
                imageSizeY,
                outputW,
                outputH,
                modelPath,
                numBytesPerChannel)
    }
}