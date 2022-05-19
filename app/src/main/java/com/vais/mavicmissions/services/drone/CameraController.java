package com.vais.mavicmissions.services.drone;

import android.os.Handler;
import androidx.annotation.NonNull;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.products.Aircraft;

/**
 * Classe qui gère la caméra du drone.
 */
public class CameraController {
    /**
     * Float, angle qui pointe le gimbal vers le bas.
     */
    public static final float GIMBAL_DOWN_ANGLE = -90;
    /**
     * Int, valeur minimum du zoom optique de la caméra.
     */
    public static final int MIN_OPTICAL_ZOOM = 240;
    /**
     * Int, valeur maximum du zoom optique de la caméra.
     */
    public static final int MAX_OPTICAL_ZOOM = 1440;

    /**
     * Int, temps en ms à attendre après le zoom de la caméra.
     */
    private static final int ZOOM_OPERATION_DELAY = 2500;

    /**
     * Int, valeur qui représente un zoom 1X.
     */
    public static final int ZOOM_1X = MIN_OPTICAL_ZOOM;
    /**
     * Int, valeur qui représente un zoom 1.6X.
     */
    public static final int ZOOM_1_6X = (int)(MIN_OPTICAL_ZOOM * 1.6);
    /**
     * Int, valeur qui représente un zoom 2X.
     */
    public static final int ZOOM_2X = MIN_OPTICAL_ZOOM * 2;
    /**
     * Int, valeur qui représente un zoom 2.2X.
     */
    public static final int ZOOM_2_2X = (int)(MIN_OPTICAL_ZOOM * 2.2);
    /**
     * Int, valeur qui représente un zoom 3X.
     */
    public static final int ZOOM_3X = MIN_OPTICAL_ZOOM * 3;
    /**
     * Int, valeur qui représente un zoom 4X.
     */
    public static final int ZOOM_4X = MIN_OPTICAL_ZOOM * 4;
    /**
     * Int, valeur qui représente un zoom 4.2X.
     */
    public static final int ZOOM_4_2X = (int)(MIN_OPTICAL_ZOOM * 4.2);
    /**
     * Int, valeur qui représente un zoom 5X.
     */
    public static final int ZOOM_5X = MIN_OPTICAL_ZOOM * 5;
    /**
     * Int, valeur qui représente un zoom 6X.
     */
    public static final int ZOOM_6X = MIN_OPTICAL_ZOOM * 6;

    /**
     * Camera, instance de la caméra du drone.
     */
    private final Camera camera;
    /**
     * Gimbal, instance du gimbal de la caméra.
     */
    private final Gimbal gimbal;

    /**
     * Boolean, indique si le drone regarde vers le bas.
     */
    private boolean lookingDown;

    /**
     * DJICodecManager, gestionnaire du flux vidéo.
     */
    protected DJICodecManager codecManager;
    /**
     * VideoDataListener, objet qui recoit le flux vidéo de la caméra.
     */
    protected VideoFeeder.VideoDataListener videoReceiver;

    /**
     * Constructeur de la classe CameraController, créé l'objet et initialise ses données membres.
     * @param aircraft Aircraft, instance du drone.
     */
    public CameraController(@NonNull Aircraft aircraft) {
        // Récupérer le drone et le gimbal.
        camera = aircraft.getCamera();
        gimbal = aircraft.getGimbal();

        // Paramétrer le flux vidéo.
        codecManager = null;
        videoReceiver = new VideoFeeder.VideoDataListener() {
            @Override
            public void onReceive(byte[] bytes, int size) {
                if (codecManager != null)
                    codecManager.sendDataToDecoder(bytes, size);
            }
        };

        setParameters();
        lookDown();
    }

    /**
     * Méthode qui paramétrise la caméra.
     */
    public void setParameters() {
        // Définir l'ISO et la vitesse de la caméra.
        camera.setISO(SettingsDefinitions.ISO.ISO_400, null);
        camera.setShutterSpeed(SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_100, null);
    }

    /**
     * Méthode qui change le zoom optique de la caméra.
     * @param zoom Int, nouveau zoom de la caméra.
     * @param callback CompletionCallback, action à effectuer lorsque le zoom est complété.
     */
    public void setZoom(final int zoom, CommonCallbacks.CompletionCallback callback) {
        // Obtenir les informations du zoom optique.
        camera.getOpticalZoomSpec(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.OpticalZoomSpec>() {
            @Override
            public void onSuccess(SettingsDefinitions.OpticalZoomSpec opticalZoomSpec) {
                // Optenir les minimum et maximum du zoom optique.
                int minZoom = opticalZoomSpec.getMinFocalLength();
                int maxZoom = opticalZoomSpec.getMaxFocalLength();

                // Vérifier que le zoom demandé soit valide avant de l'appliquer.
                camera.setOpticalZoomFocalLength(zoom >= minZoom && zoom <= maxZoom && (zoom % opticalZoomSpec.getFocalLengthStep() == 0) ? zoom : minZoom, null);

                // Attendre la fin du zoom et exécuter le callback.
                new Handler().postDelayed(() -> callback.onResult(null), ZOOM_OPERATION_DELAY);
            }
            @Override
            public void onFailure(DJIError djiError) { }
        });
    }

    /**
     * Méthode qui permet de détruire l'instance de la classe.
     */
    public void destroy() {
        // Libérer le flux vidéo.
        VideoFeeder.getInstance().getPrimaryVideoFeed().removeVideoDataListener(videoReceiver);
        codecManager.cleanSurface();
        codecManager.destroyCodec();
    }

    /**
     * Méthode qui permet au drone de regarder vers l'avant.
     */
    public void lookForward() {
        // Changer la rotation du gimbal.
        Rotation.Builder builder = new Rotation.Builder();
        builder.mode(RotationMode.ABSOLUTE_ANGLE);
        builder.pitch(0);

        gimbal.rotate(builder.build(), DJIError -> lookingDown = false);
    }

    /**
     * Méthode qui permet au drone de regarder vers un angle donné.
     * @param angle Int, angle vers lequelle le drone doit regarder.
     */
    public void lookAtAngle(int angle) {
        // Rotation le gimbal.
        Rotation.Builder builder = new Rotation.Builder();
        builder.mode(RotationMode.ABSOLUTE_ANGLE);
        builder.pitch(angle);

        gimbal.rotate(builder.build(), null);
    }

    /**
     * Méthode qui permet au drone de regarder vers le bas.
     */
    public void lookDown() {
        // Rotation le gimbal.
        Rotation.Builder builder = new Rotation.Builder();
        builder.mode(RotationMode.ABSOLUTE_ANGLE);
        builder.pitch(GIMBAL_DOWN_ANGLE);

        gimbal.rotate(builder.build(), DJIError -> lookingDown = true);
    }

    /**
     * Méthode qui permet de recevoir le flux vidéo de la caméra.
     */
    public void subscribeToVideoFeed() {
        VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(videoReceiver);
    }

    /**
     * Fonction qui permet d'obtenir le gestionnaire du flux vidéo.
     * @return DJICodecManager, gestionnaire du flux vidéo.
     */
    public DJICodecManager getCodecManager() {
        return codecManager;
    }

    /**
     * Méthode qui permet de changer le gestionnaire du flux vidéo.
     * @param codecManager DJICodecManager, nouveau gestionnaire du flux vidéo.
     */
    public void setCodecManager(DJICodecManager codecManager) {
        this.codecManager = codecManager;
    }

    /**
     * Fonction qui indique si le drone regarde vers le bas.
     * @return Boolean, vrai si le drone regarde vers le bas.
     */
    public boolean isLookingDown() {
        return lookingDown;
    }
}