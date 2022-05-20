package com.vais.mavicmissions;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import com.vais.mavicmissions.application.MavicMissionApp;
import com.vais.mavicmissions.objectives.BallRescue;
import com.vais.mavicmissions.objectives.FollowLine;
import com.vais.mavicmissions.services.drone.AircraftController;
import com.vais.mavicmissions.services.drone.CameraController;
import com.vais.mavicmissions.objectives.DynamicParkour;
import com.vais.mavicmissions.services.VisionHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.thirdparty.afinal.core.AsyncTask;

/**
 * Simon-Olivier Vaillancourt
 * 2022-05-20
 * DJI Mavic 2 Entreprise
 * Classe qui représente l'activité principale de l'application. Elle permet de démarrer les objectifs, d'observer leurs résultats et de visionner le flux vidéo du drone.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener, TextureView.SurfaceTextureListener {
    /**
     * MainActivity, instance de l'activité.
     */
    private MainActivity self;

    /**
     * String, flag de changement de connexion au produit.
     * Source: DJI Developper.
     */
    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";
    /**
     * String[], liste des permissions nécessaire à l'application.
     * Source: DJI Developper.
     */
    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };
    /**
     * List<String>, liste des permissions manquantes.
     * Source: DJI Developper.
     */
    private final List<String> missingPermission = new ArrayList<>();
    /**
     * AtomicBoolean, indique si le processus d'authentification du SDK est en cours.
     * Source: DJI Developper.
     */
    private final AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    /**
     * Int, code de demande de permission.
     * Source: DJI Developper.
     */
    private static final int REQUEST_PERMISSION_CODE = 12345;

    /**
     * Handler, gestionnaire de thread.
     */
    private Handler mHandler;

    /**
     * AircraftController, controlleur du drone.
     */
    private AircraftController controller;
    /**
     * CameraController, controlleur de la caméra du drone.
     */
    private CameraController cameraController;
    /**
     * VisionHelper, service de traitement d'image.
     */
    private VisionHelper visionHelper;

    /**
     * MavicMissionApp, gestionnaire de l'application.
     */
    private MavicMissionApp app;

    /**
     * DynamicParkour, gestionnaire du parcours dynamique.
     */
    private DynamicParkour parkourManager;
    /**
     * FollowLine, gestionnaire du suivi de ligne.
     */
    private FollowLine lineFollower;
    /**
     * BallRescue, gestionnaire du sauvetage de balle.
     */
    private BallRescue ballRescuer;

    /**
     * TableRow, conteneur des boutons de l'application.
     */
    private TableRow tr_buttons;
    /**
     * LinearLayout, conteneur du flux vidéo.
     */
    private LinearLayout ll_feed;
    /**
     * TextView, texte d'erreur.
     */
    private TextView tv_error;
    /**
     * Button, bouton de tentative de connexion.
     */
    private Button btn_retryConnection;

    /**
     * Button, bouton associé au parcours dynamique.
     */
    public Button btnDynamicParkour;
    /**
     * Button, bouton associé au suivi de ligne.
     */
    public Button btnFollowLine;
    /**
     * Button, bouton associé au sauvetage d'une balle.
     */
    public Button btnBallRescue;

    /**
     * TextureView, surface du flux vidéo.
     */
    public TextureView cameraSurface;
    /**
     * ImageView, conteneur de l'images.
     */
    public ImageView ivResult;

    /**
     * Boolean, indique si le conteneur du flux vidéo est disponible.
     */
    private boolean textureAvailable;
    /**
     * SurfaceTexture, texture à donner au conteneur du flux vidéo.
     */
    private SurfaceTexture texture;
    /**
     * Int, largeur du flux vidéo.
     */
    private int textureWidth;
    /**
     * Int, hauteur du flux vidéo.
     */
    private int textureHeight;

    /**
     * Méthode appelée à la création de l'activité.
     * @param savedInstanceState Bundle, conteneur des informations sauvegardées de l'activité.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Vérifier les permissions de l'application.
        app = (MavicMissionApp)getApplication();
        checkAndRequestPermissions();

        // Obtenir le layout.
        setContentView(R.layout.activity_main);
        mHandler = new Handler(Looper.getMainLooper());

        // Instancier les éléments de l'affichage nécessitant une interaction.
        tr_buttons = findViewById(R.id.tr_buttons);
        ll_feed = findViewById(R.id.ll_feed);
        tv_error = findViewById(R.id.tv_error);
        btn_retryConnection = findViewById(R.id.btn_retryConnection);

        btnDynamicParkour = findViewById(R.id.btnDynamicParcour);
        btnFollowLine = findViewById(R.id.btnFollowLine);
        btnBallRescue = findViewById(R.id.btnBallRescue);
        cameraSurface = findViewById(R.id.cameraPreviewSurface);
        ivResult = findViewById(R.id.iv_result);

        btnDynamicParkour.setOnClickListener(this);
        btnFollowLine.setOnClickListener(this);
        btnBallRescue.setOnClickListener(this);
        cameraSurface.setSurfaceTextureListener(this);
        btn_retryConnection.setOnClickListener(this);

        // Instancier le module de traitement d'image.
        visionHelper = new VisionHelper(this);
        self = this;
    }

    /**
     * Méthode appelée à la destruction de l'activité.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Détruire le controlleur du drone.
        if (controller != null)
            controller.destroy();

        // Détruire le controlleur de la caméra du drone.
        if (cameraController != null)
            cameraController.destroy();
    }

    /**
     * Méthode appelée lors du retour vers l'activité.
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Vérifier les permissions.
        mHandler = new Handler(Looper.getMainLooper());
        checkAndRequestPermissions();

        // Paramétrer l'accès au flux vidéo.
        if (cameraController != null) {
            cameraController.subscribeToVideoFeed();
            cameraController.setParameters();
            if (!cameraController.isLookingDown())
                cameraController.lookDown();
        }

        // Instancier le module de traitement d'image.
        visionHelper.initCV();
        self = this;
    }

    /**
     * Méthode appelée lors d'un clique sur l'activité.
     * @param view View, vue ayant été cliquée.
     */
    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        // Selon la vue qui a été cliquée.
        switch (view.getId()) {
            case R.id.btnDynamicParcour:
                // Démarrer le parcours dynamique.
                if (parkourManager.isObjectifOver())
                    parkourManager.startDynamicParkour();
                else {
                    setUIState(false);
                    showToast(getResources().getString(R.string.dynamicParourEnded));
                    parkourManager.setObjectifStarted(false);
                    btnDynamicParkour.setText(getResources().getString(R.string.dynamicParcour));

                    if (controller.getHasTakenOff())
                        quickLand();
                    else
                        setUIState(true);
                }
                break;
            case R.id.btnFollowLine:
                // Démarrer le suivi d'une ligne.
                if (lineFollower.isObjectifOver())
                    lineFollower.startFollowLine();
                else {
                    setUIState(false);
                    showToast(getResources().getString(R.string.followLineEnded));
                    lineFollower.setObjectifStarted(false);
                    btnFollowLine.setText(getResources().getString(R.string.followLine));

                    if (controller.getHasTakenOff())
                        quickLand();
                    else
                        setUIState(true);
                }
                break;
            case R.id.btnBallRescue:
                // Démarrer le sauvetage d'une balle.
                if (ballRescuer.isObjectifOver())
                    ballRescuer.startBallRescue();
                else {
                    setUIState(false);
                    showToast(getResources().getString(R.string.ballRescueEnded));
                    ballRescuer.setObjectifStarted(false);
                    btnBallRescue.setText(getResources().getString(R.string.ballRescue));

                    if (controller.getHasTakenOff())
                        quickLand();
                    else
                        setUIState(true);
                }
                break;
            case R.id.btn_retryConnection:
                // Source: https://stackoverflow.com/questions/6609414/how-do-i-programmatically-restart-an-android-app

                // Déclarer un intent d'ouverture de l'application.
                Intent mStartActivity = new Intent(this, MainActivity.class);

                int mPendingIntentId = 123456;
                PendingIntent mPendingIntent = PendingIntent.getActivity(this, mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);

                // Activer l'intent dans 100 millisecondes.
                AlarmManager mgr = (AlarmManager)this.getSystemService(ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                System.exit(0);
                break;
        }
    }

    /**
     * Méthode qui permet d'attérir le drone après un objectif.
     */
    public void quickLand() {
        // Arrêter le drone.
        controller.land(() -> {
            // Regarder la caméra vers le bas.
            cameraController.lookDown();

            // Désactiver les virtuals sticks.
            controller.loseControl();
            setUIState(true);
        });
    }

    /**
     * Méthode appelée lorsque le conteneur du flux vidéo est disponible.
     * @param surfaceTexture SurfaceTexture, texture à donner au conteneur du flux vidéo.
     * @param w Int, largeur du flux.
     * @param h Int, hauteur du flux.
     */
    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int w, int h) {
        // Si le drone est dispnible.
        if (controller != null && cameraController != null) {
            // Instancier le manageur de codec du drone.
            if (cameraController.getCodecManager() == null)
                cameraController.setCodecManager(new DJICodecManager(this, surfaceTexture, w, h));
        }
        // Si le controlleur du drone n'est pas disponible.
        else {
            // Sauvegarder les informations.
            textureAvailable = true;
            texture = surfaceTexture;
            textureWidth = w;
            textureHeight = h;
        }
    }

    /**
     * Méthode appelée lorsque le conteneur du  flux vidéo.
     * @param surfaceTexture SurfaceTexture, texture à donner au conteneur du flux vidéo.
     * @return Boolean, faux.
     */
    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
        // Détruire le codec du drone.
        if (controller != null && cameraController != null)
            if (cameraController.getCodecManager() != null) {
                cameraController.getCodecManager().cleanSurface();
                cameraController.setCodecManager(null);
            }
        return false;
    }

    /**
     * Méthode appelée lorsque la grandeur du flux change.
     * @param surfaceTexture SurfaceTexture, texture à donner au conteneur du flux vidéo.
     * @param w Int, largeur du flux.
     * @param h Int, hauteur du flux.
     */
    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int w, int h) { }

    /**
     * Méthode appelée lorsque le flux est mis à jour.
     * @param surfaceTexture SurfaceTexture, texture à donner au conteneur du flux vidéo.
     */
    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) { }

    /**
     * Méthode qui notifie si la connection au produit change.
     * Source: DJI Developper.
     */
    private void notifyStatusChange() {
        if (mHandler != null) {
            mHandler.removeCallbacks(updateRunnable);
            mHandler.postDelayed(updateRunnable, 500);
        }
    }

    /**
     * Fonction qui met à jour l'intent de connection au produit.
     * Source: DJI Developper.
     */
    private final Runnable updateRunnable = () -> {
        Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
        sendBroadcast(intent);
    };

    /**
     * Méthode qui affiche un message toast.
     * Source: DJI Developper.
     * @param toastMsg String, message à afficher.
     */
    public void showToast(final String toastMsg) {
        // Afficher le message dans le thread d'affichage.
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show());
    }

    /**
     * Méthode qui change l'état des composants.
     * @param state Boolean, faux si les composants doivent être désactivé.
     */
    public void setUIState(boolean state) {
        // Faire les changements dans le thread d'affichage.
        new Handler(Looper.getMainLooper()).post(() -> {
            btnDynamicParkour.setEnabled(state);
            btnFollowLine.setEnabled(state);
            btnBallRescue.setEnabled(state);
        });
    }

    /**
     * Méthode qui change l'état des composants.
     * @param state Boolean, faux si les composants doivent être désactivé.
     * @param exception Button, bouton qui ne changera pas d'état.
     */
    public void setUIState(boolean state, Button exception) {
        // Faire les changements dans le thread d'affichage.
        new Handler(Looper.getMainLooper()).post(() -> {
            if (exception != btnDynamicParkour)
                btnDynamicParkour.setEnabled(state);
            if (exception != btnFollowLine)
                btnFollowLine.setEnabled(state);
            if (exception != btnBallRescue)
                btnBallRescue.setEnabled(state);
        });
    }

    /**
     * Méthode qui permet de vérifier les permissions de l'application.
     * Source: DJI Developper.
     */
    private void checkAndRequestPermissions() {
        // Vérfier chaque permissions requise.
        for (String eachPermission : REQUIRED_PERMISSION_LIST)
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED)
                missingPermission.add(eachPermission);

        if (missingPermission.isEmpty())
            startSDKRegistration();
        else {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[0]),
                    REQUEST_PERMISSION_CODE);
        }
    }

    /**
     * Méthode appelée lorsque le résultat des permissions est reçues.
     * Source: DJI Developper.
     * @param requestCode Int, code de requête.
     * @param permissions String[], permissions octroyées.
     * @param grantResults int[], résultats obtenus.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_CODE)
            for (int i = grantResults.length - 1; i >= 0; i--)
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED)
                    missingPermission.remove(permissions[i]);

        if (missingPermission.isEmpty())
            startSDKRegistration();
    }

    /**
     * Méthode qui commence le processus d'authentification du SDK.
     * Source: DJI Developper.
     */
    private void startSDKRegistration() {
        // Charger les messages à afficher pendant le processus.
        String registering = getResources().getString(R.string.registering);
        String registerComplete = getResources().getString(R.string.registerComplete);
        String registerError = getResources().getString(R.string.registerError);

        // Vérifier si le processus est déjà commencé.
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(() -> {
                showToast(registering);
                DJISDKManager.getInstance().registerApp(MainActivity.this.getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                    @Override
                    public void onRegister(DJIError djiError) {
                        if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                            // Commencer la connexion au produit.
                            app.setRegistered(true);
                            DJISDKManager.getInstance().startConnectionToProduct();
                            showToast(registerComplete);
                        } else {
                            showToast(registerError);
                            app.setRegistered(false);
                        }
                    }

                    @Override
                    public void onProductDisconnect() {
                        notifyStatusChange();
                    }

                    @Override
                    public void onProductConnect(BaseProduct baseProduct) {
                        notifyStatusChange();

                        // Commencer le processus d'après-connexion.
                        onRegistered();
                    }

                    @Override
                    public void onProductChanged(BaseProduct baseProduct) { }
                    @Override
                    public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent, BaseComponent newComponent) {
                        if (newComponent != null) newComponent.setComponentListener(isConnected -> notifyStatusChange());
                    }
                    @Override
                    public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) { }
                    @Override
                    public void onDatabaseDownloadProgress(long l, long l1) { }
                });
            });
        }
    }

    /**
     * Méthode qui démarre le processus d'après-connexion.
     */
    private void onRegistered() {
        Aircraft aircraft = MavicMissionApp.getAircraftInstance();

        // Si le flight controller du drone n'est pas disponible.
        if (aircraft.getFlightController() == null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                // Afficher un message d'erreur.
                tr_buttons.setVisibility(View.GONE);
                ll_feed.setVisibility(View.GONE);
                tv_error.setVisibility(View.VISIBLE);
                btn_retryConnection.setVisibility(View.VISIBLE);
            });
            return;
        }

        // Instancier le controlleur du drone.
        controller = new AircraftController(aircraft, app, () -> new Handler(Looper.getMainLooper()).post(() -> {
            // Instancier le controlleur de caméra.
            cameraController = new CameraController(controller.getAircraft());
            if (textureAvailable)
                onSurfaceTextureAvailable(texture, textureWidth, textureHeight);

            if (!cameraController.isLookingDown())
                cameraController.lookDown();

            cameraController.setZoom(CameraController.ZOOM_1X, djiError -> setUIState(true));

            // Instacier les gestionnaires des objectifs.
            parkourManager = new DynamicParkour(self, controller, cameraController, visionHelper);
            lineFollower = new FollowLine(self, controller, cameraController, visionHelper);
            ballRescuer = new BallRescue(self, controller, cameraController, visionHelper);
        }));
    }
}