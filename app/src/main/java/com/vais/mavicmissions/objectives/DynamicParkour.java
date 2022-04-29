package com.vais.mavicmissions.objectives;

import android.os.Handler;
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
import java.util.List;

public class DynamicParkour extends Objectif {
    private AircraftInstruction lastInstruction;
    private String parkourEnded;

    public DynamicParkour(MainActivity caller, AircraftController controller, CameraController cameraController, VisionHelper visionHelper) {
        super(caller, controller, cameraController, visionHelper);

        parkourEnded = caller.getResources().getString(R.string.dynamicParourEnded);
        lastInstruction = null;
    }

    public void startDynamicParkour() {
        setStopButton(caller.btnDynamicParkour);

        controller.setCurrentSpeed(AircraftController.AIRCRAFT_SEEKING_MODE_SPEED);
        lastInstruction = null;

        caller.showToast(caller.getResources().getString(R.string.dynamicParcourStart));

        startObjectif(djiError -> {
            // Commencer la recherche de pancartes.
            controller.goForward(1000, () -> {
                controller.setCurrentSpeed(AircraftController.AIRCRAFT_SEEKING_MODE_SPEED);
                seekInstructions();
            });
        });
    }

    private void seekInstructions() {
        Shape detectedShape;
        boolean seek = true;
        boolean stop = false;

        if (!objectifStarted)
            return;

        // Capturer le flux vidéo.
        Mat matSource = getFrame();

        // Effectuer une détection de contours et isoler le plus gros.
        List<MatOfPoint> contours = visionHelper.contoursDetection(visionHelper.prepareContourDetection(matSource));
        MatOfPoint biggerContour = visionHelper.getBiggerContour(contours);

        // Détecter l'instruction.
        if (biggerContour != null) {
            detectedShape = Detector.detectShape(visionHelper.prepareContourDetection(matSource), visionHelper, biggerContour);

            // Exécuter l'action selon l'instruction.
            if (detectedShape == Shape.ARROW) {
                // Détecter les coins de la flèche.
                double angle = 0;
                Mat arr = visionHelper.prepareCornerDetection(matSource);
                MatOfPoint corners = visionHelper.detectCorners(arr, 3, 90);

                Mat arrow = Detector.detectArrow(arr, visionHelper, corners.toArray());
                if (arrow != null) {
                    Point[] croppedCorners = visionHelper.detectCorners(arrow, 3, 0.6f, 150).toArray();
                    Point head = Detector.findArrowHead(Detector.findCenterMass(arrow), croppedCorners);

                    if (head != null)
                        angle = Detector.detectAngle(arrow, head);

                    // Afficher le résultat.
                    showFrame(arrow);

                    if (lastInstruction == null)
                        lastInstruction = new AircraftInstruction(FlyInstruction.GO_TOWARDS, angle);
                    else if (new AircraftInstruction(FlyInstruction.GO_TOWARDS, angle).compare(lastInstruction)) {
                        seek = false;
                        executeInstruction(lastInstruction);
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
                    lastInstruction = new AircraftInstruction(FlyInstruction.GO_DOWN);
                else if (new AircraftInstruction(FlyInstruction.GO_UP).compare(lastInstruction)) {
                    seek = false;
                    executeInstruction(lastInstruction);
                    lastInstruction = null;
                }
                else {
                    lastInstruction = null;
                    stop = true;
                }
            }
            else if (detectedShape == Shape.D) {
                if (lastInstruction == null)
                    lastInstruction = new AircraftInstruction(FlyInstruction.GO_DOWN);
                else if (new AircraftInstruction(FlyInstruction.GO_DOWN).compare(lastInstruction)) {
                    seek = false;
                    executeInstruction(lastInstruction);
                    lastInstruction = null;
                }
                else {
                    lastInstruction = null;
                    stop = true;
                }
            }
            else if (detectedShape == Shape.H) {
                if (lastInstruction == null)
                    lastInstruction = new AircraftInstruction(FlyInstruction.TAKEOFF_LAND);
                else if (new AircraftInstruction(FlyInstruction.TAKEOFF_LAND).compare(lastInstruction)) {
                    seek = false;
                    executeInstruction(lastInstruction);
                    lastInstruction = null;
                    objectifStarted = false;
                }
                else {
                    lastInstruction = null;
                    stop = true;
                }
            }
        }

        // Continuer la recherche si rien n'a été trouvé.
        if (seek) {
            if (stop)
                controller.stop(null);
            else
                controller.goForward(2000, null);
            new Handler().postDelayed(this::seekInstructions, 250);
        }
    }

    private void executeInstruction(AircraftInstruction instruction) {
        controller.stop(() -> {
            if (instruction.getInstruction() == FlyInstruction.GO_TOWARDS) {
                controller.faceAngle((int)instruction.getAngle(), () -> {
                    controller.goForward(2000, null);
                    new Handler().postDelayed(this::seekInstructions, 500);
                });
            }
            else if (instruction.getInstruction() == FlyInstruction.GO_UP) {
                controller.stop(() -> {
                    controller.goUp(1000, () -> {
                        cameraController.setZoom(getRightZoom(), djiError -> {
                            controller.goForward(2000, null);
                            new Handler().postDelayed(this::seekInstructions, 500);
                        });
                    });
                });
            }
            else if (instruction.getInstruction() == FlyInstruction.GO_DOWN) {
                controller.stop(() -> {
                    controller.goDown(1000, () -> {
                        cameraController.setZoom(getRightZoom(), djiError -> {
                            controller.goForward(2000, null);
                            new Handler().postDelayed(this::seekInstructions, 500);
                        });
                    });
                });
            }
            else if (instruction.getInstruction() == FlyInstruction.TAKEOFF_LAND) {
                controller.land(() -> {
                    caller.showToast(parkourEnded);
                    cameraController.lookDown();
                    caller.setUIState(true);
                });
            }
        });
    }
}