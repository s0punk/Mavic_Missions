package com.vais.mavicmissions.objectives;

import android.os.Handler;
import android.os.Looper;

import com.vais.mavicmissions.Enum.Color;
import com.vais.mavicmissions.MainActivity;
import com.vais.mavicmissions.R;
import com.vais.mavicmissions.services.Detector;
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

/**
 * Classe qui gère l'accomplissement de l'objectif 3, le sauvetage d'une balle.
 */
public class BallRescue extends Objectif {
    private final int MAX_FAILED_ATTEMPT = 3;
    private final int BALL_DETECTION_THRESHOLD = 150;
    private final int CHANGE_ZONE_ROTATION = 25;

    private int failedAttempt;
    private int totalRotation;

    /**
     * Constructeur de la classe FollowLine, créé l'objet et initialise ses données membres.
     * @param caller MainActivity, instance de l'activité principale, permet d'accéder à différents éléments du UI.
     * @param controller AircraftController, controlleur du drone.
     * @param cameraController CameraController, controlleur de la caméra du drone.
     * @param visionHelper VisionHelper, service de traitement d'images.
     */
    public BallRescue(MainActivity caller, AircraftController controller, CameraController cameraController, VisionHelper visionHelper) {
        super(caller, controller, cameraController, visionHelper);
    }

    /**
     * Méthode qui commence le processus de sauvetage de la balle.
     */
    public void startBallRescue() {
        // Désactiver les boutons, excepté le bouton d'arrêt.
        setStopButton(caller.btnBallRescue);
        caller.showToast(caller.getResources().getString(R.string.ballRescueStart));

        // Commencer l'objectif.
        startObjectif(djiError -> {
            // Commencer la recherche de la balle.
            failedAttempt = 0;
            totalRotation = 0;
            cameraController.lookAtAngle(-55);
            cameraController.setZoom(CameraController.ZOOM_1_6X, djiError1 -> {
                controller.goUp(2000, this::search);
            });
        });
    }

    /**
     * Méthode qui permet de chercher la balle.
     */
    private void search() {
        // Quitter si l'objectif n'est pas démarré.
        if (!objectifStarted)
            return;

        // Capturer le flux vidéo.
        Mat matSource = getFrame();

        Point[] points = detectBall(matSource);
        Point ball = getBall(points);

        if (ball != null) {
            // Afficher la position de la balle.
            Imgproc.circle(matSource, ball, 2, new Scalar(255, 255, 0, 255), 10);
            showFrame(matSource);

            rescue();
        }
        else {
            if (++failedAttempt > MAX_FAILED_ATTEMPT) {
                failedAttempt = 0;
                totalRotation += CHANGE_ZONE_ROTATION;

                if (totalRotation < 360)
                    // Rotationner le drone pour chercher une nouvelle zone.
                    controller.faceAngle(CHANGE_ZONE_ROTATION, this::search);
                else if (!cameraController.isLookingDown()) {
                    // Regarder directement en dessous du drone.
                    cameraController.lookDown();
                    totalRotation = 0;
                    search();
                }
                else
                    new Handler(Looper.getMainLooper()).post(() -> caller.onClick(caller.btnBallRescue));
            }
            else
                new Handler().postDelayed(this::search, 500);
        }
    }

    private Point getBall(Point[] points) {
        return points.length > BALL_DETECTION_THRESHOLD ? Detector.getAveragePoint(points) : null;
    }

    private Point[] detectBall(Mat source) {
        // Filter les couleurs de la balle.
        Mat yellow = visionHelper.filterColor(source, Color.YELLOW);
        Mat green = visionHelper.filterColor(source, Color.BALL_GREEN);

        // Combiner les filtres de couleurs.
        Mat combination = new Mat();
        Core.add(yellow, green, combination);

        // Trouver le plus gros contour.
        List<MatOfPoint> contours = visionHelper.contoursDetection(combination);
        MatOfPoint biggerContour = visionHelper.getBiggerContour(contours);

        Point[] detectedPoints = biggerContour.toArray();
        detectedPoints = detectedPoints.length == 4 ? new Point[] {} : detectedPoints;

        return detectedPoints;
    }

    private void rescue() {
        Mat matSource = getFrame();

        Point ball = getBall(detectBall(matSource));
        int centerX = (int)matSource.width() / 2;
        int centerY = (int)matSource.height() / 2;

        // Si le drone voit la balle.
        if (ball != null) {
            // Si la balle est centrée.
            if (ball.x > centerX - 55 && ball.x < centerX + 55) {
                if (cameraController.isLookingDown()) {
                    if (ball.y < centerY - 55 && ball.y < centerY + 55) {
                        // Attérir le drone.
                        controller.faceAngle(AircraftController.ROTATION_LEFT, () -> {
                            controller.goBack(500, () -> {
                                cameraController.lookAtAngle(0);
                                controller.land(null);
                            });
                        });
                    }
                    else if (ball.y < centerY)
                        controller.goBack(250, this::rescue);
                    else if (ball.y > centerY)
                        controller.goForward(250, this::rescue);
                }
                else
                    controller.goForward(1000, this::rescue);
            }
            else {
                // Centrer la balle sur l'axe des x.
                if (ball.x > centerX)
                    controller.goLeft(250, this::rescue);
                else if (ball.x < centerX)
                    controller.goRight(250, this::rescue);
            }
        }
        // Si le drone ne voit plus la balle.
        else {
            if (cameraController.isLookingDown()) {
                // Reculer le drone car il a dépasser la balle.
                controller.goBack(500, this::rescue);
            }
            else {
                cameraController.lookDown();
                rescue();
            }
        }
    }
}