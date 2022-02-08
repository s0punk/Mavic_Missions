package com.vais.mavicmissions.services;

public class AircraftController {
    private float pitch;
    private float roll;
    private float yaw;
    private float throttle;

    public float getPitch() { return pitch; }
    public void setPitch(float pitch) { this.pitch = pitch; }
    public float getRoll() { return roll; }
    public void setRoll(float roll) { this.roll = roll; }
    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }
    public float getThrottle() { return throttle; }
    public void setThrottle(float throttle) { this.throttle = throttle; }
}