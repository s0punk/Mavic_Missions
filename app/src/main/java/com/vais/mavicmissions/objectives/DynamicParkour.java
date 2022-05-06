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
        Mat filteredMat = visionHelper.prepareContourDetection(visionHelper.filterColor(matSource, Color.BLACK));
        List<MatOfPoint> contours = visionHelper.contoursDetection(filteredMat);
        MatOfPoint centeredContour = visionHelper.getCenteredContour(filteredMat, contours);

        // Détecter l'instruction.
        if (centeredContour != null) {
            detectedShape = Detector.detectShape(filteredMat, visionHelper, centeredContour, caller);

            // Dessiner le contour.
            for (Point p : centeredContour.toArray())
                Imgproc.circle(filteredMat, p, 1, new Scalar(255, 0, 0, 255), 5);
            showFrame(filteredMat);

            // Exécuter l'action selon l'instruction.
            if (detectedShape == Shape.ARROW) {
                // Détecter les coins de la flèche.
                double angle = 0;
                Mat arr = visionHelper.prepareCornerDetection(matSource);
                MatOfPoint corners = visionHelper.detectCorners(arr, 3, 90);

                Mat arrow = Detector.detectArrow(arr, corners.toArray());
                if (arrow != null) {
                    Point[] croppedCorners = visionHelper.detectCorners(arrow, 3, 0.6f, 150).toArray();
                    Point head = Detector.findArrowHead(Detector.findCenterMass(arrow), croppedCorners);

                    if (head != null) {
                        angle = Detector.detectAngle(arrow, head);
                        Imgproc.circle(arrow, head, 2, new Scalar(255, 0, 0, 255), 10);
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
                    //executeInstruction(lastInstruction);
                    caller.showToast("U");
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
                    //executeInstruction(lastInstruction);
                    caller.showToast("D");
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
                    //executeInstruction(lastInstruction);
                    caller.showToast("H");
                    lastInstruction = null;
                    //objectifStarted = false;
                }
                else {
                    lastInstruction = null;
                    stop = true;
                }
            }
        }

        // Continuer la recherche si rien n'a été trouvé.
        /*if (seek) {
            if (stop)
                controller.stop(null);
            else
                controller.goForward(4500, null);
            new Handler().postDelayed(this::seekInstructions, 250);
        }*/
        new Handler().postDelayed(this::seekInstructions, 500);
    }

    private void executeInstruction(AircraftInstruction instruction) {
        controller.stop(() -> {
            if (instruction.getInstruction() == FlyInstruction.GO_TOWARDS) {
                controller.faceAngle((int)instruction.getAngle(), () -> {
                    controller.goForward(4500, null);
                    new Handler().postDelayed(this::seekInstructions, 2000);
                });
            }
            else if (instruction.getInstruction() == FlyInstruction.GO_UP) {
                controller.stop(() -> {
                    controller.goUp(1000, () -> {
                        cameraController.setZoom(getRightZoom(), djiError -> {
                            controller.goForward(4500, null);
                            new Handler().postDelayed(this::seekInstructions, 2000);
                        });
                    });
                });
            }
            else if (instruction.getInstruction() == FlyInstruction.GO_DOWN) {
                controller.stop(() -> {
                    controller.goDown(1000, () -> {
                        cameraController.setZoom(getRightZoom(), djiError -> {
                            controller.goForward(4500, null);
                            new Handler().postDelayed(this::seekInstructions, 2000);
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