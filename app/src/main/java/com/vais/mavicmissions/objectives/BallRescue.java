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

/**
 * Classe qui gère l'accomplissement de l'objectif 2, le sauvetage d'une balle.
 */
public class BallRescue extends Objectif {
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
            cameraController.lookAtAngle(-35);
            search();
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

        // Filter les couleurs de la balle.
        Mat yellow = visionHelper.filterColor(matSource, Color.YELLOW);
        Mat green = visionHelper.filterColor(matSource, Color.BALL_GREEN);

        // Combiner les filtres de couleurs.
        Mat combination = new Mat();
        Core.add(yellow, green, combination);

        // Trouver le plus gros contour.
        List<MatOfPoint> contours = visionHelper.contoursDetection(combination);
        MatOfPoint biggerContour = visionHelper.getBiggerContour(contours);
        Point[] points = biggerContour.toArray();

        Point avg = visionHelper.getAveragePoint(points);

        // Afficher la position de la balle.
        Imgproc.circle(matSource, avg, 2, new Scalar(255, 255, 0, 255), 10);
        showFrame(matSource);

        new Handler().postDelayed(this::search, 500);
    }

    /**
     * Méthode qui permet de se déplacer vers la balle.
     */
    private void rescue() {

    }
}