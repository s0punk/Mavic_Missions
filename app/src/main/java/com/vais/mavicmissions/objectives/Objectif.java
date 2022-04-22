package com.vais.mavicmissions.objectives;

import com.vais.mavicmissions.MainActivity;
import com.vais.mavicmissions.services.AircraftController;
import com.vais.mavicmissions.services.CameraController;
import com.vais.mavicmissions.services.VisionHelper;

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

    public boolean isObjectifStarted() {
        return objectifStarted;
    }

    public void setObjectifStarted(boolean objectifStarted) {
        this.objectifStarted = objectifStarted;
    }
}