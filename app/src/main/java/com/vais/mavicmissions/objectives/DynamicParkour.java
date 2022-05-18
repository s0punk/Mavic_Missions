package com.vais.mavicmissions.objectives;

import android.os.Handler;
import com.vais.mavicmissions.Enum.Color;
import com.vais.mavicmissions.Enum.FlyInstruction;
import com.vais.mavicmissions.Enum.Shape;
import com.vais.mavicmissions.MainActivity;
import com.vais.mavicmissions.R;
import com.vais.mavicmissions.services.drone.AircraftController;
import com.vais.mavicmissions.services.drone.AircraftInstruction;
import com.vais.mavicmissions.services.drone.CameraController;
import com.vais.mavicmissions.services.Detector;
import com.vais.mavicmissions.services.VisionHelper;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import java.util.List;

/**
 * Classe qui gère le suivi d'un parcours dynamique.
 */
public class DynamicParkour extends Objectif {
    /**
     * Int, nombre de maximum de détection non-reconnue permis.
     */
    private final static int MAX_UNKNOWN_DETECTION = 25;

    /**
     * AircraftInstruction, dernière instruction détectée par le drone.
     */
    private AircraftInstruction lastInstruction;

    /**
     * String, message affiché lors de l'arrêt du parcours.
     */
    private String parkourEnded;

    /**
     * Int, nombre actuelle de détection non-reconnue.
     */
    private int unknownDetectionCount;

    /**
     * Constructeur de la classe DynamicParkour, créé l'objet et initialise ses données membres.
     * @param caller MainActivity, activité principale de l'application.
     * @param controller AircraftController, controlleur du drone.
     * @param cameraController CameraController, controlleur de la caméra du drone.
     * @param visionHelper VisionHelper, service de traitement d'images.
     */
    public DynamicParkour(MainActivity caller, AircraftController controller, CameraController cameraController, VisionHelper visionHelper) {
        super(caller, controller, cameraController, visionHelper);

        parkourEnded = caller.getResources().getString(R.string.dynamicParourEnded);
        lastInstruction = null;
    }

    /**
     * Méthode qui commence le suivi du parcours.
     */
    public void startDynamicParkour() {
        // Désactiver les boutons, excepté le bouton d'arrêt.
        setStopButton(caller.btnDynamicParkour);

        // Changer la vitesse du drone.
        controller.setCurrentSpeed(AircraftController.AIRCRAFT_SEEKING_MODE_SPEED);
        caller.showToast(caller.getResources().getString(R.string.dynamicParcourStart));

        unknownDetectionCount = 0;

        // Commencer l'objectif.
        startObjectif(djiError -> {
            // Commencer la recherche de pancartes.
            controller.goForward(1000, () -> {
                controller.setCurrentSpeed(AircraftController.AIRCRAFT_SEEKING_MODE_SPEED);
                seekInstructions();
            });
        });
    }

    /**
     * Méthode qui recherche une pancarte du parcours.
     */
    private void seekInstructions() {
        if (!objectifStarted)
            return;

        Shape detectedShape;
        boolean seek = true;
        boolean stop = false;

        // Capturer le flux vidéo.
        Mat matSource = getFrame();

        // Effectuer une détection de contours et isoler le plus gros.
        Mat filteredMat = visionHelper.prepareContourDetection(visionHelper.filterColor(matSource, Color.BLACK));
        List<MatOfPoint> contours = visionHelper.contoursDetection(filteredMat);
        MatOfPoint biggerContour = visionHelper.getBiggerContour(contours);

        // Détecter l'instruction.
        if (biggerContour != null) {
            detectedShape = Detector.detectShape(matSource, visionHelper, biggerContour, this);

            // Exécuter l'action selon l'instruction.

            // Flèche.
            if (detectedShape == Shape.ARROW) {
                // Détecter les coins de la flèche.
                double angle = 0;
                Mat arr = visionHelper.prepareCornerDetection(matSource);
                MatOfPoint corners = visionHelper.detectCorners(arr, 3, 90);

                Mat arrow = Detector.detectArrow(matSource, corners.toArray(), visionHelper);
                if (arrow != null) {
                    arrow = visionHelper.dilate(arrow, 5);

                    Point[] croppedCorners = visionHelper.detectCorners(arrow, 3, 0.6f, 150).toArray();
                    Point head = Detector.findArrowHead(Detector.findCenterMass(arrow), croppedCorners);

                    if (head != null) {
                        angle = Detector.detectAngle(new Point((int)(arrow.width() / 2), (int)(arrow.height() / 2)), head);
                        Imgproc.circle(arrow, head, 2, new Scalar(255, 0, 0, 255), 20);
                    }

                    // Afficher le résultat.
                    showFrame(arrow);

                    if (lastInstruction == null)
                        lastInstruction = new AircraftInstruction(FlyInstruction.GO_TOWARDS, angle);
                    else if (new AircraftInstruction(FlyInstruction.GO_TOWARDS, angle).compare(lastInstruction)) {
                        seek = false;
                        //executeInstruction(lastInstruction);
                        lastInstruction = null;
                    }
                    else {
                        lastInstruction = null;
                        stop = true;
                    }
                }
            }
            else if (detectedShape == Shape.U) {
                if (lastInstruction == null)
                    lastInstruction = new AircraftInstruction(FlyInstruction.GO_UP);
                else if (new AircraftInstruction(FlyInstruction.GO_UP).compare(lastInstruction)) {
                    seek = false;
                    caller.showToast("U");
                    //executeInstruction(lastInstruction);
                    lastInstruction = null;
                }
                else {
                    lastInstruction = null;
                    stop = true;
                }
            }
            // Down.
            else if (detectedShape == Shape.D) {
                if (lastInstruction == null)
                    lastInstruction = new AircraftInstruction(FlyInstruction.GO_DOWN);
                else if (new AircraftInstruction(FlyInstruction.GO_DOWN).compare(lastInstruction)) {
                    seek = false;
                    caller.showToast("D");
                    //executeInstruction(lastInstruction);
                    lastInstruction = null;
                }
                else {
                    lastInstruction = null;
                    stop = true;
                }
            }
            // Attérir.
            else if (detectedShape == Shape.H) {
                if (lastInstruction == null)
                    lastInstruction = new AircraftInstruction(FlyInstruction.TAKEOFF_LAND);
                else if (new AircraftInstruction(FlyInstruction.TAKEOFF_LAND).compare(lastInstruction)) {
                    seek = false;
                    caller.showToast("H");
                    //executeInstruction(lastInstruction);
                    lastInstruction = null;
                }
                else {
                    lastInstruction = null;
                    stop = true;
                }
            }
        }

        // Continuer la recherche si rien n'a été trouvé.
        /*if (seek) {
            if (++unknownDetectionCount > MAX_UNKNOWN_DETECTION)
                controller.land(() -> {
                    objectifStarted = false;
                    cameraController.lookDown();
                    controller.loseControl();
                    caller.showToast(parkourEnded);
                    caller.setUIState(true);
                });
            else if (stop)
                controller.stop(null);
            else
                controller.goForward(2500, null);
            new Handler().postDelayed(this::seekInstructions, 250);
        }*/
        new Handler().postDelayed(this::seekInstructions, 750);
    }

    /**
     * Méthode qui exécute l'instruction détecté par le drone.
     * @param instruction AircraftInstruction, instruction détecté par le drone.
     */
    private void executeInstruction(AircraftInstruction instruction) {
        // Arrêter le drone.
        controller.stop(() -> {
            // Aller en direction de la flèche.
            if (instruction.getInstruction() == FlyInstruction.GO_TOWARDS) {
                controller.faceAngle((int)instruction.getAngle(), () -> {
                    controller.goForward(2500, null);
                    new Handler().postDelayed(this::seekInstructions, 2000);
                });
            }
            // Monter l'altitude.
            else if (instruction.getInstruction() == FlyInstruction.GO_UP) {
                controller.stop(() -> controller.goUp(2000, () -> {
                    cameraController.setZoom(CameraController.ZOOM_3X, djiError -> {
                        controller.goForward(2500, null);
                        new Handler().postDelayed(this::seekInstructions, 2000);
                    });
                }));
            }
            // Descendre l'altitude.
            else if (instruction.getInstruction() == FlyInstruction.GO_DOWN) {
                controller.stop(() -> controller.goDown(1000, () -> {
                    cameraController.setZoom(CameraController.ZOOM_2_2X, djiError -> {
                        controller.goForward(2500, null);
                        new Handler().postDelayed(this::seekInstructions, 2000);
                    });
                }));
            }
            // Attérir.
            else if (instruction.getInstruction() == FlyInstruction.TAKEOFF_LAND) {
                controller.land(() -> {
                    controller.loseControl();
                    objectifStarted = false;
                    caller.showToast(parkourEnded);
                    cameraController.lookDown();
                    caller.setUIState(true);
                });
            }
        });
    }
}