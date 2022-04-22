package com.vais.mavicmissions.objectives;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import com.vais.mavicmissions.Enum.FlyInstruction;
import com.vais.mavicmissions.Enum.Shape;
import com.vais.mavicmissions.MainActivity;
import com.vais.mavicmissions.R;
import com.vais.mavicmissions.services.AircraftController;
import com.vais.mavicmissions.services.AircraftInstruction;
import com.vais.mavicmissions.services.CameraController;
import com.vais.mavicmissions.services.Detector;
import com.vais.mavicmissions.services.VisionHelper;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

import java.util.List;

import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;

public class DynamicParkour {
    private MainActivity caller;
    private AircraftController controller;
    private CameraController cameraController;
    private VisionHelper visionHelper;

    private AircraftInstruction lastInstruction;

    private String parkourEnded;
    private boolean dynamicParkourStarted;

    public DynamicParkour(MainActivity caller, AircraftController controller, CameraController cameraController, VisionHelper visionHelper) {
        this.caller = caller;
        this.controller = controller;
        this.cameraController = cameraController;
        this.visionHelper = visionHelper;

        dynamicParkourStarted = false;
        parkourEnded = caller.getResources().getString(R.string.dynamicParourEnded);
        lastInstruction = null;
    }

    public void startDynamicParkour() {
        // Configurer le bouton d'arrêt du parcour.
        caller.setUIState(false, caller.btnDynamicParkour);
        caller.btnDynamicParkour.setText(caller.getResources().getString(R.string.stop));
        dynamicParkourStarted = true;

        controller.setCurrentSpeed(AircraftController.AIRCRAFT_SEEKING_MODE_SPEED);
        lastInstruction = null;

        caller.showToast(caller.getResources().getString(R.string.dynamicParcourStart));

        // Vérifier l'état du drone.
        controller.checkVirtualStick(() -> {
            if (controller.getHasTakenOff()) {
                // Attérir puis décoller le drone.
                controller.land(() -> {
                    cameraController.lookDown();
                    controller.takeOff(() -> {
                        //showToast("Fin décollage");
                        cameraController.setZoom(caller.getRightZoom(), new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                // Commencer la recherche de pancartes.
                                controller.goForward(1000, () -> {
                                    controller.setCurrentSpeed(AircraftController.AIRCRAFT_SEEKING_MODE_SPEED);
                                    seekInstructions();
                                });
                            }
                        });
                    });
                });
            }
            else {
                // Décoller le drone.
                controller.takeOff(() -> {
                    //showToast("Fin décollage");
                    cameraController.setZoom(caller.getRightZoom(), new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            // Commencer la recherche de pancartes.
                            controller.goForward(1000, () -> {
                                controller.setCurrentSpeed(AircraftController.AIRCRAFT_SEEKING_MODE_SPEED);
                                seekInstructions();
                            });
                        }
                    });
                });
            }
        });
    }

    public void seekInstructions() {
        Shape detectedShape;
        boolean seek = true;
        boolean stop = false;

        if (!dynamicParkourStarted)
            return;

        // Capturer le flux vidéo.
        Bitmap source = caller.cameraSurface.getBitmap();
        Mat matSource = visionHelper.bitmapToMap(source);

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
                    angle = Detector.detectAngle(arrow, head);

                    // Afficher le résultat.
                    new Handler(Looper.getMainLooper()).post(() -> caller.ivResult.setImageBitmap(visionHelper.matToBitmap(arrow)));

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
                    dynamicParkourStarted = false;
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
            new Handler().postDelayed(this::seekInstructions, 500);
        }
    }

    private void executeInstruction(AircraftInstruction instruction) {
        controller.stop(() -> {
            if (instruction.getInstruction() == FlyInstruction.GO_TOWARDS) {
                controller.faceAngle((int)instruction.getAngle(), true, () -> {
                    controller.goForward(2000, null);
                    seekInstructions();
                });
            }
            else if (instruction.getInstruction() == FlyInstruction.GO_UP) {
                controller.stop(() -> {
                    controller.goUp(1000, () -> {
                        cameraController.setZoom(caller.getRightZoom(), djiError -> {
                            controller.goForward(2000, null);
                            seekInstructions();
                        });
                    });
                });
            }
            else if (instruction.getInstruction() == FlyInstruction.GO_DOWN) {
                controller.stop(() -> {
                    controller.goDown(1000, () -> {
                        cameraController.setZoom(caller.getRightZoom(), djiError -> {
                            controller.goForward(2000, null);
                            seekInstructions();
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

    public void setDynamicParkourStarted(boolean dynamicParkourStarted) { this.dynamicParkourStarted = dynamicParkourStarted; }
    public boolean isDynamicParkourStarted() { return dynamicParkourStarted; }
}