package com.vais.mavicmissions.objectives;

import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import com.vais.mavicmissions.MainActivity;
import com.vais.mavicmissions.R;
import com.vais.mavicmissions.services.drone.AircraftController;
import com.vais.mavicmissions.services.drone.CameraController;
import com.vais.mavicmissions.services.VisionHelper;
import org.opencv.core.Mat;

import dji.common.util.CommonCallbacks.CompletionCallback;

/**
 * Classe qui gère un objectif.
 */
public abstract class Objectif {
    /**
     * MainActivity, instance de l'activité de l'application.
     */
    public MainActivity caller;
    /**
     * AircraftController, controlleur du drone.
     */
    protected AircraftController controller;
    /**
     * CameraController, controlleur de la caméra du drone.
     */
    protected CameraController cameraController;
    /**
     * VisionHelper, service de traitement d'image.
     */
    protected VisionHelper visionHelper;

    /**
     * Boolean, indique si l'objectif est démarré.
     */
    protected boolean objectifStarted;

    /**
     * Constructeur de la classe Objectif, créé l'objet et initialise ses données membres.
     * @param caller MainActivity, instance de l'activité de l'application.
     * @param controller AircraftController, controlleur du drone.
     * @param cameraController CameraController, controlleur de la caméra du drone.
     * @param visionHelper VisionHelper, service de traitement d'image.
     */
    public Objectif(MainActivity caller, AircraftController controller, CameraController cameraController, VisionHelper visionHelper) {
        this.caller = caller;
        this.controller = controller;
        this.cameraController = cameraController;
        this.visionHelper = visionHelper;

        objectifStarted = false;
    }

    /**
     * Méthode qui paramétrise le bouton d'arrête de l'objectif.
     * @param button Button, bouton d'arrête de l'objectif.
     */
    protected void setStopButton(Button button) {
        // Configurer le bouton d'arrêt de l'objectif.
        caller.setUIState(false, button);
        button.setText(caller.getResources().getString(R.string.stop));
        objectifStarted = true;
    }

    /**
     * Méthode qui démarre l'objectif.
     * @param onReady CommonCallbacks.CompletionCallback, callback à appeler lorsque l'objectif est prêt.
     */
    protected void startObjectif(CompletionCallback onReady) {
        // Vérifier l'état du drone.
        controller.checkVirtualStick(() -> {
            cameraController.lookDown();
            // Décoller le drone.
            controller.takeOff(() -> cameraController.setZoom(getRightZoom(), djiError -> onReady.onResult(null)));
        });
    }

    /**
     * Fonction qui permet d'obtenir un frame du flux vidéo.
     * @return Mat, matrice du flux vidéo.
     */
    public Mat getFrame() {
        return visionHelper.bitmapToMap(caller.cameraSurface.getBitmap());
    }

    /**
     * Fonction qui donne le zoom approprié selon l'altitude du drone.
     * @return Int, zoom à donner au drone.
     */
    public int getRightZoom() {
        int zoom = CameraController.ZOOM_2X;
        float altitude = controller.getHeight();

        // Selon l'altitude du drone.
        if (altitude >= 3)
            zoom = CameraController.ZOOM_6X;
        else if (altitude >= 2)
            zoom = CameraController.ZOOM_4_2X;
        else if (altitude >= 1)
            zoom = CameraController.ZOOM_2_2X;
        else if (altitude < 1)
            zoom = CameraController.ZOOM_1_6X;

        return zoom;
    }

    /**
     * Méthode qui affiche une matrice dans l'activité de l'application.
     * @param frame Mat, matrice à afficher.
     */
    public void showFrame(Mat frame) {
        new Handler(Looper.getMainLooper()).post(() -> caller.ivResult.setImageBitmap(visionHelper.matToBitmap(frame)));
    }

    /**
     * Fonction qui indique si l'objectif est démarré.
     * @return Boolea, vrai si l'objectif est démarré.
     */
    public boolean isObjectifOver() {
        return !objectifStarted;
    }

    /**
     * Méthode qui permet de définir si l'objectif est démarré.
     * @param objectifStarted Boolean, vrai si l'objectif est démarré.
     */
    public void setObjectifStarted(boolean objectifStarted) {
        this.objectifStarted = objectifStarted;
    }
}