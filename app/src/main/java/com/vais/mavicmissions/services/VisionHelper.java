package com.vais.mavicmissions.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.vais.mavicmissions.Enum.Color;
import com.vais.mavicmissions.R;

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
    private static final int CONTOURS_THRESHOLD = 150;

    private Context context;
    private BaseLoaderCallback cvLoaderCallback;

    private Scalar lowerGreen;
    private Scalar upperGreen;

    private Scalar lowerYellow;
    private Scalar upperYellow;

    private Scalar lowerBallGreen;
    private Scalar upperBallGreen;

    private Scalar lowerBlack;
    private Scalar upperBlack;

    public VisionHelper(Context context) {
        this.context = context;

        cvLoaderCallback = new BaseLoaderCallback(context) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);
                if (status != LoaderCallbackInterface.SUCCESS)
                    super.onManagerConnected(status);
            }
        };

        // Définir les limites du vert.
        lowerGreen = new Scalar(32, 40, 40);
        upperGreen = new Scalar(82, 240, 240);

        // Définir les limites du jaune.
        lowerYellow = new Scalar(20, 100, 100);
        upperYellow = new Scalar(30, 255, 255);

        // Définir les limites du vert de la balle.
        lowerBallGreen = new Scalar(32, 100, 100);
        upperBallGreen = new Scalar(82, 255, 255);

        lowerBlack = new Scalar(0, 0, 0);
        upperBlack = new Scalar(145, 255, 30);
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
        Imgproc.dilate(src, result, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(maskSize, maskSize)));

        return result;
    }

    public Mat prepareContourDetection(Mat src) {
        // Préparer l'image.
        src = smooth(src, 15);
        src = dilate(src, 5);

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
        result = toGrayscale(src);
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

    public MatOfPoint getCenteredContour(Mat source, List<MatOfPoint> contours) {
        MatOfPoint centeredContour = null;

        int targetPos = (int)(source.width() / 2);
        double smallestDifference = source.width();
        Point avg;

        for (MatOfPoint contour : contours) {
            avg = Detector.getAveragePoint(contour.toArray());
            double difference = Math.abs(targetPos - avg.x);
            if (difference < smallestDifference) {
                centeredContour = contour;
                smallestDifference = difference;
            }
        }

        return centeredContour;
    }

    public Mat filterColor(Mat src, Color color) {
        Mat colorMask = new Mat();

        src = smooth(src, 3);

        // Transformer en HSV.
        Mat hsv = new Mat();
        Imgproc.cvtColor(src, hsv, Imgproc.COLOR_RGB2HSV);

        // Définir les limites de couleurs.
        Scalar lower = null, upper = null;

        if (color == Color.YELLOW) {
            lower = lowerYellow;
            upper = upperYellow;
        }
        else if (color == Color.LINE_GREEN) {
            lower = lowerGreen;
            upper = upperGreen;
        }
        else if (color == Color.BALL_GREEN) {
            lower = lowerBallGreen;
            upper = upperBallGreen;
        }
        else if (color == Color.BLACK) {
            lower = lowerBlack;
            upper = upperBlack;
        }

        Core.inRange(hsv, lower, upper, colorMask);

        return colorMask;
    }

    /**
     *
     * Source: https://www.tabnine.com/code/java/methods/org.opencv.imgproc.Imgproc/matchTemplate
     * @param src
     * @param templateRes
     */
    public Mat matchTemplate(Mat src, int templateRes) {
        // Convertir la ressource en matrice.
        Drawable tSource = ContextCompat.getDrawable(context, templateRes);
        Mat template = bitmapToMap(((BitmapDrawable)tSource).getBitmap());
        template = toGrayscale(template);
        src = toGrayscale(src);

        Mat result = new Mat();
        Imgproc.matchTemplate(src, template, result, Imgproc.TM_CCORR);
        Core.MinMaxLocResult locResult = Core.minMaxLoc(result);
        Imgproc.rectangle(src, locResult.maxLoc, new Point(locResult.maxLoc.x + template.cols(), locResult.maxLoc.y + template.rows()), new Scalar(255, 0, 0, 255));

        Toast.makeText(context, locResult.maxVal + "", Toast.LENGTH_SHORT).show();

        return src;
    }
}