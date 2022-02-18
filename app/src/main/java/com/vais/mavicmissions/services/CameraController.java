package com.vais.mavicmissions.services;

import androidx.annotation.NonNull;

import java.util.List;

import dji.sdk.camera.Camera;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.products.Aircraft;

public class CameraController {
    private Aircraft aircraft;

    private Camera camera;
    private Gimbal gimbal;

    public CameraController(@NonNull Aircraft aircraft) {
        this.aircraft = aircraft;

        camera = aircraft.getCamera();
        gimbal = aircraft.getGimbal();
    }
}