package com.vais.mavicmissions.services;

import com.vais.mavicmissions.Enum.Shape;
import com.vais.mavicmissions.R;
import com.vais.mavicmissions.objectives.Objectif;
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
import java.util.List;

/**
 * Simon-Olivier Vaillancourt
 * 2022-05-20
 * DJI Mavic 2 Entreprise
 * Classe qui gère la détection d'élément.
 */
public class Detector {
    /**
     * Double, epsilon par défaut lors de la détection d'arrêtes d'une forme.
     */
    private static final double DEFAULT_EPSILON = 0.04;

    /**
     * Int, limite maximum pour la comparaison de contours.
     */
    private static final float MATCH_SHAPE_TRESH = 1.3f;

    /**
     * Fonction qui permet de détecter une forme à la caméra.
     * @param source Mat, matrice à analyzer.
     * @param visionHelper VisionHelper, service de traitement d'image.
     * @param contour MatOfPoint, contour détecté.
     * @param m Objectif, gestionnaire de l'objectif.
     * @return Shape, forme détectée.
     */
    public static Shape detectShape(Mat source, VisionHelper visionHelper, MatOfPoint contour, Objectif m) {
        Shape detectedShape = Shape.UNKNOWN;

        // Détecter les côtés du contour.
        MatOfPoint2f c2f = new MatOfPoint2f(contour.toArray());
        double perimeter = Imgproc.arcLength(c2f, true);
        MatOfPoint2f approx = new MatOfPoint2f();
        Imgproc.approxPolyDP(c2f, approx, DEFAULT_EPSILON * perimeter, true);

        int sidesCount = approx.toArray().length;

        double[] similarities = new double[3];
        similarities[0] = visionHelper.matchShape(contour, R.mipmap.ic_d_foreground);
        similarities[1] = visionHelper.matchShape(contour, R.mipmap.ic_u_foreground);
        similarities[2] = visionHelper.matchShape(contour, R.mipmap.ic_h_foreground);

        boolean isCircle = detectCircle(contour);

        // Déterminer la forme selon les paramètres obtenus.
        if (similarities[1] < MATCH_SHAPE_TRESH)
            detectedShape = Shape.U;
        else if (similarities[0] < MATCH_SHAPE_TRESH)
            detectedShape = Shape.D;
        else if (similarities[2] < MATCH_SHAPE_TRESH && isCircle)
            detectedShape = Shape.H;
        else if (sidesCount <= 8 && sidesCount >= 2)
            detectedShape = Shape.ARROW;

        if (detectedShape != Shape.ARROW)
            m.showFrame(visionHelper.drawContour(source.clone(), contour));

        return detectedShape;
    }

    /**
     * Fonction qui permet de détecter une flèche.
     * @param source Mat, matrice à analyzer.
     * @param corners Point[], coins de la flèche.
     * @param visionHelper VisionHelper, service de traitement d'image.
     * @return Mat, matrice filtré.
     */
    public static Mat detectArrow(Mat source, Point[] corners, VisionHelper visionHelper) {
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
        try { cropped = new Mat(source, crop); }
        catch (Exception e) {
            cropped = new Mat(source, new Rect(x.get(0), y.get(0), (x.get(x.size() - 1) - x.get(0)), (y.get(y.size() - 1) - y.get(0))));
        }

        return visionHelper.toGrayscale(cropped);
    }

    /**
     * Fonction qui permet d'obtenir le centre de masse d'une matrice.
     * @param source Mat, matrice à analyzer.
     * @return Point, centre de masse.
     */
    public static Point findCenterMass(Mat source) {
        Moments moments = Imgproc.moments(source);
        int cX = (int)(moments.get_m10() / moments.get_m00());
        int cY = (int)(moments.get_m01() / moments.get_m00());
        return new Point(cX, cY);
    }

    /**
     * Fonction qui permet d'obtenir la pointe d'une flèche.
     * @param massCenter Point, centre de masse de la matrice.
     * @param corners Point[], coins de la flèche.
     * @return Point, point de la pointe de la flèche.
     */
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

    /**
     * Fonction qui permet de trouver l'angle entre deux points.
     * @param base Point, premier point.
     * @param head Point, deuxième point.
     * @return Double, angle entre les deux points.
     */
    public static double detectAngle(Point base, Point head) {
        double angle = 0;

        // Déterminer le quandrant de la pointe.
        int quadrant = 0;
        if (head.x > base.x && head.y < base.y)
            quadrant = 1;
        else if (head.x > base.x && head.y > base.y)
            quadrant = 2;
        else if (head.x < base.x && head.y > base.y)
            quadrant = 3;
        else if (head.x < base.x && head.y < base.y)
            quadrant = 4;

        if (quadrant == 0) {
            if (head.x == base.x)
                angle = head.y > base.y ? -180 : 0;
            else if (head.y == base.y)
                angle = head.x > base.y ? 90 : -90;
        }
        else {
            // Trouver l'hypoténuse avec le théorem de pythagore.
            double headA = Math.abs(head.x - base.x);
            double headB = Math.abs(head.y - base.y);
            double c = Math.sqrt(Math.pow(headA, 2) + Math.pow(headB, 2));

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

    /**
     * Fonction qui trouve la distance entre deux points.
     * @param p1 Point, premier point.
     * @param p2 Point, deuxième point.
     * @return Doubble, distance entre les deux points.
     */
    public static double getLength(Point p1, Point p2) {
        if (p1 == null || p2 == null ) return 0;

        // Calculer la distance.
        double r1 = Math.pow(p1.x - p2.x, 2);
        double r2 = Math.pow(p1.y - p2.y, 2);
        return Math.round(Math.sqrt(r1 + r2));
    }

    /**
     * Fonction qui trouve le point moyen d'une liste de points.
     * @param points Point[], tableau de points.
     * @return Point, point moyen de la liste.
     */
    public static Point getAveragePoint(Point[] points) {
        if (points == null || points.length == 0)
            return null;

        // Calculer la moyenne des x et y.
        int avgX = 0, avgY = 0;
        for (Point p : points) {
            avgX += p.x;
            avgY += p.y;
        }

        // Diviser selon le nombre de points.
        avgX = avgX / points.length;
        avgY = avgY / points.length;

        return new Point(avgX, avgY);
    }

    /**
     * Fonction qui détermine si deux points sont alignés.
     * @param base Point, premier point.
     * @param head Point, deuxième point.
     * @return Point[], tableau contenant les nouveaux points alignés. (1, 2)
     */
    public static Point[] detectPointAlignement(Point base, Point head) {
        int difference = (int)(base.x - head.x);

        if (difference > 5)
            base.x -= 10;
        else if (difference < 5)
            base.x += 10;

        return new Point[] { base, head };
    }

    /**
     * Fonction qui permet d'obtenir le centre de la matrice.
     * @param source Mat, matrice à analyzer.
     * @return Point, centre de la matrice.
     */
    public static Point getCenterPoint(Mat source) {
        return new Point((int)(source.width() / 2), (int)(source.height() / 2));
    }

    public static boolean detectCircle(MatOfPoint contour) {
        double radius = -1;
        double actualRadius;
        Point center = getAveragePoint(contour.toArray());

        for (Point p : contour.toArray()) {
            actualRadius = getLength(center, p);

            if (radius == -1)
                radius = actualRadius;
            else if (actualRadius < radius - 10 || actualRadius > radius + 10)
                return false;
        }

        return true;
    }
}