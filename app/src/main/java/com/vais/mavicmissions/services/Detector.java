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
    private static double DEFAULT_EPSILON = 0.04;

    private static class Edge {
        Point start;
        Point end;
        double length;

        public Edge(Point start, Point end) {
            this.start = start;
            this.end = end;
            this.length = calculateLength();
        }

        public double calculateLength() {
            if (start == null || end == null ) return 0;

            double r1 = Math.pow(start.x - end.x, 2);
            double r2 = Math.pow(start.y - end.y, 2);
            return Math.sqrt(r1 + r2);
        }

        public static List<Edge> sort(List<Edge> edges) {
            for (int i = 0; i < edges.size(); i++)
                if (edges.size() > i + 1 && edges.get(i).getLength() > edges.get(i + 1).getLength()) {
                    Edge tEdge = edges.get(i);
                    edges.set(i, edges.get(i + 1));
                    edges.set(i + 1, tEdge);
                }

            return edges;
        }

        public static Point findMeetingPoint(Edge first, Edge second) {
            Point meetingPoint = null;

            if (first == null || second == null) return null;

            if ((first.start.x == second.start.x && first.start.y == second.start.y) || (first.start.x == second.end.x && first.start.y == second.end.y))
                return first.start;
            else if ((first.end.x == second.start.x && first.end.y == second.start.y) || (first.end.x == second.end.x && first.end.y == second.end.y))
                return first.end;

            return meetingPoint;
        }

        public Point getStart() { return start; }

        public Point getEnd() { return end; }

        public double getLength() { return length; }
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

    public static double detectDirection(MatOfPoint contour) {
        MatOfPoint2f c2f = new MatOfPoint2f(contour.toArray());
        double perimeter = Imgproc.arcLength(c2f, true);
        MatOfPoint2f approx = new MatOfPoint2f();
        Imgproc.approxPolyDP(c2f, approx, DEFAULT_EPSILON * perimeter, true);

        Point[] corners = approx.toArray();
        List<Edge> edges = new ArrayList<Edge>();

        Edge first;
        Edge second;
        Point meetingPoint;

        // Établir tous les côtés de la flèche.
        for (Point start : corners)
            for (Point end : corners)
                if (start.x != end.x && start.y != end.y)
                    edges.add(new Edge(start, end));

        // Trouver les deux côtés les plus long.
        edges = Edge.sort(edges);
        first = edges.get(edges.size() - 1);
        second = edges.get(edges.size() - 2);

        // Trouver le point où les deux côtés se retrouvent.
        meetingPoint = Edge.findMeetingPoint(first, second);

        // Trouver l'angle du point par rapport au centre de la photo.
        return meetingPoint != null ? Math.atan2(meetingPoint.y, meetingPoint.x) : 0;
    }

    public static int getSidesCount(MatOfPoint contour) {
        MatOfPoint2f c2f = new MatOfPoint2f(contour.toArray());
        double perimeter = Imgproc.arcLength(c2f, true);
        MatOfPoint2f approx = new MatOfPoint2f();
        Imgproc.approxPolyDP(c2f, approx, DEFAULT_EPSILON * perimeter, true);

        return approx.toArray().length;
    }
}