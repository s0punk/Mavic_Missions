package com.vais.mavicmissions.objectives;

import android.os.Handler;

import com.vais.mavicmissions.Enum.Color;
import com.vais.mavicmissions.MainActivity;
import com.vais.mavicmissions.R;
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

        // Configurer la vitesse du drone à 1 m/s.
        controller.setCurrentSpeed(AircraftController.AIRCRAFT_SEEKING_MODE_SPEED);

        // Commencer l'objectif.
        startObjectif(djiError -> {
            controller.goForward(2000, null);
            seekGreenLine();
        });
    }

    /**
     * Méthode qui permet de trouver une ligne verte.
     */
    private void seekGreenLine() {
        // Quitter si l'objectif n'est pas démarré.
        if (!objectifStarted)
            return;

        // Capturer le flux vidéo.
        Mat matSource = getFrame();

        // Isoler le vert.
        Mat green = visionHelper.filterColor(matSource, Color.LINE_GREEN);

        // Détecter les coins.
        Point center = new Point((int)green.width() / 2, (int)green.height() / 2);
        MatOfPoint corners = visionHelper.detectCorners(green, 25, 0.5f, 15);
        Point[] points = corners.toArray();

        // Trouver la direction de la ligne.
        int generalDirection = AircraftController.ROTATION_FRONT;
        int up = 0, right = 0, left = 0;

        // Pour chaque coins détectés.
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
        showFrame(matSource);
        caller.showToast(generalDirection + "");

        // Effectuer l'action requise.
        changeDirection(generalDirection);
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
                new Handler().postDelayed(this::seekGreenLine, 1000);
                break;
            case AircraftController.ROTATION_RIGHT:
            case AircraftController.ROTATION_LEFT:
                // Attendre 500 ms pour que le drone atteingne le coin de la ligne.
                new Handler().postDelayed(() -> {
                    // Arrêter le drone et effectuer la rotation.
                    controller.stop(() -> {
                        controller.faceAngle(direction, () -> {
                            // Avancer pendant 2 sec et continuer à chercher la ligne.
                            controller.goForward(2000, null);
                            new Handler().postDelayed(this::seekGreenLine, 1000);
                        });
                    });
                }, 500);
                break;
        }
    }
}