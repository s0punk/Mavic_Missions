package com.vais.mavicmissions.services;

import android.content.Context;
import android.os.Handler;

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
import io.reactivex.annotations.NonNull;

public class AircraftController {
    private Aircraft aircraft;
    private FlightController flightController;
    private AircraftListener listener;
//https://guides.codepath.com/android/Creating-Custom-Listeners
    private MavicMissionApp app;

    private boolean hasTakenOff;

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
        public void onControllerReady();
    }

    public AircraftController(Aircraft aircraft, MavicMissionApp app) {
        listener = null;
        if (aircraft != null && app.getRegistered()) {
            this.aircraft = aircraft;
            this.flightController = aircraft.getFlightController();
            this.app = app;
            hasTakenOff = false;

            flightController.setVirtualStickModeEnabled(true,  djiError -> { flightController.setVirtualStickAdvancedModeEnabled(true); });
            wait(5);

            flightController.setYawControlMode(YawControlMode.ANGLE);
            flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
            flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
            flightController.setRollPitchControlMode(RollPitchControlMode.ANGLE);

            resetAxis();
        }
    }

    public void destroy() {
        flightController.setVirtualStickModeEnabled(false,  djiError -> { flightController.setVirtualStickAdvancedModeEnabled(false); });
        wait(5);
    }

    public void resetAxis() {
        pitch = 0;
        roll = 0;
        yaw = 0;
        throttle = 0;
    }

    public void takeOff() {
        if (aircraft != null && flightController != null && !hasTakenOff) {
            flightController.startTakeoff(djiError -> {
                hasTakenOff = true;
                wait(5);
            });
        }
    }

    public void land() {
        if (aircraft != null && flightController != null) {
            resetAxis();
            flightController.startLanding(djiError -> flightController.confirmLanding(djiError1 -> {
                hasTakenOff = false;
            }));
        }
    }

    private void sendTask() {
        if (aircraft != null && flightController != null && hasTakenOff) {
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

    private void wait(int waitTime) {
        Handler handler = new Handler();
        handler.postDelayed(() -> { }, waitTime);
    }

    public AircraftListener getListener() { return listener; }
    public void setListener(AircraftListener listener) { this.listener = listener; }
    public Boolean getHasTakenOff() { return hasTakenOff; }
    public float getPitch() { return pitch; }
    public float getRoll() { return roll; }
    public float getYaw() { return yaw; }
    public float getThrottle() { return throttle; }
}