package com.vais.mavicmissions.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.android.Utils;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

public class VisionHelper {
    private Context context;

    private boolean openCVLoaded;

    private BaseLoaderCallback cvLoaderCallback;

    public VisionHelper(Context context) {
        this.context = context;

        cvLoaderCallback = new BaseLoaderCallback(context) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);
                if (status == LoaderCallbackInterface.SUCCESS)
                    openCVLoaded = true;
                else {
                    super.onManagerConnected(status);
                }
            }
        };
    }

    public void initCV() {
        if (!OpenCVLoader.initDebug())
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, context, cvLoaderCallback);
        else
            cvLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    public Bitmap matToBitmap(Mat src) {
        Bitmap result = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src, result);
        return result;
    }

    public Mat bitmapToMap(Bitmap src) {
        Mat result = new Mat();
        Utils.bitmapToMat(src, result);
        return result;
    }

    public Mat toGrayscale(Mat src) {
        Mat result = new Mat();

        if (openCVLoaded) {
            Imgproc.cvtColor(src, result, Imgproc.COLOR_RGB2GRAY);
            return result;
        }
        else
            return null;
    }

    public Mat erode(Mat src, int maskSize) {
        Mat result = new Mat();
        Imgproc.erode(src, result, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(maskSize, maskSize)));

        return result;
    }

    public Mat dilate(Mat src, int maskSize) {
        Mat result = new Mat();
        Imgproc.erode(src, result, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(maskSize, maskSize)));

        return result;
    }

    public Mat open(Mat src, int maskSize) {
        return dilate(erode(src, maskSize), maskSize);
    }

    public Mat close(Mat src, int maskSize) {
        return erode(dilate(src, maskSize), maskSize);
    }
}