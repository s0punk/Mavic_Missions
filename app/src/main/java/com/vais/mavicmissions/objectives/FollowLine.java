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
 * Simon-Olivier Vaillancourt
 * 2022-05-20
 * DJI Mavic 2 Entreprise
 * Classe qui gère l'accomplissement de l'objectif 2, le suivi d'une ligne verte.
 */
public class FollowLine extends Objectif {
    /**
     * Int, indique le nombre de coins à détecter lors de la détection de la direction de la ligne.
     */
    private final static int DIRECTION_DETECTION = 55;
    /**
     * Int, indique le nombre de coins à détecter lors de la détection de l'alignement du drone.
     */
    private final static int ALIGNEMENT_DETECTION = 2;
    /**
     * Int, indique la distance minimum des coins lors de la détection de la direction de la ligne.
     */
    private final static int DIRECTION_DISTANCE = 25;
    /**
     * Int, indique la distance minimum des coins lors de l'alignement du drone.
     */
    private final static int ALIGNEMENT_DISTANCE = 175;

    /**
     * Mat, dernière capture du flux vidéo prise.
     */
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
        /*
          Suivi d'une ligne verte

          Informations de vol:
          - Vitesse de déplacement: 0.25 m/s
          - Hauteur de décollage: 1.2 mètres (par défaut)

          Intersection: Le drone va se diriger vers l'endroit où il est le plus sûr que la ligne se trouve. Cela dépend du résultat de la détection de la ligne.
          Lors des tests, le drone a toujours été tout droit. Il reste possible que le drone décide de tourner.

          Déroulement de l'objectif:
          1. Le drone va décoller et positionner son gimbale à -80 degrées.
          2. Le drone va commencer son processus d'alignement avec la ligne.
          2.1 Le drone va détecter un point sur la ligne et calculer l'angle nécessaire pour être droit sur la ligne
          2.2 Le drone vérifier si la ligne est trop à droite ou trop à gauche, il se déplace en conséquance.
          3. Le drone commence à suivre la ligne, il détecte des points sur la ligne afin de trouver la direction à suivre.
          4. Après avoir suivi la ligne pendant environ 1 sec., le drone revérifie son alignement avec la ligne et retourne à l'étape 2.
         */

        // Désactiver les boutons, excepté le bouton d'arrêt.
        setStopButton(caller.btnFollowLine);
        caller.showToast(caller.getResources().getString(R.string.followLineStart));

        // Configurer la vitesse du drone.
        controller.setCurrentSpeed(AircraftController.AIRCRAFT_FOLLOW_MODE_SPEED);

        // Commencer l'objectif.
        startObjectif(djiError -> {
            cameraController.lookAtAngle(-80);
            align();
        });
    }

    /**
     * Fonction qui permet de détecter des point sur la ligne.
     * @param maxCorners Int, nombre maximum de points à détecter.
     * @param minDistance Int, distance minimum des points.
     * @return Point[], points de la ligne détectées.
     */
    public Point[] detectLine(int maxCorners, int minDistance) {
        // Capturer le flux vidéo.
        currentView = getFrame();

        // Isoler le vert.
        Mat green = visionHelper.filterColor(currentView, Color.LINE_GREEN);

        // Détecter les coins.
        MatOfPoint corners = visionHelper.detectCorners(green, maxCorners, 0.5f, minDistance);

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
        Point[] points = detectLine(DIRECTION_DETECTION, DIRECTION_DISTANCE);
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
            Imgproc.circle(currentView, p, 2, new Scalar(255, 0, 0, 255), 10);
        }

        showFrame(currentView);

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
            align();
            return;
        }
        else
            caller.showToast("U");

        // Effectuer l'action requise.
        changeDirection(generalDirection);
    }

    /**
     * Méthode qui aligne le drone par rapport à la ligne.
     */
    private void align() {
        // Détecter deux coins sur la ligne.
        Point[] corners = detectLine(ALIGNEMENT_DETECTION, ALIGNEMENT_DISTANCE);
        int halfX = currentView.width() / 2;

        // S'il y a 2 coins.
        if (corners.length == 2) {
            Point base = corners[1];
            Point head = corners[0];

            // Vérifier que les points soient bien alignés.
            Point[] alignement = Detector.detectPointAlignement(base, head);
            base = alignement[0];
            head = alignement[1];

            int angle = (int)Detector.detectAngle(base, head);
            if (angle > 90)
                angle = angle - 180;
            else if (angle < -90)
                angle = angle + 180;

            // Afficher les coins.
            Imgproc.circle(currentView, base, 2, new Scalar(0, 0, 255, 255), 10);
            Imgproc.circle(currentView, head, 2, new Scalar(0, 0, 255, 255), 10);
            showFrame(currentView);
            caller.showToast(angle + "");

            // Rotationner le drone correctement.
            controller.faceAngle(angle, () -> {
                // Centrer le drone par rapport à la ligne.
                Point[] nCorners = detectLine(ALIGNEMENT_DETECTION, ALIGNEMENT_DISTANCE);
                if (nCorners[0].x >= halfX - 100 && nCorners[0].x <= halfX + 100)
                    followLine();
                else if (nCorners[0].x < halfX) {
                    // Déplacer le drone à gauche.
                    controller.setCurrentSpeed(0.1f);
                    controller.goLeft(500, () -> {
                        controller.setCurrentSpeed(AircraftController.AIRCRAFT_FOLLOW_MODE_SPEED);
                        followLine();
                    });
                }
                else if (nCorners[0].x > halfX) {
                    // Déplacer le drone à droite.
                    controller.setCurrentSpeed(0.1f);
                    controller.goRight(500, () -> {
                        controller.setCurrentSpeed(AircraftController.AIRCRAFT_FOLLOW_MODE_SPEED);
                        followLine();
                    });
                }
                else
                    followLine();
            });
        }
        else
            align();
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
                controller.goForward(4000, null);
                new Handler().postDelayed(this::align, 1000);
                break;
            case AircraftController.ROTATION_RIGHT:
            case AircraftController.ROTATION_LEFT:
                // Arrêter le drone et effectuer la rotation.
                controller.stop(() -> {
                    controller.faceAngle(direction, () -> {
                        // Avancer pendant 2 sec et continuer à chercher la ligne.
                        controller.goForward(4000, null);
                        new Handler().postDelayed(this::align, 1000);
                    });
                });
                break;
        }
    }
}