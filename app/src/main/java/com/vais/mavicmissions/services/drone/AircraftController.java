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

/**
 * Simon-Olivier Vaillancourt
 * 2022-05-20
 * DJI Mavic 2 Entreprise
 * Classe qui gère les déplacements du drone.
 */
public class AircraftController {
    /**
     * Int, temps en ms attendu entre chaque commande envoyée au drone.
     */
    public static final int COMMAND_TIMEOUT = 5000;
    /**
     * Int, temps en ms attendu après le décollage du drone.
     */
    public static final int TAKEOFF_TIMEOUT = 7500;
    /**
     * Int, duré en ms minimum d'une commande.
     */
    public static final int MINIMUM_COMMAND_DURATION = 100;
    /**
     * Int, code qui indique q'une commande n'a pas de duré.
     */
    public static final int INFINITE_COMMAND = 0;
    /**
     * Int, vitesse maximum en m/s du drone.
     */
    public static final int MAXIMUM_AIRCRAFT_SPEED = 1;
    /**
     * Int, vitesse en m/s du drone lorsqu'il effectue l'objectif 1.
     */
    public static final float AIRCRAFT_SEEKING_MODE_SPEED = 0.5f;
    /**
     * Int, vitesse en m/s du drone lorsqu'il effectue l'objectif 2.
     */
    public static final float AIRCRAFT_FOLLOW_MODE_SPEED = 0.25f;
    /**
     * Int, vitesse en m/s maximum pour les déplacements verticales du drone.
     */
    public static final int MAXIMUM_VERTICAL_SPEED = 1;

    /**
     * Int, temps en ms attendu entre deux commandes.
     */
    private static final int COMMAND_RESET = 500;
    /**
     * Int, duré en ms d'une rotation du drone sur l'axe du yaw.
     */
    private static final int ROTATION_DURATION = 1500;

    /**
     * Int, angle qui représente l'avant du drone.
     */
    public static final int ROTATION_FRONT = 0;
    /**
     * Int, angle qui représente la gauche du drone.
     */
    public static final int ROTATION_LEFT = -90;
    /**
     * Int, angle qui représente la droite du drone.
     */
    public static final int ROTATION_RIGHT = 90;
    /**
     * Int, angle qui représente l'arrière du drone.
     */
    public static final int ROTATION_BACK = -180;

    /**
     * Aircraft, instance du drone.
     */
    private Aircraft aircraft;
    /**
     * FlightController, instance du contrôleur de vol du drone.
     */
    private FlightController flightController;

    /**
     * Boolean, indique si le drone est en vol.
     */
    private boolean hasTakenOff;
    /**
     * Boolean, indique si le contrôleur du drone est prêt.
     */
    private boolean controllerReady;

    /**
     * Float, vitesse en m/s actuelle du drone.
     */
    private float currentSpeed;

    /**
     * Float, valeur du pitch du drone.
     */
    public float pitch;
    /**
     * Float, valeur du roll du drone.
     */
    public float roll;
    /**
     * Float, valeur du yaw du drone.
     */
    public float yaw;
    /**
     * Float, valeur de la hauteur du drone.
     */
    public float throttle;

    /**
     * Boolean, indique si le drone utilise le mode vélocité pour ses déplacement, sinon, le mode angle.
     */
    private boolean velocityMode;

    /**
     * Classe qui gère l'envoie de commandes au drone.
     */
    private class SendVirtualStickDataTask extends TimerTask {
        /**
         * Méthode qui permet d'envoyer une commande au drone.
         */
        @Override
        public void run() {
            if (VerificationUnit.isFlightControllerAvailable()) {
                flightController.sendVirtualStickFlightControlData(new FlightControlData(pitch, roll, yaw, throttle), djiError -> { });
            }
        }
    }

    /**
     * Interface qui permet d'appller une méthode lorsque le contrôleur du drone est prêt.
     */
    public interface ControllerListener {
        void onControllerReady();
    }

    /**
     * Constructeur de la classe AircraftController, créé l'objet et initialise ses données membres.
     * @param aircraft Aircraft, instance du drone.
     * @param app MavicMissionApp, instance de l'application.
     * @param listener ControllerListener, méthode à appeler lorsque le drone est prêt.
     */
    public AircraftController(@NonNull Aircraft aircraft, @NonNull MavicMissionApp app, @Nullable ControllerListener listener) {
        controllerReady = false;
        hasTakenOff = false;

        // Si le SDK de l'application est enregistré.
        if (app.getRegistered()) {
            this.aircraft = aircraft;
            this.flightController = aircraft.getFlightController();

            // Activer les virtuals sticks.
            flightController.setVirtualStickModeEnabled(true,  djiError -> flightController.setVirtualStickAdvancedModeEnabled(true));
            new Handler().postDelayed(() -> {
                controllerReady = true;

                if (listener != null)
                    listener.onControllerReady();

            }, COMMAND_TIMEOUT);

            // Paramètrer le drone.
            setFlightControllerParams();
            velocityMode = true;
            resetAxis();
            setCurrentSpeed(MAXIMUM_AIRCRAFT_SPEED);
            yaw = flightController.getCompass().getHeading();
        }
    }

    /**
     * Méthode qui modifie certains paramètres du drone.
     */
    public void setFlightControllerParams() {
        flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
        flightController.setYawControlMode(YawControlMode.ANGLE);
        flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
        flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
        flightController.setFlightOrientationMode(FlightOrientationMode.AIRCRAFT_HEADING, null);
    }

    /**
     * Méthode qui vérifie l'état des virtuals sticks du drone.
     * @param listener ControllerListener, méthode à appeler lorsque le drone est prêt.
     */
    public void checkVirtualStick(ControllerListener listener) {
        // Vérifier les virtuals sticks.
        flightController.getVirtualStickModeEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
            @Override
            public void onSuccess(Boolean virtualStickEnabled) {
                // Si les virtuals sticks sont désactivés.
                if (!virtualStickEnabled)
                    // Activer les virtuals sticks.
                    flightController.setVirtualStickModeEnabled(true, djiError -> {
                        setFlightControllerParams();
                        listener.onControllerReady();
                    });
                else {
                    // Reparamètrer le drone.
                    setFlightControllerParams();
                    listener.onControllerReady();
                }
            }
            @Override
            public void onFailure(DJIError djiError) { }
        });
    }

    /**
     * Méthode qui permet de détruire l'instance de la classe.
     */
    public void destroy() {
        // Désactiver les virtuals sticks.
        flightController.setVirtualStickModeEnabled(false,  djiError -> flightController.setVirtualStickAdvancedModeEnabled(false));
        new Handler().postDelayed(() -> {
            controllerReady = false;
            DJISDKManager.getInstance().startConnectionToProduct();
        }, COMMAND_TIMEOUT);
    }

    /**
     * Méthode qui permet de réinitialiser les axes du drones, excepté le yaw.
     */
    public void resetAxis() {
        pitch = 0;
        roll = 0;
        throttle = 0;
    }

    /**
     * Fonction qui permet d'avoir l'altitude du drone.
     * @return Float, altitude du drone.
     */
    public float getHeight() {
        return flightController.getState().getAircraftLocation().getAltitude();
    }

    /**
     * Méthode qui permet de désactiver les virtuals sticks.
     */
    public void loseControl() {
        flightController.setVirtualStickModeEnabled(false, null);
    }

    /**
     * Méthode qui permet de décoller le drone.
     * @param listener ControllerListener, méthode à appeler lorsque le drone est prêt.
     */
    public void takeOff(@NonNull ControllerListener listener) {
        // Si le drone n'a pas déjà décollé.
        if (flightController != null && !hasTakenOff) {
            controllerReady = false;
            // Décoller le drone.
            flightController.startTakeoff(djiError ->
                    new Handler().postDelayed(() -> {
                        controllerReady = true;
                        hasTakenOff = true;
                        listener.onControllerReady();
                    }, TAKEOFF_TIMEOUT)
            );
        }
    }

    /**
     * Méthode qui permet d'attérir le drone.
     * @param listener ControllerListener, méthode à appeler lorsque le drone est prêt.
     */
    public void land(@NonNull ControllerListener listener) {
        // Si le drone est en vol.
        if (flightController != null && hasTakenOff) {
            resetAxis();

            controllerReady = false;

            // Commencer l'attérissage.
            flightController.startLanding(djiError -> {
                new Handler().postDelayed(() -> {
                    // Confirmer l'attérissage.
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

    /**
     * Méthode qui permet d'envoyer une commande au drone.
     */
    private void sendTask() {
        // Si le drone est prêt.
        if (flightController != null && controllerReady && hasTakenOff) {
            // Préparer la commande.
            SendVirtualStickDataTask task = new SendVirtualStickDataTask();
            Timer timer = new Timer();
            // Envoyer la commande.
            timer.schedule(task, 100, 200);
        }
    }

    /**
     * Méthode qui attend la duré d'une commande.
     * @param time Int, temps à attendre en ms.
     * @param listener ControllerListener, méthode à appeler lorsque le drone est prêt.
     */
    private void waitCommandDuration(int time, ControllerListener listener) {
        // Si le temps à attendre est valide.
        if (time >= MINIMUM_COMMAND_DURATION)
            // Exécuter l'action après le temps requis.
            new Handler().postDelayed(() -> {
                resetAxis();

                if (listener != null)
                    new Handler().postDelayed(listener::onControllerReady, COMMAND_RESET);
            }, time);
    }

    /**
     * Méthode qui attent la duré d'un déplacement verticale.
     * @param time Int, temps à attendre en ms.
     * @param listener ControllerListener, méthode à appeler lorsque le drone est prêt.
     */
    private void waitThrottleDuration(int time, ControllerListener listener) {
        // Si le temps à attendre est valide.
        if (time >= MINIMUM_COMMAND_DURATION)
            // Exécuter l'action après le temps requis.
            new Handler().postDelayed(() -> {
                throttle = 0;

                if (listener != null)
                    new Handler().postDelayed(listener::onControllerReady, COMMAND_RESET);
            }, time);
    }

    /**
     * Méthode qui permet de déplacer le drone vers le haut.
     * @param time Int, temps à attendre en ms.
     * @param listener ControllerListener, méthode à appeler lorsque le drone est prêt.
     */
    public void goUp(int time, ControllerListener listener) {
        // Envoyer la commande.
        throttle = MAXIMUM_VERTICAL_SPEED;
        sendTask();

        // Attendre la commande.
        time = time == INFINITE_COMMAND ? 500 : time;
        waitThrottleDuration(time, listener);
    }

    /**
     * Méthode qui permet de déplacer le drone vers le bas.
     * @param time Int, temps à attendre en ms.
     * @param listener ControllerListener, méthode à appeler lorsque le drone est prêt.
     */
    public void goDown(int time, ControllerListener listener) {
        // Envoyer la commande.
        throttle = -MAXIMUM_VERTICAL_SPEED;
        sendTask();

        // Attendre la commande.
        time = time == INFINITE_COMMAND ? 500 : time;
        waitThrottleDuration(time, listener);
    }

    /**
     * Méthode qui permet de déplacer le drone vers sa gauche.
     * @param time Int, temps à attendre en ms.
     * @param listener ControllerListener, méthode à appeler lorsque le drone est prêt.
     */
    public void goLeft(int time, ControllerListener listener) {
        // Envoyer la commande.
        resetAxis();
        pitch = velocityMode ? -currentSpeed : currentSpeed;

        // Attendre la commande.
        sendTask();
        waitCommandDuration(time, listener);
    }

    /**
     * Méthode qui permet de déplacer le drone vers sa droite.
     * @param time Int, temps à attendre en ms.
     * @param listener ControllerListener, méthode à appeler lorsque le drone est prêt.
     */
    public void goRight(int time, ControllerListener listener) {
        // Envoyer la commande.
        resetAxis();
        pitch = velocityMode ? currentSpeed : -currentSpeed;

        // Attendre la commande.
        sendTask();
        waitCommandDuration(time, listener);
    }

    /**
     * Méthode qui permet de déplacer le drone vers l'avant.
     * @param time Int, temps à attendre en ms.
     * @param listener ControllerListener, méthode à appeler lorsque le drone est prêt.
     */
    public void goForward(int time, ControllerListener listener) {
        // Envoyer la commande.
        resetAxis();
        roll = velocityMode ? currentSpeed : -currentSpeed;

        // Attendre la commande.
        sendTask();
        waitCommandDuration(time, listener);
    }

    /**
     * Méthode qui permet de déplacer le drone vers l'arrière.
     * @param time Int, temps à attendre en ms.
     * @param listener ControllerListener, méthode à appeler lorsque le drone est prêt.
     */
    public void goBack(int time, ControllerListener listener) {
        // Envoyer la commande.
        resetAxis();
        roll = velocityMode ? -currentSpeed : currentSpeed;

        // Attendre la commande.
        sendTask();
        waitCommandDuration(time, listener);
    }

    /**
     * Méthode qui permet de rotation le drone vers un angle précis.
     * @param angle Int, angle à faire face.
     * @param listener ControllerListener, méthode à appeler lorsque le drone est prêt.
     */
    public void faceAngle(int angle, ControllerListener listener) {
        // Calculer l'angle par rapport au nord.
        resetAxis();
        yaw = calculateRealAngle(angle);

        // Attendre la commande.
        sendTask();
        if (listener != null)
            new Handler().postDelayed(listener::onControllerReady, ROTATION_DURATION);
    }

    /**
     * Action qui permet de calculer un angle par rapport au nord.
     * @param desiredAngle Int, angle par rapport au drone.
     * @return Int, angle par rapport au nord.
     */
    public int calculateRealAngle(int desiredAngle) {
        int angle = 0;

        // Trouver l'angle actuelle du drone par rapport au nord.
        int droneAngle = (int)flightController.getCompass().getHeading();

        if (desiredAngle == ROTATION_FRONT)
            return droneAngle;

        // Convertir l'angle si elle dépasse les limites.
        angle = droneAngle + desiredAngle;
        if (angle > 180)
            angle = angle - 360;
        else if (angle < -180)
            angle = angle + 360;

        return angle;
    }

    /**
     * Méthode qui permet d'arrêter les mouvements du drone.
     * @param listener ControllerListener, méthode à appeler lorsque le drone est prêt.
     */
    public void stop(ControllerListener listener) {
        // Réinitialiser les axes du drone.
        resetAxis();
        sendTask();

        // Attendre la commande.
        if (listener != null)
            new Handler().postDelayed(listener::onControllerReady, COMMAND_RESET);
    }

    /**
     * Fonction qui permet d'obtenir l'instance du done.
     * @return Aircraft, instance du drone.
     */
    public Aircraft getAircraft() {
        return aircraft;
    }

    /**
     * Fonction qui permet d'obtenir le contrôleur de vol du drone.
     * @return FlightController, contrôleur de vol du drone.
     */
    public FlightController getFlightController() { return flightController; }

    /**
     * Fonction qui permet de savoir si le drone est en vol.
     * @return Boolean, vrai si le drone est en vol.
     */
    public boolean getHasTakenOff() { return hasTakenOff; }

    /**
     * Méthode qui permet de changer la vitesse actuelle du drone en m/s.
     * @param speed Float, nouvelle vitesse du drone en m/s.
     */
    public void setCurrentSpeed(float speed) {
        // Si la nouvelle vitesse est valide.
        if (currentSpeed >= AIRCRAFT_SEEKING_MODE_SPEED && currentSpeed <= MAXIMUM_AIRCRAFT_SPEED)
            this.currentSpeed = speed;
        else
            this.currentSpeed = AIRCRAFT_SEEKING_MODE_SPEED;
    }
}