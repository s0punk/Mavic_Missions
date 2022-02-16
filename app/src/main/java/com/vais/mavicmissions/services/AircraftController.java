package com.vais.mavicmissions.services;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.vais.mavicmissions.application.MavicMissionApp;

import java.util.Timer;
import java.util.TimerTask;

import dji.common.error.DJIError;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

public class AircraftController {
    private static final int COMMAND_TIMEOUT = 5000;

    private Aircraft aircraft;
    private FlightController flightController;
    private AircraftListener listener;

    private MavicMissionApp app;

    private boolean hasTakenOff;
    private boolean controllerReady;

    private float pitch;
    private float roll;
    private float yaw;
    private float throttle;

    private class SendVirtualStickDataTask extends TimerTask {
        @Override
        public void run() {
            if (VerificationUnit.isFlightControllerAvailable()) {
                MavicMissionApp.getAircraftInstance()
                        .getFlightController()
                        .sendVirtualStickFlightControlData(new FlightControlData(pitch, roll, yaw, throttle), (CommonCallbacks.CompletionCallback) djiError -> { });
            }
        }
    }

    public interface AircraftListener {
        public void onControllerStateChanged(boolean controllerState);
    }

    public AircraftController(@NonNull Aircraft aircraft, @NonNull MavicMissionApp app, @NonNull AircraftListener listener) {
        controllerReady = false;
        hasTakenOff = false;
        this.listener = listener;

        if (app.getRegistered()) {
            this.aircraft = aircraft;
            this.flightController = aircraft.getFlightController();
            this.app = app;

            flightController.setVirtualStickModeEnabled(true,  djiError -> { flightController.setVirtualStickAdvancedModeEnabled(true); });
            new Handler().postDelayed(() -> setControllerReady(true), COMMAND_TIMEOUT);

            flightController.setYawControlMode(YawControlMode.ANGLE);
            flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
            flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
            flightController.setRollPitchControlMode(RollPitchControlMode.ANGLE);

            resetAxis();
        }
    }

    public void destroy() {
        flightController.setVirtualStickModeEnabled(false,  djiError -> { flightController.setVirtualStickAdvancedModeEnabled(false); });
        new Handler().postDelayed(() -> setControllerReady(false), COMMAND_TIMEOUT);
    }

    public void resetAxis() {
        pitch = 0;
        roll = 0;
        yaw = 0;
        throttle = 0;
    }

    public void takeOff() {
        if (aircraft != null && flightController != null && !hasTakenOff) {
            setControllerReady(false);
            flightController.startTakeoff(djiError -> {
                new Handler().postDelayed(() -> {
                    setControllerReady(true);
                    hasTakenOff = true;
                }, COMMAND_TIMEOUT);
            });
        }
    }

    public void land() {
        if (aircraft != null && flightController != null && hasTakenOff) {
            resetAxis();

            setControllerReady(false);
            flightController.startLanding(djiError -> {
                new Handler().postDelayed(() -> {
                    flightController.confirmLanding(djiError1 -> {
                        new Handler().postDelayed(() -> {
                            setControllerReady(true);
                            hasTakenOff = false;
                        }, COMMAND_TIMEOUT);
                    });
                }, COMMAND_TIMEOUT);
            });
        }
    }

    private void sendTask() {
        if (aircraft != null && flightController != null && controllerReady && hasTakenOff) {
            SendVirtualStickDataTask task = new SendVirtualStickDataTask();
            Timer timer = new Timer();
            timer.schedule(task, 100, 200);
        }
    }

    public void goLeft(int degree) {
        resetAxis();
        roll = -degree;
        sendTask();
    }

    public void goRight(int degree) {
        resetAxis();
        roll = degree;
        sendTask();
    }

    public void goForward(int degree) {
        resetAxis();
        pitch = -degree;
        sendTask();
    }

    public void goBack(int degree) {
        resetAxis();
        pitch = degree;
        sendTask();
    }

    private void setControllerReady(boolean ready) {
        controllerReady = ready;
        listener.onControllerStateChanged(ready);
    }

    public Boolean getHasTakenOff() { return hasTakenOff; }
    public float getPitch() { return pitch; }
    public float getRoll() { return roll; }
    public float getYaw() { return yaw; }
    public float getThrottle() { return throttle; }
}