package com.vais.mavicmissions.services;

import com.vais.mavicmissions.Enum.Shape;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class Detector {
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

    public static double detectDirection(Point[] corners) {
        double angle = 0;
        Point c1 = null;
        Point c2 = null;
        double smallestDistance = 0;

        // Trouver le point central, c1.


        for (int i = 0; i < corners.length; i++) {
            for (int j = 0; j < corners.length; j++) {
                // Trouver la distance entre le point présent et le suivant (d=√((x_2-x_1)²+(y_2-y_1)²)).
                double r1 = Math.pow(corners[i].x - corners[j].x, 2);
                double r2 = Math.pow(corners[i].y - corners[j].y, 2);
                double d = Math.sqrt(r1 + r2);

                if (smallestDistance == 0 || d < smallestDistance && d != 0) {
                    smallestDistance = d;
                    c1 = corners[i];
                    c2 = corners[j];
                }
            }
        }

        if (c1 != null)
            angle = Math.atan2(c2.y - c1.y, c2.x - c1.x);

        return smallestDistance;
    }

    public static int getSidesCount(MatOfPoint contour) {
        MatOfPoint2f c2f = new MatOfPoint2f(contour.toArray());
        double perimeter = Imgproc.arcLength(c2f, true);
        MatOfPoint2f approx = new MatOfPoint2f();
        Imgproc.approxPolyDP(c2f, approx, DEFAULT_EPSILON * perimeter, true);

        return approx.toArray().length;
    }
}