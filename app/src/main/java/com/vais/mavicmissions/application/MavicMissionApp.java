package com.vais.mavicmissions.application;

import android.app.Application;
import android.content.Context;
import com.secneo.sdk.Helper;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * Classe qui gère l'application.
 */
public class MavicMissionApp extends Application {
    /**
     * BaseProduct, produit auquel est connectée la manette.
     */
    private static BaseProduct product;
    /**
     * Application, instance de l'application.
     */
    private static Application app = null;

    /**
     * Boolean, indique que l'application est authentifié auprès de DJI.
     */
    private Boolean registered = false;

    /**
     * Méthode qui permet d'installer le SDK dans l'application.
     * Source: DJI Developper.
     * @param paramContext Context, contexte de l'application.
     */
    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(MavicMissionApp.this);
    }

    /**
     * Fonction qui permet d'obtenir l'instance du produit connecté.
     * Source: DJI Developper.
     * @return BaseProduct, produit connecté.
     */
    public static synchronized BaseProduct getProductInstance() {
        product = DJISDKManager.getInstance().getProduct();
        return product;
    }

    /**
     * Fonction qui permet de vérifier si le drone est connecté.
     * Source: DJI Developper.
     * @return Boolean, indique si le drone est connecté.
     */
    public static boolean isAircraftConnected() {
        return getProductInstance() != null && getProductInstance() instanceof Aircraft;
    }

    /**
     * Fonction qui permet de vérifier si le produit en main est connecté.
     * Source: DJI Developper.
     * @return Boolean, permet de vérifier si le produit en main est connecté.
     */
    public static boolean isHandHeldConnected() {
        return getProductInstance() != null && getProductInstance() instanceof HandHeld;
    }

    /**
     * Fonction qui permet d'obtenir l'instance du drone.
     * Source: DJI Developper.
     * @return Aircraft, instance du drone connecté.
     */
    public static synchronized Aircraft getAircraftInstance() {
        if (!isAircraftConnected()) {
            return null;
        }
        return (Aircraft) getProductInstance();
    }

    /**
     * Fonction qui permet de savoir si l'application est authentifiée auprès de DJI.
     * @return Boolean, indique si l'application est authentifiée auprès de DJI.
     */
    public Boolean getRegistered() { return registered; }

    /**
     * Méthode qui permet de dire si l'application est authentifiée auprès de DJI.
     * @param registered Boolean, indique si l'application est authentifiée auprès de DJI.
     */
    public void setRegistered(Boolean registered) { this.registered = registered; }
}
