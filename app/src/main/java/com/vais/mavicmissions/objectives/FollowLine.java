package com.vais.mavicmissions.objectives;

import android.os.Handler;

import com.vais.mavicmissions.Enum.Color;
import com.vais.mavicmissions.MainActivity;
import com.vais.mavicmissions.R;
import com.vais.mavicmissions.services.Detector;
import com.vais.mavicmissions.services.drone.AircraftController;
import com.vais.mavicmissions.services.drone.CameraController;
import com.vais.mavicmissions.services.VisionHelper;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

/**
 * Classe qui gère l'accomplissement de l'objectif 2, le suivi d'une ligne verte.
 */
public class FollowLine extends Objectif {
    private Mat currentView;

    /**
     * Constructeur de la classe FollowLine, créé l'objet et initialise ses données membres.
     * @param caller MainActivity, instance de l'activité principale, permet d'accéder à différents éléments du UI.
     * @param controller AircraftController, controlleur du drone.
     * @param cameraController CameraController, controlleur de la caméra du drone.
     * @param visionHelper VisionHelper, service de traitement d'images.
     */
    public FollowLine(MainActivity caller, AircraftController controller, CameraController cameraController, VisionHelper visionHelper) {
        super(caller, controller, cameraController, visionHelper);
    }

    /**
     * Méthode qui commence le processus de suivi d'une ligne verte.
     */
    public void startFollowLine() {
        // Désactiver les boutons, excepté le bouton d'arrêt.
        setStopButton(caller.btnFollowLine);
        caller.showToast(caller.getResources().getString(R.string.followLineStart));

        // Configurer la vitesse du drone.
        controller.setCurrentSpeed(AircraftController.AIRCRAFT_FOLLOW_MODE_SPEED);

        // Commencer l'objectif.
        startObjectif(djiError -> {
            cameraController.lookDown();
            getOnLine();
        });
    }

    /**
     * Méthode qui permet au drone de se placer au dessus de la ligne verte.
     */
    public void getOnLine() {
        // Quitter si l'objectif n'est pas démarré.
        if (!objectifStarted)
            return;

        /*Point[] points = detectLine();

        if (points.length != 0) {
            Point avg = Detector.getAveragePoint(points);
            Point center = Detector.getCenterPoint(currentView);

            if (avg.x > center.x - 75 && avg.x < center.x + 75) {
                //followLine();
                caller.showToast("Aligné");
            }
            else
                controller.faceAngle(AircraftController.ROTATION_RIGHT, this::centerLine);
        }
        else
            controller.goForward(250, this::getOnLine);*/
        followLine();
    }

    public void centerLine() {
        Point[] points = detectLine();

        Point avg = Detector.getAveragePoint(points);
        Point center = Detector.getCenterPoint(currentView);

        // Déplacer le drone à gauche.
        if (avg.x < center.x - 75)
            controller.goLeft(250, this::centerLine);
        // Déplacer le drone à droite.
        else if (avg.x > center.x + 75)
            controller.goRight(250, this::centerLine);
        else if (avg.x > center.x - 75 && avg.x < center.x + 75)
            followLine();
    }

    public Point[] detectLine() {
        // Capturer le flux vidéo.
        currentView = getFrame();

        // Isoler le vert.
        Mat green = visionHelper.filterColor(currentView, Color.LINE_GREEN);

        // Détecter les coins.
        MatOfPoint corners = visionHelper.detectCorners(green, 25, 0.5f, 15);

        return corners.toArray();
    }

    /**
     * Méthode qui permet de trouver la direction de la ligne verte.
     */
    private void followLine() {
        // Quitter si l'objectif n'est pas démarré.
        if (!objectifStarted)
            return;

        // Détecter les coins.
        Point[] points = detectLine();
        Point center = Detector.getCenterPoint(currentView);

        // Trouver la direction de la ligne.
        int generalDirection = AircraftController.ROTATION_FRONT;
        int up = 0, right = 0, left = 0;

        // Pour chaque coins détectés.
        for (Point p : points) {
            // Déterminer la direction du point.
            if (p.y < center.y && p.x > center.x - 75 && p.x < center.x + 75)
                up++;
            else if (p.x > center.x && p.y > center.y - 25 && p.y < center.y + 25)
                right++;
            else if (p.x < center.x && p.y > center.y - 25 && p.y < center.y + 25)
                left++;

            // Afficher le point.
            Imgproc.circle(currentView, p, 2, new Scalar(255, 0, 0, 255), 10);
        }
        Imgproc.circle(currentView, center, 2, new Scalar(0, 255, 0, 255), 10);
        Imgproc.circle(currentView, new Point(center.x, center.y - 25), 2, new Scalar(0, 255, 0, 255), 10);
        Imgproc.circle(currentView, new Point(center.x, center.y + 25), 2, new Scalar(0, 255, 0, 255), 10);

        // Déterminer la direction générale.
        if (right > left && right > up) {
            generalDirection = AircraftController.ROTATION_RIGHT;
            caller.showToast("R");
        }
        else if (left > right && left > up) {
            generalDirection = AircraftController.ROTATION_LEFT;
            caller.showToast("L");
        }
        else if (left == 0 && right == 0 && up == 0) {
            controller.stop(null);
            return;
        }
        else
            caller.showToast("U");

        // Effectuer l'action requise.
        changeDirection(generalDirection);

        // Afficher le résultat.
        showFrame(currentView);
    }

    /**
     * Méthode qui change la direction du drone selon la direction de la ligne.
     * @param direction Int, rotation que le drone doit effectuer.
     */
    private void changeDirection(int direction) {
        // Selon la direction reçue.
        switch (direction) {
            case AircraftController.ROTATION_FRONT:
                // Avancer pendant 2 sec et continuer à chercher la ligne.
                controller.goForward(2000, null);
                new Handler().postDelayed(this::followLine, 1000);
                break;
            case AircraftController.ROTATION_RIGHT:
            case AircraftController.ROTATION_LEFT:
                // Arrêter le drone et effectuer la rotation.
                controller.stop(() -> {
                    controller.faceAngle(direction, () -> {
                        // Avancer pendant 2 sec et continuer à chercher la ligne.
                        controller.goForward(2000, null);
                        new Handler().postDelayed(this::followLine, 1000);
                    });
                });
                break;
        }
    }
}