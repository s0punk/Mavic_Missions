package com.vais.mavicmissions.services;

import com.vais.mavicmissions.Enum.Shape;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
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

    public static String detectDirection(Point[] corners) {
        Point c1;
        Point c2;
        Point c3;

        //StringBuilder result = new StringBuilder();
        /*for (int i = 0; i < corners.length; i++) {
            // Si il y a un autre coins après celui-ci.
            if (corners.length < i + 1) {
                // Trouver la distance entre le point présent et le suivant (d=√((x_2-x_1)²+(y_2-y_1)²)).
                double r1 = Math.pow(corners[i + 1].x - corners[i].x, 2);
                double r2 = Math.pow(corners[i + 1].y - corners[i].y, 2);
                double distance = Math.sqrt(r1 + r2);
                result.append(distance).append(", ");
            }
        }*/



        return "";
    }

    public static int getSidesCount(MatOfPoint contour) {
        MatOfPoint2f c2f = new MatOfPoint2f(contour.toArray());
        double perimeter = Imgproc.arcLength(c2f, true);
        MatOfPoint2f approx = new MatOfPoint2f();
        Imgproc.approxPolyDP(c2f, approx, DEFAULT_EPSILON * perimeter, true);

        return approx.toArray().length;
    }
}