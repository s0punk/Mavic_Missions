package com.vais.mavicmissions.services;

import com.vais.mavicmissions.application.MavicMissionApp;

import java.util.Timer;
import java.util.TimerTask;

import dji.common.error.DJIError;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.util.CommonCallbacks;

public class AircraftController {
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

    public float getPitch() { return pitch; }
    public void setPitch(float pitch) { this.pitch = pitch; }
    public float getRoll() { return roll; }
    public void setRoll(float roll) { this.roll = roll; }
    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }
    public float getThrottle() { return throttle; }
    public void setThrottle(float throttle) { this.throttle = throttle; }
    public SendVirtualStickDataTask getControlTask() { return controlTask; }
    public void setControlTask(SendVirtualStickDataTask controlTask) { this.controlTask = controlTask; }
    public Timer getTimer() { return timer; }
    public void setTimer(Timer timer) { this.timer = timer; }
}