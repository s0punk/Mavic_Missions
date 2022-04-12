package com.vais.mavicmissions.services;

import android.content.Context;
import android.widget.Toast;

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
        MatOfPoint corners = visionHelper.detectCorners(source, 10, 0.4f, 10);

        int cornerCount = corners.toArray().length;
        int sidesCount = approx.toArray().length;

        if (cornerCount > 7)
            detectedShape = Shape.H;
        else if (sidesCount == 2 || sidesCount == 4)
            detectedShape = Shape.ARROW;
        else {
            // Détecter deux coins seulement.
            corners = visionHelper.detectCorners(source, 2, 0.01f, 110);
            Point[] points = corners.toArray();

            if (points.length == 2) {
                double distance = getLength(points[0], points[1]);
                if (distance < 250)
                    detectedShape = Shape.U;
                else
                    detectedShape = Shape.D;
            }
        }

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

        Rect crop = new Rect(x.get(0) - 25, y.get(0) - 25, newWidth, newHeight);
        Mat cropped = new Mat(source, crop);

        // Convertir l'image en image binaire.
        Mat binary = new Mat();
        Imgproc.threshold(cropped, binary, 70, 70, Imgproc.THRESH_BINARY_INV);

        // Trouver le centre de masse du noir dans l'image.
        Moments moments = Imgproc.moments(binary);
        int cX = (int)(moments.get_m10() / moments.get_m00());
        int cY = (int)(moments.get_m01() / moments.get_m00());
        Point massCenter = new Point(cX, cY);

        // Refaire la détection des coins puisque la taille de l'image est différente.
        MatOfPoint newCorners = visionHelper.detectCorners(binary, 3, 30);
        corners = newCorners.toArray();

        // Calculer la distance entre chaque point et le centre de masse.
        double smallestDistance = Double.MAX_VALUE;
        int cornerID = 0;
        for (int i = 0; i < corners.length; i++) {
            double currentDistance = getLength(corners[i], massCenter);

            if (currentDistance < smallestDistance) {
                smallestDistance = currentDistance;
                cornerID = i;
            }
        }

        Point head = corners[cornerID];

        // Trouver l'angle de la flèche.
        int halfWidth = (int)binary.width() / 2;
        int halfHeight = (int)binary.height() / 2;
        double angle = 0;

        // Déterminer le quandrant de la pointe.
        int quadrant = 0;
        if (head.x > halfWidth && head.y < halfHeight)
            quadrant = 1;
        else if (head.x > halfWidth && head.y > halfHeight)
            quadrant = 2;
        else if (head.x < halfWidth && head.y > halfHeight)
            quadrant = 3;
        else if (head.x < halfWidth && head.y < halfHeight)
            quadrant = 4;

        if (quadrant == 0) {
            if (head.x == halfWidth)
                angle = head.y > halfHeight ? -180 : 0;
            else if (head.y == halfHeight)
                angle = head.x > halfWidth ? 90 : -90;
        }
        else {
            // Trouver l'hypoténuse avec le théorem de pythagore.
            double headA = Math.abs(head.x - halfWidth);
            double headB = Math.abs(head.y - halfHeight);
            double c = Math.sqrt(Math.pow(headA, 2) + Math.pow(headB, 2));

            // Trouver l'arc sinus de l'angle.
            angle = Math.toDegrees(Math.asin(headB / c));

            // Trouver l'angle à donner au drone.
            switch (quadrant) {
                case 1:
                    angle = 0 + angle;
                    break;
                case 2:
                    angle = 90 + angle;
                    break;
                case 3:
                    angle = -90 - angle;
                    break;
                case 4:
                    angle = - angle;
                    break;
            }
        }

        return angle;
    }

    public static double getLength(Point p1, Point p2) {
        if (p1 == null || p2 == null ) return 0;

        double r1 = Math.pow(p1.x - p2.x, 2);
        double r2 = Math.pow(p1.y - p2.y, 2);
        return Math.round(Math.sqrt(r1 + r2));
    }
}