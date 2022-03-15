package com.vais.mavicmissions.services;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vais.mavicmissions.application.MavicMissionApp;

import org.opencv.android.OpenCVLoader;

import java.util.Timer;
import java.util.TimerTask;

import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class AircraftController {
    public static final int COMMAND_TIMEOUT = 5000;
    public static final int MINIMUM_COMMAND_DURATION = 100;
    public static final int INFINITE_COMMAND = 0;
    public static final int MAXIMUM_AIRCRAFT_SPEED = 1;

    private static final int COMMAND_RESET = 500;
    private static final int ROTATION_DURATION = 1500;

    private static final int ROTATION_FRONT = 0;
    private static final int ROTATION_LEFT = -90;
    private static final int ROTATION_RIGHT = 90;
    private static final int ROTATION_BACK = -180;

    private Aircraft aircraft;
    private FlightController flightController;

    private MavicMissionApp app;

    private boolean hasTakenOff;
    private boolean controllerReady;

    private float pitch;
    private float roll;
    private float yaw;
    private float throttle;
    private boolean velocityMode;

    private class SendVirtualStickDataTask extends TimerTask {
        @Override
        public void run() {
            if (VerificationUnit.isFlightControllerAvailable()) {
                MavicMissionApp.getAircraftInstance()
                        .getFlightController()
                        .sendVirtualStickFlightControlData(new FlightControlData(pitch, roll, yaw, throttle), djiError -> { });
            }
        }
    }

    public interface ControllerListener {
        void onControllerReady();
    }

    public AircraftController(@NonNull Aircraft aircraft, @NonNull MavicMissionApp app, @Nullable ControllerListener listener) {
        controllerReady = false;
        hasTakenOff = false;

        if (app.getRegistered()) {
            this.aircraft = aircraft;
            this.flightController = aircraft.getFlightController();
            this.app = app;

            flightController.setVirtualStickModeEnabled(true,  djiError -> flightController.setVirtualStickAdvancedModeEnabled(true));
            new Handler().postDelayed(() -> {
                controllerReady = true;

                if (listener != null)
                    listener.onControllerReady();

            }, COMMAND_TIMEOUT);

            flightController.setYawControlMode(YawControlMode.ANGLE);
            flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
            flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
            flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
            velocityMode = true;
            resetAxis();
        }
    }

    public void destroy() {
        flightController.setVirtualStickModeEnabled(false,  djiError -> flightController.setVirtualStickAdvancedModeEnabled(false));
        new Handler().postDelayed(() -> {
            controllerReady = false;
            DJISDKManager.getInstance().startConnectionToProduct();
        }, COMMAND_TIMEOUT);
    }

    public void resetAxis() {
        pitch = 0;
        roll = 0;
        throttle = 0;
    }

    public void takeOff(@NonNull ControllerListener listener) {
        if (flightController != null && !hasTakenOff) {
            controllerReady = false;
            flightController.startTakeoff(djiError ->
                    new Handler().postDelayed(() -> {
                        controllerReady = true;
                        hasTakenOff = true;
                        listener.onControllerReady();
                    }, COMMAND_TIMEOUT)
            );
        }
    }

    public void land(@NonNull ControllerListener listener) {
        if (flightController != null && hasTakenOff) {
            resetAxis();

            controllerReady = false;
            flightController.startLanding(djiError -> {
                new Handler().postDelayed(() -> {
                    flightController.confirmLanding(djiError1 -> {
                        new Handler().postDelayed(() -> {
                            controllerReady = true;
                            hasTakenOff = false;
                            listener.onControllerReady();
                        }, COMMAND_TIMEOUT);
                    });
                }, COMMAND_TIMEOUT);
            });
        }
    }

    private void sendTask() {
        if (flightController != null && controllerReady && hasTakenOff) {
            SendVirtualStickDataTask task = new SendVirtualStickDataTask();
            Timer timer = new Timer();
            timer.schedule(task, 100, 200);
        }
    }

    private void waitCommandDuration(int time, ControllerListener listener) {
        if (time >= MINIMUM_COMMAND_DURATION)
            new Handler().postDelayed(() -> {
                resetAxis();
                new Handler().postDelayed(() -> listener.onControllerReady(), COMMAND_RESET);
            }, time);
    }

    public void goLeft(int time, ControllerListener listener) {
        resetAxis();
        pitch = velocityMode ? -MAXIMUM_AIRCRAFT_SPEED : MAXIMUM_AIRCRAFT_SPEED;

        sendTask();
        waitCommandDuration(time, listener);
    }

    public void goRight(int time, ControllerListener listener) {
        resetAxis();
        pitch = velocityMode ? MAXIMUM_AIRCRAFT_SPEED : -MAXIMUM_AIRCRAFT_SPEED;

        sendTask();
        waitCommandDuration(time, listener);
    }

    public void goForward(int time, ControllerListener listener) {
        resetAxis();
        roll = velocityMode ? MAXIMUM_AIRCRAFT_SPEED : -MAXIMUM_AIRCRAFT_SPEED;

        sendTask();
        waitCommandDuration(time, listener);
    }

    public void goBack(int time, ControllerListener listener) {
        resetAxis();
        roll = velocityMode ? -MAXIMUM_AIRCRAFT_SPEED : MAXIMUM_AIRCRAFT_SPEED;

        sendTask();
        waitCommandDuration(time, listener);
    }

    public void faceAngle(int angle, ControllerListener listener) {
        resetAxis();
        yaw = angle;

        sendTask();
        new Handler().postDelayed(listener::onControllerReady, ROTATION_DURATION);
    }

    public void faceFront(ControllerListener listener) {
        resetAxis();
        yaw = ROTATION_FRONT;
        sendTask();
        new Handler().postDelayed(listener::onControllerReady, ROTATION_DURATION);
    }

    public void faceLeft(ControllerListener listener) {
        resetAxis();
        yaw = ROTATION_LEFT;

        sendTask();
        new Handler().postDelayed(listener::onControllerReady, ROTATION_DURATION);
    }

    public void faceRight(ControllerListener listener) {
        resetAxis();
        yaw = ROTATION_RIGHT;

        sendTask();
        new Handler().postDelayed(listener::onControllerReady, ROTATION_DURATION);
    }

    public void faceBack(ControllerListener listener) {
        resetAxis();
        yaw = ROTATION_BACK;

        sendTask();
        new Handler().postDelayed(listener::onControllerReady, ROTATION_DURATION);
    }

    public Aircraft getAircraft() {
        return aircraft;
    }
}