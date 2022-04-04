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
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class VisionHelper {
    private static int CONTOURS_THRESHOLD = 150;

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
        Imgproc.cvtColor(src, result, Imgproc.COLOR_RGB2GRAY);

        return result;
    }

    public Mat smooth(Mat src, int maskSize) {
        Mat result = new Mat();
        Imgproc.GaussianBlur(src, result, new Size(maskSize, maskSize), 0, 0);

        return result;
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

    public Mat prepareContourDetection(Mat src) {
        src = toGrayscale(src);
        src = smooth(src, 15);

        Mat result = new Mat();
        Mat resultNorm = new Mat();
        Mat resultNormScaled = new Mat();
        Imgproc.cornerHarris(src, result, 2, 3, 0.04);

        Core.normalize(result, resultNorm, 0, 255, Core.NORM_MINMAX);
        Core.convertScaleAbs(resultNorm, resultNormScaled);

        return resultNormScaled;
    }

    public Mat prepareCornerDetection(Mat src) {
        Mat result = src;
        result = toGrayscale(result);
        result = smooth(result, 15);

        return result;
    }

    public MatOfPoint detectCorners(Mat src, int maxCorner) {
        // Détecter les coins.
        MatOfPoint corners = new MatOfPoint();
        Imgproc.goodFeaturesToTrack(src, corners, maxCorner, 0.01, 0.01);

        return corners;
    }

    public MatOfPoint detectCorners(Mat src, int maxCorner, float quality) {
        // Détecter les coins.
        MatOfPoint corners = new MatOfPoint();
        Imgproc.goodFeaturesToTrack(src, corners, maxCorner, quality, 0.01);

        return corners;
    }

    public MatOfPoint detectCorners(Mat src, int maxCorner, int minDistance) {
        // Détecter les coins.
        MatOfPoint corners = new MatOfPoint();
        Imgproc.goodFeaturesToTrack(src, corners, maxCorner, 0.01, minDistance);

        return corners;
    }

    public MatOfPoint detectCorners(Mat src, int maxCorner, float quality, int minDistance) {
        // Détecter les coins.
        MatOfPoint corners = new MatOfPoint();
        Imgproc.goodFeaturesToTrack(src, corners, maxCorner, quality, minDistance);

        return corners;
    }

    public List<MatOfPoint> contoursDetection(Mat src) {
        // Trouver les contours.
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Mat binary = new Mat();
        Imgproc.threshold(src, binary, CONTOURS_THRESHOLD, CONTOURS_THRESHOLD, Imgproc.THRESH_BINARY_INV);
        Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        return contours;
    }

    public MatOfPoint getBiggerContour(List<MatOfPoint> contours) {
        MatOfPoint biggerContour = null;
        int biggerRowCount = 0;
        for (MatOfPoint contour : contours) {
            if (contour.rows() > biggerRowCount) {
                biggerRowCount = contour.rows();
                biggerContour = contour;
            }
        }

        return biggerContour;
    }

    public MatOfPoint getSmallerContour(List<MatOfPoint> contours) {
        MatOfPoint smallestContour = null;
        int smallestContourCount = Integer.MAX_VALUE;
        for(MatOfPoint contour : contours) {
            if (contour.rows() < smallestContourCount)
                smallestContourCount = contour.rows();
            smallestContour = contour;

        }

        return smallestContour;
    }

    public Mat drawPoly(Mat src, MatOfPoint points) {
        List<MatOfPoint> list = new ArrayList<>();
        list.add(points);

        Imgproc.fillPoly(src, list, new Scalar(0, 0, 0, 255), 8);

        return src;
    }
}