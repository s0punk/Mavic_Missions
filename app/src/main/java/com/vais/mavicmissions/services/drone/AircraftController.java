package com.vais.mavicmissions.services.drone;

import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.vais.mavicmissions.application.MavicMissionApp;
import com.vais.mavicmissions.services.VerificationUnit;

import java.util.Timer;
import java.util.TimerTask;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightOrientationMode;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class AircraftController {
    public static final int COMMAND_TIMEOUT = 5000;
    public static final int TAKEOFF_TIMEOUT = 7500;
    public static final int MINIMUM_COMMAND_DURATION = 100;
    public static final int INFINITE_COMMAND = 0;
    public static final int MAXIMUM_AIRCRAFT_SPEED = 1;
    public static final float AIRCRAFT_SEEKING_MODE_SPEED = 0.5f;
    public static final int MAXIMUM_VERTICAL_SPEED = 1;

    private static final int SEEKING_MODE_SCREENSHOT_DELAY = 500;

    private static final int COMMAND_RESET = 500;
    private static final int ROTATION_DURATION = 1500;

    public static final int ROTATION_FRONT = 0;
    public static final int ROTATION_LEFT = -90;
    public static final int ROTATION_RIGHT = 90;
    public static final int ROTATION_BACK = -180;

    private Aircraft aircraft;
    private FlightController flightController;

    private MavicMissionApp app;

    private boolean hasTakenOff;
    private boolean controllerReady;
    private boolean calibrated;

    private float currentSpeed;

    public float pitch;
    public float roll;
    public float yaw;
    public float throttle;
    private boolean velocityMode;

    private class SendVirtualStickDataTask extends TimerTask {
        @Override
        public void run() {
            if (VerificationUnit.isFlightControllerAvailable()) {
                flightController.sendVirtualStickFlightControlData(new FlightControlData(pitch, roll, yaw, throttle), djiError -> { });
            }
        }
    }

    public interface ControllerListener {
        void onControllerReady();
    }

    public AircraftController(@NonNull Aircraft aircraft, @NonNull MavicMissionApp app, @Nullable ControllerListener listener) {
        controllerReady = false;
        hasTakenOff = false;
        calibrated = false;

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

            setFlightControllerParams();
            velocityMode = true;
            resetAxis();
            setCurrentSpeed(MAXIMUM_AIRCRAFT_SPEED);
            yaw = flightController.getCompass().getHeading();
        }
    }

    public void setFlightControllerParams() {
        flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
        flightController.setYawControlMode(YawControlMode.ANGLE);
        flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
        flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
        flightController.setFlightOrientationMode(FlightOrientationMode.AIRCRAFT_HEADING, null);
    }

    public void checkVirtualStick(ControllerListener listener) {
        flightController.getVirtualStickModeEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
            @Override
            public void onSuccess(Boolean virtualStickEnabled) {
                if (!virtualStickEnabled)
                    flightController.setVirtualStickModeEnabled(true, djiError -> {
                        setFlightControllerParams();
                        listener.onControllerReady();
                    });
                else {
                    setFlightControllerParams();
                    listener.onControllerReady();
                }
            }
            @Override
            public void onFailure(DJIError djiError) { }
        });
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

    public float getHeight() {
        return flightController.getState().getAircraftLocation().getAltitude();
    }

    public void takeOff(@NonNull ControllerListener listener) {
        if (flightController != null && !hasTakenOff) {
            controllerReady = false;
            flightController.startTakeoff(djiError ->
                    new Handler().postDelayed(() -> {
                        controllerReady = true;
                        hasTakenOff = true;
                        listener.onControllerReady();
                    }, TAKEOFF_TIMEOUT)
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

                if (listener != null)
                    new Handler().postDelayed(listener::onControllerReady, COMMAND_RESET);
            }, time);
    }

    private void waitThrottleDuration(int time, ControllerListener listener) {
        if (time >= MINIMUM_COMMAND_DURATION)
            new Handler().postDelayed(() -> {
                throttle = 0;

                if (listener != null)
                    new Handler().postDelayed(listener::onControllerReady, COMMAND_RESET);
            }, time);
    }

    public void goUp(int time, ControllerListener listener) {
        throttle = MAXIMUM_VERTICAL_SPEED;
        sendTask();

        time = time == INFINITE_COMMAND ? 500 : time;
        waitThrottleDuration(time, listener);
    }

    public void goDown(int time, ControllerListener listener) {
        throttle = -MAXIMUM_VERTICAL_SPEED;
        sendTask();

        time = time == INFINITE_COMMAND ? 500 : time;
        waitThrottleDuration(time, listener);
    }

    public void goLeft(int time, ControllerListener listener) {
        resetAxis();
        pitch = velocityMode ? -currentSpeed : currentSpeed;

        sendTask();
        waitCommandDuration(time, listener);
    }

    public void goRight(int time, ControllerListener listener) {
        resetAxis();
        pitch = velocityMode ? currentSpeed : -currentSpeed;

        sendTask();
        waitCommandDuration(time, listener);
    }

    public void goForward(int time, ControllerListener listener) {
        resetAxis();
        roll = velocityMode ? currentSpeed : -currentSpeed;

        sendTask();
        waitCommandDuration(time, listener);
    }

    public void goBack(int time, ControllerListener listener) {
        resetAxis();
        roll = velocityMode ? -currentSpeed : currentSpeed;

        sendTask();
        waitCommandDuration(time, listener);
    }

    public void faceAngle(int angle, ControllerListener listener) {
        resetAxis();
        yaw = calculateRealAngle(angle);

        sendTask();
        if (listener != null)
            new Handler().postDelayed(listener::onControllerReady, ROTATION_DURATION);
    }

    public int calculateRealAngle(int desiredAngle) {
        int angle = 0;
        int droneAngle = (int)flightController.getCompass().getHeading();

        if (desiredAngle == ROTATION_FRONT)
            return droneAngle;

        angle = droneAngle + desiredAngle;
        if (angle > 180)
            angle = angle - 360;
        else if (angle < -180)
            angle = angle + 360;

        return angle;
    }

    public void stop(ControllerListener listener) {
        resetAxis();
        sendTask();

        if (listener != null)
            new Handler().postDelayed(listener::onControllerReady, COMMAND_RESET);
    }

    public Aircraft getAircraft() {
        return aircraft;
    }

    public FlightController getFlightController() { return flightController; }

    public boolean isCalibrated() { return calibrated; }

    public boolean getHasTakenOff() { return hasTakenOff; }

    public void setCurrentSpeed(float speed) {
        if (currentSpeed >= AIRCRAFT_SEEKING_MODE_SPEED && currentSpeed <= MAXIMUM_AIRCRAFT_SPEED)
            this.currentSpeed = speed;
        else
            this.currentSpeed = AIRCRAFT_SEEKING_MODE_SPEED;
    }
}