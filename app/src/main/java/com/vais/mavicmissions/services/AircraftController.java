package com.vais.mavicmissions.services;

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

    private SendVirtualStickDataTask controlTask;
    private Timer timer;

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

    public AircraftController(Aircraft aircraft) {
        if (aircraft != null) {
            this.aircraft = aircraft;
            this.flightController = aircraft.getFlightController();

            flightController.setVirtualStickModeEnabled(true,  djiError -> { flightController.setVirtualStickAdvancedModeEnabled(true); });

            flightController.setYawControlMode(YawControlMode.ANGLE);
            flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
            flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
            flightController.setRollPitchControlMode(RollPitchControlMode.ANGLE);

            pitch = 0;
            roll = 0;
            yaw = 0;
            throttle = 0;
        }
    }

    private void SendTask() {
        if (aircraft != null && flightController != null) {
            SendVirtualStickDataTask task = new SendVirtualStickDataTask();
            Timer timer = new Timer();
            timer.schedule(task, 100, 200);
        }
    }

    public void goLeft(int degree) {
        pitch = 0;
        roll = -degree;
        yaw = 0;
        throttle = 0;

        SendTask();
    }

    public void goRight(int degree) {
        pitch = 0;
        roll = degree;
        yaw = 0;
        throttle = 0;

        SendTask();
    }

    public void goForward(int degree) {
        pitch = -degree;
        roll = 0;
        yaw = 0;
        throttle = 0;

        SendTask();
    }

    public void goBack(int degree) {
        pitch = degree;
        roll = 0;
        yaw = 0;
        throttle = 0;

        SendTask();
    }

    public float getPitch() { return pitch; }
    public float getRoll() { return roll; }
    public float getYaw() { return yaw; }
    public float getThrottle() { return throttle; }
    public SendVirtualStickDataTask getControlTask() { return controlTask; }
    public Timer getTimer() { return timer; }
}