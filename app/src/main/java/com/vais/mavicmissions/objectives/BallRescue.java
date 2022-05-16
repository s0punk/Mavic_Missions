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
    /**
     * Int, nombre maximum d'essaie pour la détection de la balle.
     */
    private final int MAX_FAILED_ATTEMPT = 3;
    /**
     * Int, threshold utilisée pour la détection de la balle.
     */
    private final int BALL_DETECTION_THRESHOLD = 150;
    /**
     * Int, rotation à effectuer lorsque le drone doit ballayer une nouvelle zone.
     */
    private final int CHANGE_ZONE_ROTATION = 25;

    /**
     * String, message affiché lorsque le sauvetage est terminé.
     */
    private final String rescueEnded;

    /**
     * Int, nombre d'essaie effectué pour la détection de la balle.
     */
    private int failedAttempt;
    /**
     * Int, angle de rotation total effectuée.
     */
    private int totalRotation;

    /**
     * Int, zoom actuelle de la caméra du drone.
     */
    private int zoom;

    /**
     * Constructeur de la classe FollowLine, créé l'objet et initialise ses données membres.
     * @param caller MainActivity, instance de l'activité principale, permet d'accéder à différents éléments du UI.
     * @param controller AircraftController, controlleur du drone.
     * @param cameraController CameraController, controlleur de la caméra du drone.
     * @param visionHelper VisionHelper, service de traitement d'images.
     */
    public BallRescue(MainActivity caller, AircraftController controller, CameraController cameraController, VisionHelper visionHelper) {
        super(caller, controller, cameraController, visionHelper);
        rescueEnded = caller.getResources().getString(R.string.ballRescueEnded);
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
            zoom = 6;
            cameraController.lookAtAngle(-55);
            cameraController.setZoom(CameraController.MIN_OPTICAL_ZOOM * zoom, djiError1 -> {
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

            cameraController.setZoom(CameraController.ZOOM_1X, djiError -> rescue());
        }
        else {
            // Si le zoom est au minimum.
            if (zoom == 1) {
                zoom = 6;
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
                    else {
                        objectifStarted = false;
                        controller.land(() -> {
                            caller.showToast(rescueEnded);
                            cameraController.lookDown();
                            caller.setUIState(true);
                        });
                    }
                }
                else
                    new Handler().postDelayed(this::search, 500);
            }
            else
                cameraController.setZoom(CameraController.MIN_OPTICAL_ZOOM * --zoom, djiError -> {
                    search();
                });
        }
    }

    /**
     * Fonction qui permet d'obtenir la coordonnée de la balle.
     * @param points Point[], points du contour de la balle.
     * @return Point, coordonnée du milieu de la balle.
     */
    private Point getBall(Point[] points) {
        return points.length > BALL_DETECTION_THRESHOLD ? Detector.getAveragePoint(points) : null;
    }

    /**
     * Fonction qui permet de détecter le contour de la balle.
     * @param source Mat, matrice à analyzer.
     * @return Point[], points du contour de la balle.
     */
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

    /**
     * Méthode qui permet de déplacer le drone jusqu'à la balle.
     */
    private void rescue() {
        Mat matSource = getFrame();

        Point ball = getBall(detectBall(matSource));
        Point center = Detector.getCenterPoint(matSource);

        // Si le drone voit la balle.
        if (ball != null) {
            double angle = Detector.detectAngle(center, ball);
            if (angle > 90)
                angle = angle - 180;
            else if (angle < -90)
                angle = angle + 180;

            controller.faceAngle((int)angle, () -> {
                controller.setCurrentSpeed(AircraftController.MAXIMUM_AIRCRAFT_SPEED);
                controller.goForward(2000, null);
                rescue();
            });
        }
        // Si le drone ne voit plus la balle.
        else
            controller.land(() -> {
                objectifStarted = false;
                caller.showToast(rescueEnded);
                cameraController.lookDown();
                caller.setUIState(true);
            });
    }
}