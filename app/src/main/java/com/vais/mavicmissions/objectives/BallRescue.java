package com.vais.mavicmissions.objectives;

import android.os.Handler;

import com.vais.mavicmissions.Enum.Color;
import com.vais.mavicmissions.MainActivity;
import com.vais.mavicmissions.R;
import com.vais.mavicmissions.services.drone.AircraftController;
import com.vais.mavicmissions.services.drone.CameraController;
import com.vais.mavicmissions.services.VisionHelper;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.List;

public class BallRescue extends Objectif {
    public BallRescue(MainActivity caller, AircraftController controller, CameraController cameraController, VisionHelper visionHelper) {
        super(caller, controller, cameraController, visionHelper);
    }

    public void startBallRescue() {
        setStopButton(caller.btnBallRescue);

        caller.showToast(caller.getResources().getString(R.string.ballRescueStart));

        startObjectif(djiError -> {
            // Commencer la recherche de la balle.
            cameraController.lookAtAngle(-35);
            search();
        });
    }

    private void search() {
        if (!objectifStarted)
            return;

        // Capturer le flux vidéo.
        Mat matSource = getFrame();

        Mat yellow = visionHelper.filterColor(matSource, Color.YELLOW);
        Mat green = visionHelper.filterColor(matSource, Color.BALL_GREEN);

        // Combiner le jaune et le vert.
        Mat combination = new Mat();
        Core.add(yellow, green, combination);

        // trouver un cercle.
        List<MatOfPoint> contours = visionHelper.contoursDetection(combination);
        MatOfPoint biggerContour = visionHelper.getBiggerContour(contours);
        Point[] points = biggerContour.toArray();

        // Faire la moyenne du contour.
        Point avg = null;
        int avgX = 0, avgY = 0;
        for (Point p : points) {
            avgX += p.x;
            avgY += p.y;
        }
        avgX = avgX / points.length;
        avgY = avgY / points.length;
        avg = new Point(avgX, avgY);

        Imgproc.circle(matSource, avg, 2, new Scalar(255, 255, 0, 255), 10);
        showFrame(matSource);

        new Handler().postDelayed(this::search, 500);
    }

    private void rescue() {

    }
}