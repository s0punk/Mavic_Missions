package com.vais.mavicmissions.services;

import com.vais.mavicmissions.Enum.Shape;
import com.vais.mavicmissions.MainActivity;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Detector {
    private static final double DEFAULT_EPSILON = 0.04;

    public static Shape detectShape(Mat source, VisionHelper visionHelper, MatOfPoint contour) {
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

        if (contour.rows() < 50)
            return detectedShape;
        else if (sidesCount == 2 || sidesCount == 4)
            detectedShape = Shape.ARROW;
        else if (cornerCount > 7)
            detectedShape = Shape.H;
        else if (sidesCount == 5 || sidesCount == 8 || sidesCount == 9)
            detectedShape = Shape.U;
        else if (sidesCount == 6 || sidesCount == 7)
            detectedShape = Shape.D;
        return detectedShape;
    }

    public static Mat detectArrow(Mat source, Point[] corners) {
        if (corners.length != 3) return null;

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
        Mat cropped;
        try { cropped = new Mat(source, crop); } catch (Exception e) { return null; }

        // Convertir l'image en image binaire.
        Mat binary = new Mat();
        Imgproc.threshold(cropped, binary, 190, 190, Imgproc.THRESH_BINARY_INV);

        return binary;
    }

    public static Point findCenterMass(Mat source) {
        Moments moments = Imgproc.moments(source);
        int cX = (int)(moments.get_m10() / moments.get_m00());
        int cY = (int)(moments.get_m01() / moments.get_m00());
        return new Point(cX, cY);
    }

    public static Point findArrowHead(Point massCenter, Point[] corners) {
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

        return corners.length > cornerID ? corners[cornerID] : null;
    }

    public static double detectAngle(Mat source, Point head) {
        double angle = 0;

        // Trouver l'angle de la flèche.
        int halfWidth = (int)source.width() / 2;
        int halfHeight = (int)source.height() / 2;

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
            double c = Math.sqrt(Math.pow(headA, 2) + Math.pow(headB, 2));;

            // Trouver l'angle à donner au drone.
            switch (quadrant) {
                case 1:
                    // Trouver l'arc sinus de l'angle.
                    angle = Math.toDegrees(Math.asin(headA / c));
                    break;
                case 2:
                    // Trouver l'arc sinus de l'angle.
                    angle = Math.toDegrees(Math.asin(headB / c));
                    angle = 90 + angle;
                    break;
                case 3:
                    angle = Math.toDegrees(Math.asin(headB / c));
                    angle = -90 + (- angle);
                    break;
                case 4:
                    // Trouver l'arc sinus de l'angle.
                    angle = Math.toDegrees(Math.asin(headA / c));
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

    public static Point getAveragePoint(Point[] points) {
        if (points == null || points.length == 0)
            return null;

        int avgX = 0, avgY = 0;
        for (Point p : points) {
            avgX += p.x;
            avgY += p.y;
        }
        avgX = avgX / points.length;
        avgY = avgY / points.length;

        return new Point(avgX, avgY);
    }

    public static Point getCenterPoint(Mat source) {
        return new Point((int)source.width() / 2, (int)source.height() / 2);
    }
}