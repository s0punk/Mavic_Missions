package com.vais.mavicmissions.services;

import com.vais.mavicmissions.Enum.Shape;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.imgproc.Imgproc;

public class ShapeDetector {
    private static double DEFAULT_EPSILON = 0.04;

    public static Shape detect(MatOfPoint contour) {
        Shape detectedShape = Shape.UNKNOWN;

        MatOfPoint2f c2f = new MatOfPoint2f(contour.toArray());
        double perimeter = Imgproc.arcLength(c2f, true);
        MatOfPoint2f approx = new MatOfPoint2f();
        Imgproc.approxPolyDP(c2f, approx, DEFAULT_EPSILON * perimeter, true);

        switch (approx.toArray().length) {
            case 2:
            case 4:
                detectedShape = Shape.ARROW;
                break;
            case 5:
                detectedShape = Shape.U;
                break;
            case 6:
                detectedShape = Shape.D;
                break;
            case 8:
                detectedShape = Shape.H;
        }

        return detectedShape;
    }

    public static int getSidesCount(MatOfPoint contour) {
        MatOfPoint2f c2f = new MatOfPoint2f(contour.toArray());
        double perimeter = Imgproc.arcLength(c2f, true);
        MatOfPoint2f approx = new MatOfPoint2f();
        Imgproc.approxPolyDP(c2f, approx, DEFAULT_EPSILON * perimeter, true);

        return approx.toArray().length;
    }
}