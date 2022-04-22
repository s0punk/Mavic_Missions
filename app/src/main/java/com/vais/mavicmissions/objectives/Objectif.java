package com.vais.mavicmissions.objectives;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;

import com.vais.mavicmissions.MainActivity;
import com.vais.mavicmissions.R;
import com.vais.mavicmissions.services.drone.AircraftController;
import com.vais.mavicmissions.services.drone.CameraController;
import com.vais.mavicmissions.services.VisionHelper;

import org.opencv.core.Mat;

import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;

public abstract class Objectif {
    protected MainActivity caller;
    protected AircraftController controller;
    protected CameraController cameraController;
    protected VisionHelper visionHelper;

    protected boolean objectifStarted;

    public Objectif(MainActivity caller, AircraftController controller, CameraController cameraController, VisionHelper visionHelper) {
        this.caller = caller;
        this.controller = controller;
        this.cameraController = cameraController;
        this.visionHelper = visionHelper;

        objectifStarted = false;
    }

    protected void setStopButton(Button button) {
        // Configurer le bouton d'arrêt de l'objectif.
        caller.setUIState(false, button);
        button.setText(caller.getResources().getString(R.string.stop));
        objectifStarted = true;
    }

    protected void startObjectif(CommonCallbacks.CompletionCallback onReady) {
        // Vérifier l'état du drone.
        /*controller.checkVirtualStick(() -> {
            if (controller.getHasTakenOff()) {
                // Attérir puis décoller le drone.
                controller.land(() -> {
                    cameraController.lookDown();
                    controller.takeOff(() -> {
                        //showToast("Fin décollage");
                        cameraController.setZoom(caller.getRightZoom(), new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                onReady.onResult(null);
                            }
                        });
                    });
                });
            } else {
                // Décoller le drone.
                controller.takeOff(() -> {
                    //showToast("Fin décollage");
                    cameraController.setZoom(caller.getRightZoom(), new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            onReady.onResult(null);
                        }
                    });
                });
            }
        });*/
        onReady.onResult(null);
    }

    protected Mat getFrame() {
        return visionHelper.bitmapToMap(caller.cameraSurface.getBitmap());
    }

    protected void showFrame(Mat frame) {
        new Handler(Looper.getMainLooper()).post(() -> caller.ivResult.setImageBitmap(visionHelper.matToBitmap(frame)));
    }

    public boolean isObjectifStarted() {
        return objectifStarted;
    }

    public void setObjectifStarted(boolean objectifStarted) {
        this.objectifStarted = objectifStarted;
    }
}