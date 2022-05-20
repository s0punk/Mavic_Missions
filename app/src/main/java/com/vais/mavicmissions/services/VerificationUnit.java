package com.vais.mavicmissions.services;

import com.vais.mavicmissions.application.MavicMissionApp;
import dji.sdk.products.Aircraft;

/**
 * Simon-Olivier Vaillancourt
 * 2022-05-20
 * DJI Mavic 2 Entreprise
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