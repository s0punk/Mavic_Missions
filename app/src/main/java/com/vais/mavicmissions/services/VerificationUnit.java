package com.vais.mavicmissions.services;

import androidx.annotation.Nullable;
import com.vais.mavicmissions.application.MavicMissionApp;
import dji.sdk.accessory.AccessoryAggregation;
import dji.sdk.accessory.beacon.Beacon;
import dji.sdk.accessory.speaker.Speaker;
import dji.sdk.accessory.spotlight.Spotlight;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.flightcontroller.Simulator;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;

/**
 * Classe qui permet de faire des vérifications auprès du drone et du SDK.
 * Source (Fichier au complet): DJI Developper.
 *
 */
public class VerificationUnit {
    /**
     * Fonction qui permet de déterminer si le produit est disponible.
     * @return Boolean, vrai si le produit est disponible.
     */
    public static boolean isProductModuleAvailable() {
        return (null != MavicMissionApp.getProductInstance());
    }

    /**
     * Fonction qui détermine si le produit est un drone.
     * @return Boolean, vrai si le produit est un drone.
     */
    public static boolean isAircraft() {
        return MavicMissionApp.getProductInstance() instanceof Aircraft;
    }

    /**
     * Fonction qui permet de déterminer si le flight controller du drone est disponible.
     * @return Boolean, vrai si le module est disponible.
     */
    public static boolean isFlightControllerAvailable() {
        return isProductModuleAvailable() && isAircraft() && (null != MavicMissionApp.getAircraftInstance()
                .getFlightController());
    }
}