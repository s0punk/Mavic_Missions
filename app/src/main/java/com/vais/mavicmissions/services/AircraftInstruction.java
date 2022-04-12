package com.vais.mavicmissions.services;

import com.vais.mavicmissions.Enum.FlyInstruction;

public class AircraftInstruction {
    private static final int ANGLE_THRESHOLD = 25;

    private FlyInstruction instruction;
    private double angle;

    public AircraftInstruction(FlyInstruction instruction) {
        this.instruction = instruction;
    }

    public AircraftInstruction(FlyInstruction instruction, double angle) {
        this.instruction = instruction;

        if (instruction == FlyInstruction.GO_TOWARDS)
            this.angle = angle;
    }

    public boolean compare(AircraftInstruction otherInstruction) {
        if (instruction == otherInstruction.instruction) {
            if (instruction == FlyInstruction.GO_TOWARDS)
                return angle <= otherInstruction.angle + ANGLE_THRESHOLD && angle >= otherInstruction.angle - ANGLE_THRESHOLD;
            else return true;
        }
        else return false;
    }

    public FlyInstruction getInstruction() { return instruction; }
    public double getAngle() { return angle; }
}