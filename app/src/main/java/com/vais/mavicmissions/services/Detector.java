package com.vais.mavicmissions.services;

import com.vais.mavicmissions.Enum.Shape;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Detector {
    private static final double DEFAULT_EPSILON = 0.04;

    private static class Edge {
        private final Point start;
        private final Point end;
        private final long length;

        public Edge(Point start, Point end) {
            this.start = start;
            this.end = end;
            this.length = getLength(start, end);
        }

        public static long getLength(Point p1, Point p2) {
            if (p1 == null || p2 == null ) return 0;

            double r1 = Math.pow(p1.x - p2.x, 2);
            double r2 = Math.pow(p1.y - p2.y, 2);
            return Math.round(Math.sqrt(r1 + r2));
        }

        public static void sort(List<Edge> edges) {
            for (int i = 0; i < edges.size(); i++)
                if (edges.size() > i + 1 && edges.get(i).length > edges.get(i + 1).length) {
                    Edge tEdge = edges.get(i);
                    edges.set(i, edges.get(i + 1));
                    edges.set(i + 1, tEdge);
                }
        }

        public static Point findMeetingPoint(Edge first, Edge second) {
            final int areaLimit = 50;

            if (first == null || second == null) return null;
            Point[] allPoints = { first.start, second.start, first.end, second.end };

            for (int i = 0; i < allPoints.length; i++)
                for (int j = 0; j < allPoints.length; j++)
                    if (i != j)
                        if (allPoints[j].x >= allPoints[i].x - areaLimit && allPoints[j].x <= allPoints[i].x + areaLimit && allPoints[j].y >= allPoints[i].y - areaLimit && allPoints[j].y <= allPoints[i].y + areaLimit)
                            return allPoints[i];

            return null;
        }

        public static boolean alreadyExist(List<Edge> edges, Point target) {
            for (Edge e : edges)
                if (comparePoints(e.start, target) || comparePoints(e.end, target))
                    return true;

            return false;
        }

        public static boolean comparePoints(Point p1, Point p2) {
            return p1.x == p2.x && p1.y == p2.y;
        }
    }

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

    public static Point[] detectArrowDirection(Point[] corners) {
        List<Edge> edges = new ArrayList<>();

        if (corners.length != 4) return null;

        Edge first;
        Edge second;
        Point meetingPoint = null;

        // Établir tous les côtés de la flèche.
        for (int i = 0; i < corners.length; i++) {

            Point[] copy = corners.clone();
            int nextPoint = getNextPoint(copy, corners[i]);
            if (i != corners.length -1) {
                // Établir un lien entre les deux points.
                while(Edge.alreadyExist(edges, corners[nextPoint])) {
                    Point[] newCopy = new Point[copy.length - 1];
                    int k = 0;
                    // Enlever l'item de la liste.
                    for (int j = 0; j < copy.length; j++)
                        if (j != nextPoint) {
                            newCopy[k] = copy[j];
                            k++;
                        }

                    // Trouver un autre point.
                    nextPoint = getNextPoint(copy, corners[i]);
                }
                edges.add(new Edge(corners[i], corners[nextPoint]));
            }
            else
                // Fermer la forme.
                edges.add(new Edge(corners[i], edges.get(0).start));
        }

        // Trouver les deux côtés les plus long.
        Edge.sort(edges);
        first = edges.get(edges.size() - 1);
        second = edges.get(edges.size() - 2);

        meetingPoint = Edge.findMeetingPoint(first, second);

        return new Point[]{first.start, first.end, second.start, second.end, meetingPoint};
    }

    public static int getNextPoint(Point[] corners, Point startCorner) {
        // Trouver le coin le plus proche.
        long length = Long.MAX_VALUE;
        int nextPoint = 0;
        for (int j = 0; j < corners.length; j++) {
            long newLength = Edge.getLength(startCorner, corners[j]);

            if (newLength < length && newLength != 0) {
                length = newLength;
                nextPoint = j;
            }
        }

        return nextPoint;
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