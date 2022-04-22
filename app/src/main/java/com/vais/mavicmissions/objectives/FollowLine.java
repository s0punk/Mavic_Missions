package com.vais.mavicmissions.objectives;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import com.vais.mavicmissions.MainActivity;
import com.vais.mavicmissions.R;
import com.vais.mavicmissions.services.AircraftController;
import com.vais.mavicmissions.services.CameraController;
import com.vais.mavicmissions.services.Detector;
import com.vais.mavicmissions.services.VisionHelper;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class FollowLine {
    private MainActivity caller;
    private AircraftController controller;
    private CameraController cameraController;
    private VisionHelper visionHelper;

    private boolean followLineStarted;

    public FollowLine(MainActivity caller, AircraftController controller, CameraController cameraController, VisionHelper visionHelper) {
        this.caller = caller;
        this.controller = controller;
        this.cameraController = cameraController;
        this.visionHelper = visionHelper;

        followLineStarted = false;
    }

    public void startFollowLine() {
        // Configurer le bouton d'arrêt du suivi.
        caller.setUIState(false, caller.btnFollowLine);
        caller.btnFollowLine.setText(caller.getResources().getString(R.string.stop));
        followLineStarted = true;

        controller.setCurrentSpeed(AircraftController.MAXIMUM_AIRCRAFT_SPEED);

        caller.showToast(caller.getResources().getString(R.string.followLineStart));

        // Vérifier l'état du drone.
        cameraController.lookForward();
        seekGreenLine();
        controller.checkVirtualStick(() -> {
            if (controller.getHasTakenOff()) {
                cameraController.setZoom(CameraController.ZOOM_1X, djiError -> {
                    // Commencer le suivi de la ligne.
                    controller.goForward(2000, null);
                    seekGreenLine();
                });
            }
            else {
                // Décoller le drone.
                controller.takeOff(() -> {
                    cameraController.setZoom(CameraController.ZOOM_1X, djiError -> {
                        // Commencer le suivi de la ligne.
                        controller.goForward(2000, null);
                        seekGreenLine();
                    });
                });
            }
        });
    }

    public void seekGreenLine() {
        if (!followLineStarted)
            return;

        // Capturer le flux vidéo.
        Bitmap source = caller.cameraSurface.getBitmap();
        Mat matSource = visionHelper.bitmapToMap(source);

        // Isoler le vert.
        Mat green = visionHelper.filterGreen(matSource);

        // Détecter les coins.
        Point center = new Point((int)green.width() / 2, (int)green.height() / 2);
        MatOfPoint corners = visionHelper.detectCorners(green, 25, 0.5f, 15);
        Point[] points = corners.toArray();

        int generalDirection = AircraftController.ROTATION_FRONT;
        int up = 0, right = 0, left = 0;

        for (Point p : points) {
            // Déterminer la direction du point.
            if (p.y < center.y && p.x > center.x - 75 && p.x < center.x + 75)
                up++;
            else if (p.x > center.x && p.y > 50)
                right++;
            else if (p.x < center.x && p.y > 50)
                left++;

            // Afficher le point.
            Imgproc.circle(matSource, p, 2, new Scalar(255, 0, 0, 255), 10);
        }
        Imgproc.circle(matSource, center, 2, new Scalar(0, 255, 0, 255), 10);

        // Déterminer la direction générale.
        if (right > left && right > up)
            generalDirection = AircraftController.ROTATION_RIGHT;
        else if (left > right && left > up)
            generalDirection = AircraftController.ROTATION_LEFT;

        // Afficher le résultat.
        new Handler(Looper.getMainLooper()).post(() -> caller.ivResult.setImageBitmap(visionHelper.matToBitmap(matSource)));
        caller.showToast(generalDirection + "");

        // Effectuer l'action requise.
        changeDirection(generalDirection);
    }

    private void changeDirection(int direction) {
        switch (direction) {
            case AircraftController.ROTATION_FRONT:
                controller.goForward(2000, null);
                new Handler().postDelayed(this::seekGreenLine, 500);
                break;
            case AircraftController.ROTATION_RIGHT:
            case AircraftController.ROTATION_LEFT:
                new Handler().postDelayed(() -> {
                    controller.stop(() -> {
                        controller.faceAngle(direction, true, () -> {
                            controller.goForward(2000, null);
                            new Handler().postDelayed(this::seekGreenLine, 500);
                        });
                    });
                }, 500);
                break;
        }
    }

    public boolean isFollowLineStarted() { return followLineStarted; }
    public void setFollowLineStarted(boolean followLineStarted) { this.followLineStarted = followLineStarted; }
}