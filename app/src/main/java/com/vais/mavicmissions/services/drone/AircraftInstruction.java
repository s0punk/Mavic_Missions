package com.vais.mavicmissions.services.drone;

import com.vais.mavicmissions.Enum.FlyInstruction;

/**
 * Simon-Olivier Vaillancourt
 * 2022-05-20
 * DJI Mavic 2 Entreprise
 * Classe qui représente une instruction à envpyer au drone.
 */
public class AircraftInstruction {
    /**
     * Int, valeur du jeu d'angle autorisé.
     */
    private static final int ANGLE_THRESHOLD = 25;

    /**
     * FlyInstruction, instruction de vol.
     */
    private FlyInstruction instruction;
    /**
     * Double, angle de rotation à effectuer.
     */
    private double angle;

    /**
     * Constructeur de la classe AircraftInstruction, créé l'objet et initialise ses données membres.
     * @param instruction FlyInstruction, instruction de vol.
     */
    public AircraftInstruction(FlyInstruction instruction) {
        this.instruction = instruction;
    }

    /**
     * Constructeur de la classe AircraftInstruction, créé l'objet et initialise ses données membres.
     * @param instruction FlyInstruction, instruction de vol.
     * @param angle Double, angle de rotation.
     */
    public AircraftInstruction(FlyInstruction instruction, double angle) {
        this.instruction = instruction;

        if (instruction == FlyInstruction.GO_TOWARDS)
            this.angle = angle;
    }

    /**
     * Fonction qui compare deux instructions de vol.
     * @param otherInstruction AircraftInstruction, instruction à comparer.
     * @return Boolean, vrai si les deux instructions sont pareil.
     */
    public boolean compare(AircraftInstruction otherInstruction) {
        if (instruction == otherInstruction.instruction) {
            if (instruction == FlyInstruction.GO_TOWARDS)
                return angle <= otherInstruction.angle + ANGLE_THRESHOLD && angle >= otherInstruction.angle - ANGLE_THRESHOLD;
            else return true;
        }
        else return false;
    }

    /**
     * Fonction qui retourne l'instruction de vol.
     * @return FlyInstruction, instruction de vol.
     */
    public FlyInstruction getInstruction() { return instruction; }

    /**
     * Fonction qui retourne l'angle de rotation.
     * @return Double, angle de rotation.
     */
    public double getAngle() { return angle; }
}