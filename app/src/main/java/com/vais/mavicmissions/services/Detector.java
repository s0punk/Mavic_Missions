package com.vais.mavicmissions.services;

import com.vais.mavicmissions.Enum.Shape;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dji.internal.util.ArrayUtil;

public class Detector {
    private static final double DEFAULT_EPSILON = 0.04;

    public static Shape detect(Mat source, VisionHelper visionHelper, MatOfPoint contour) {
        Shape detectedShape = Shape.UNKNOWN;

        // Détecter les côtés du contour.
        MatOfPoint2f c2f = new MatOfPoint2f(contour.toArray());
        double perimeter = Imgproc.arcLength(c2f, true);
        MatOfPoint2f approx = new MatOfPoint2f();
        Imgproc.approxPolyDP(c2f, approx, DEFAULT_EPSILON * perimeter, true);

        // Détecter les coins.
        MatOfPoint corners = visionHelper.detectCorners(source, 10, 0.5f, 10);

        Point[] points = corners.toArray();
        double slope = -1;
        if (points.length == 2)
            slope = (points[1].y - points[0].y) / (points[1].x - points[0].x);

        int cornerCount = corners.toArray().length;
        int sidesCount = approx.toArray().length;

        if (cornerCount > 6)
            detectedShape = Shape.H;
        else if (sidesCount == 2 || sidesCount == 4)
            detectedShape = Shape.ARROW;
        else if (slope >= -1.5 && slope <= 1.5)
            detectedShape = Shape.U;
        else
            detectedShape = Shape.D;

        return detectedShape;
    }

    public static double detectArrowDirection(Mat source, VisionHelper visionHelper, Point[] corners) {
        if (corners.length != 3) return 0;

        // Redimensionner l'image pour y avoir la flèche seulement.
        List<Integer> x = new ArrayList<>();
        List<Integer> y = new ArrayList<>();

        for (Point corner : corners) {
            x.add((int) corner.x);
            y.add((int) corner.y);
        }
        Collections.sort(x);
        Collections.sort(y);

        int newWidth = (x.get(x.size() - 1) - x.get(0)) + 50;
        int newHeight = (y.get(y.size() - 1) - y.get(0)) + 50;

        newWidth = newWidth > source.width() ? newWidth - 50 : newWidth;
        newHeight = newHeight > source.height() ? newHeight - 50 : newHeight;

        Rect crop = new Rect(x.get(0) - 25, y.get(0) - 25, newWidth, newHeight);
        Mat cropped = new Mat(source, crop);

        // Convertir l'image en image binaire.
        Mat binary = new Mat();
        Imgproc.threshold(cropped, binary, 115, 115, Imgproc.THRESH_BINARY_INV);

        // Trouver le centre de masse du noir dans l'image.
        Moments moments = Imgproc.moments(binary);
        int cX = (int)(moments.get_m10() / moments.get_m00());
        int cY = (int)(moments.get_m01() / moments.get_m00());
        Point massCenter = new Point(cX, cY);

        // Refaire la détection des coins puisque la taille de l'image est différente.
        MatOfPoint newCorners = visionHelper.detectCorners(binary, 3, 30);
        corners = newCorners.toArray();

        // Calculer la distance entre chaque point et le centre de masse.
        List<Point> otherCorners = new ArrayList<>();
        double smallestDistance = Double.MAX_VALUE;
        int cornerID = 0;
        for (int i = 0; i < corners.length; i++) {
            double currentDistance = getLength(corners[i], massCenter);

            if (currentDistance < smallestDistance) {
                smallestDistance = currentDistance;
                cornerID = i;
            }
        }

        // Trouver l'angle de la flèche.
        double angle = 0;
        for (Point p : corners)
            if (p != corners[cornerID])
                otherCorners.add(p);
        double delta1 = corners[cornerID].y - otherCorners.get(0).y / corners[cornerID].x - otherCorners.get(0).x;
        double delta2 = corners[cornerID].y - otherCorners.get(1).y / corners[cornerID].x - otherCorners.get(1).x;
        double direction = delta1 + delta2;

        return angle;
    }

    public static double getLength(Point p1, Point p2) {
        if (p1 == null || p2 == null ) return 0;

        double r1 = Math.pow(p1.x - p2.x, 2);
        double r2 = Math.pow(p1.y - p2.y, 2);
        return Math.round(Math.sqrt(r1 + r2));
    }

    public static Point[] getSides(MatOfPoint contour) {
        MatOfPoint2f c2f = new MatOfPoint2f(contour.toArray());
        double perimeter = Imgproc.arcLength(c2f, true);
        MatOfPoint2f approx = new MatOfPoint2f();
        Imgproc.approxPolyDP(c2f, approx, DEFAULT_EPSILON * perimeter, true);

        return approx.toArray();
    }

    public static int getSidesCount(MatOfPoint contour) {
        MatOfPoint2f c2f = new MatOfPoint2f(contour.toArray());
        double perimeter = Imgproc.arcLength(c2f, true);
        MatOfPoint2f approx = new MatOfPoint2f();
        Imgproc.approxPolyDP(c2f, approx, DEFAULT_EPSILON * perimeter, true);

        return approx.toArray().length;
    }
}