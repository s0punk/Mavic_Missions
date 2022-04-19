package com.vais.mavicmissions;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.vais.mavicmissions.Enum.FlyInstruction;
import com.vais.mavicmissions.Enum.Shape;
import com.vais.mavicmissions.application.MavicMissionApp;
import com.vais.mavicmissions.services.AircraftController;
import com.vais.mavicmissions.services.AircraftInstruction;
import com.vais.mavicmissions.services.CameraController;
import com.vais.mavicmissions.services.Detector;
import com.vais.mavicmissions.services.VisionHelper;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.thirdparty.afinal.core.AsyncTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, TextureView.SurfaceTextureListener{
    private static final String TAG = "MainActivity";

    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";
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
    private List<String> missingPermission = new ArrayList<>();
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private static final int REQUEST_PERMISSION_CODE = 12345;

    private String parkourEnded;

    private Handler mHandler;

    private AircraftController controller;
    private CameraController cameraController;
    private VisionHelper visionHelper;

    private MavicMissionApp app;
    private AircraftInstruction lastInstruction;

    private boolean dynamicParkourStarted;
    private boolean followLineStarted;

    private Button btnDynamicParkour;
    private Button btnFollowLine;
    private Button btnBallRescue;

    private TextureView cameraSurface;
    private ImageView ivResult;

    private boolean textureAvailable;
    private SurfaceTexture texture;
    private int textureWidth;
    private int textureHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (MavicMissionApp)getApplication();
        checkAndRequestPermissions();

        setContentView(R.layout.activity_main);
        mHandler = new Handler(Looper.getMainLooper());

        btnDynamicParkour = findViewById(R.id.btnDynamicParcour);
        btnFollowLine = findViewById(R.id.btnFollowLine);
        btnBallRescue = findViewById(R.id.btnBallRescue);
        cameraSurface = findViewById(R.id.cameraPreviewSurface);
        ivResult = findViewById(R.id.iv_result);

        btnDynamicParkour.setOnClickListener(this);
        btnFollowLine.setOnClickListener(this);
        btnBallRescue.setOnClickListener(this);
        cameraSurface.setSurfaceTextureListener(this);

        visionHelper = new VisionHelper(this);

        parkourEnded = getResources().getString(R.string.dynamicParourEnded);
        dynamicParkourStarted = false;
        followLineStarted = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (controller != null)
            controller.destroy();

        if (cameraController != null)
            cameraController.destroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mHandler = new Handler(Looper.getMainLooper());
        checkAndRequestPermissions();

        if (cameraController != null) {
            cameraController.subscribeToVideoFeed();
            cameraController.setParameters();
            if (!cameraController.isLookingDown())
                cameraController.lookDown();
        }

        visionHelper.initCV();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnDynamicParcour:
                if (!dynamicParkourStarted)
                    startDynamicParcour();
                else {
                    showToast(getResources().getString(R.string.dynamicParourEnded));
                    dynamicParkourStarted = false;
                    btnDynamicParkour.setText(getResources().getString(R.string.dynamicParcour));

                    // Arrêter le drone.
                    controller.land(() -> {
                        cameraController.lookDown();
                        setUIState(true);
                    });
                }
                break;
            case R.id.btnFollowLine:
                if (!followLineStarted)
                    startFollowLine();
                else {
                    showToast(getResources().getString(R.string.followLineEnded));
                    followLineStarted = false;
                    btnFollowLine.setText(getResources().getString(R.string.followLine));

                    // Arrêter le drone.
                    controller.land(() -> {
                        cameraController.lookDown();
                        setUIState(true);
                    });
                }
                break;
            case R.id.btnBallRescue:
                setUIState(false);

                controller.checkVirtualStick(() -> {
                    if (controller.getHasTakenOff()) {
                        controller.land(() -> {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                setUIState(true);
                            });
                        });
                    }
                    else {
                        controller.takeOff(() -> {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                setUIState(true);
                            });
                        });
                    }
                });
                break;
        }
    }

    private int getRightZoom() {
        int zoom = CameraController.ZOOM_2X;
        float altitude = controller.getHeight();

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

    private void checkAndRequestPermissions() {
        for (String eachPermission : REQUIRED_PERMISSION_LIST)
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED)
                missingPermission.add(eachPermission);

        if (missingPermission.isEmpty())
            startSDKRegistration();
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }
    }

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

    private void startSDKRegistration() {
        String registering = getResources().getString(R.string.registering);
        String registerComplete = getResources().getString(R.string.registerComplete);
        String registerError = getResources().getString(R.string.registerError);

        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(() -> {
                showToast(registering);
                DJISDKManager.getInstance().registerApp(MainActivity.this.getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                    @Override
                    public void onRegister(DJIError djiError) {
                        if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
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

    private void onRegistered() {
        controller = new AircraftController(MavicMissionApp.getAircraftInstance(), app, new AircraftController.ControllerListener() {
            @Override
            public void onControllerReady() {
                new Handler(Looper.getMainLooper()).post(() -> {
                    cameraController = new CameraController(controller.getAircraft());
                    if (textureAvailable)
                        onSurfaceTextureAvailable(texture, textureWidth, textureHeight);

                    if (!cameraController.isLookingDown())
                        cameraController.lookDown();

                    cameraController.setZoom(CameraController.ZOOM_1X, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            setUIState(true);
                        }
                    });
                });
            }
        });
    }

    private void startFollowLine() {
        // Configurer le bouton d'arrêt du suivi.
        setUIState(false);
        btnFollowLine.setText(getResources().getString(R.string.stop));
        followLineStarted = true;
        btnFollowLine.setEnabled(true);

        controller.setCurrentSpeed(AircraftController.MAXIMUM_AIRCRAFT_SPEED);

        showToast(getResources().getString(R.string.followLineStart));

        // Vérifier l'état du drone.
        controller.checkVirtualStick(() -> {
            if (controller.getHasTakenOff()) {
                cameraController.setZoom(CameraController.ZOOM_1X, djiError -> {
                    // Commencer le suivi de la ligne.
                    seekGreenLine();
                });
            }
            else {
                // Décoller le drone.
                controller.takeOff(() -> {
                    cameraController.setZoom(CameraController.ZOOM_1X, djiError -> {
                        // Commencer le suivi de la ligne.
                        seekGreenLine();
                    });
                });
            }
        });
    }

    private void seekGreenLine() {
        if (!followLineStarted)
            return;

        // Capturer le flux vidéo.
        Bitmap source = cameraSurface.getBitmap();
        Mat matSource = visionHelper.bitmapToMap(source);

        // Isoler le vert.
        Mat green = visionHelper.filterGreen(matSource);

        // Détecter les coins.
        MatOfPoint corners = visionHelper.detectCorners(green, 25, 0.01f, 15);
        Point[] points = corners.toArray();

        for (Point p : points)
            Imgproc.circle(matSource, p, 2, new Scalar(255, 0, 0, 255), 10);

        // Afficher le résultat.
        new Handler(Looper.getMainLooper()).post(() -> ivResult.setImageBitmap(visionHelper.matToBitmap(matSource)));

        // Détecter la direction de la ligne.
        /*if (points.length > 2) {

        }*/

        // Continuer le suivi.
        new Handler().postDelayed(this::seekGreenLine, 500);
    }

    private void startDynamicParcour() {
        // Configurer le bouton d'arrêt du parcour.
        setUIState(false);
        btnDynamicParkour.setText(getResources().getString(R.string.stop));
        dynamicParkourStarted = true;
        btnDynamicParkour.setEnabled(true);

        controller.setCurrentSpeed(AircraftController.AIRCRAFT_SEEKING_MODE_SPEED);
        lastInstruction = null;

        showToast(getResources().getString(R.string.dynamicParcourStart));

        // Vérifier l'état du drone.
        controller.checkVirtualStick(() -> {
            if (controller.getHasTakenOff()) {
                // Attérir puis décoller le drone.
                controller.land(() -> {
                    cameraController.lookDown();
                    controller.takeOff(() -> {
                        cameraController.setZoom(getRightZoom(), new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                // Commencer la recherche de pancartes.
                                controller.goForward(1000, () -> {
                                    controller.setCurrentSpeed(AircraftController.AIRCRAFT_SEEKING_MODE_SPEED);
                                    seekInstructions();
                                });
                            }
                        });
                    });
                });
            }
            else {
                // Décoller le drone.
                controller.takeOff(() -> {
                    cameraController.setZoom(getRightZoom(), new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            // Commencer la recherche de pancartes.
                            controller.goForward(1000, () -> {
                                controller.setCurrentSpeed(AircraftController.AIRCRAFT_SEEKING_MODE_SPEED);
                                seekInstructions();
                            });
                        }
                    });
                });
            }
        });
    }

    private void seekInstructions() {
        Shape detectedShape;
        boolean seek = true;
        boolean stop = false;

        if (!dynamicParkourStarted)
            return;

        // Capturer le flux vidéo.
        Bitmap source = cameraSurface.getBitmap();
        Mat matSource = visionHelper.bitmapToMap(source);

        // Effectuer une détection de contours et isoler le plus gros.
        List<MatOfPoint> contours = visionHelper.contoursDetection(visionHelper.prepareContourDetection(matSource));
        MatOfPoint biggerContour = visionHelper.getBiggerContour(contours);

        // Détecter l'instruction.
        if (biggerContour != null) {
            detectedShape = Detector.detectShape(visionHelper.prepareContourDetection(matSource), visionHelper, biggerContour);

            // Exécuter l'action selon l'instruction.
            if (detectedShape == Shape.ARROW) {
                // Détecter les coins de la flèche.
                double angle = 0;
                Mat arr = visionHelper.prepareCornerDetection(matSource);
                MatOfPoint corners = visionHelper.detectCorners(arr, 3, 90);

                Mat arrow = Detector.detectArrow(arr, visionHelper, corners.toArray());
                if (arrow != null) {
                    Point[] croppedCorners = visionHelper.detectCorners(arrow, 3, 0.6f, 150).toArray();
                    Point head = Detector.findArrowHead(Detector.findCenterMass(arrow), croppedCorners);
                    angle = Detector.detectAngle(arrow, head);

                    // Afficher le résultat.
                    ivResult.setImageBitmap(visionHelper.matToBitmap(arrow));

                    if (lastInstruction == null)
                        lastInstruction = new AircraftInstruction(FlyInstruction.GO_TOWARDS, angle);
                    else if (new AircraftInstruction(FlyInstruction.GO_TOWARDS, angle).compare(lastInstruction)) {
                        seek = false;
                        executeInstruction(lastInstruction);
                        lastInstruction = null;
                    }
                    else {
                        lastInstruction = null;
                        stop = true;
                    }
                }
            }
            else if (detectedShape == Shape.U) {
                if (lastInstruction == null)
                    lastInstruction = new AircraftInstruction(FlyInstruction.GO_DOWN);
                else if (new AircraftInstruction(FlyInstruction.GO_UP).compare(lastInstruction)) {
                    seek = false;
                    executeInstruction(lastInstruction);
                    lastInstruction = null;
                }
                else {
                    lastInstruction = null;
                    stop = true;
                }
            }
            else if (detectedShape == Shape.D) {
                if (lastInstruction == null)
                    lastInstruction = new AircraftInstruction(FlyInstruction.GO_DOWN);
                else if (new AircraftInstruction(FlyInstruction.GO_DOWN).compare(lastInstruction)) {
                    seek = false;
                    executeInstruction(lastInstruction);
                    lastInstruction = null;
                }
                else {
                    lastInstruction = null;
                    stop = true;
                }
            }
            else if (detectedShape == Shape.H) {
                if (lastInstruction == null)
                    lastInstruction = new AircraftInstruction(FlyInstruction.TAKEOFF_LAND);
                else if (new AircraftInstruction(FlyInstruction.TAKEOFF_LAND).compare(lastInstruction)) {
                    seek = false;
                    executeInstruction(lastInstruction);
                    lastInstruction = null;
                    dynamicParkourStarted = false;
                }
                else {
                    lastInstruction = null;
                    stop = true;
                }
            }
        }

        // Continuer la recherche si rien n'a été trouvé.
        if (seek) {
            if (stop)
                controller.stop(null);
            else
                controller.goForward(2000, null);
            new Handler().postDelayed(this::seekInstructions, 500);
        }
    }

    private void executeInstruction(AircraftInstruction instruction) {
        controller.stop(() -> {
            if (instruction.getInstruction() == FlyInstruction.GO_TOWARDS) {
                controller.faceAngle((int)instruction.getAngle(), () -> {
                    controller.goForward(2000, null);
                    seekInstructions();
                });
            }
            else if (instruction.getInstruction() == FlyInstruction.GO_UP) {
                controller.stop(() -> {
                    controller.goUp(1000, () -> {
                        cameraController.setZoom(getRightZoom(), djiError -> {
                            controller.goForward(2000, null);
                            seekInstructions();
                        });
                    });
                });
            }
            else if (instruction.getInstruction() == FlyInstruction.GO_DOWN) {
                controller.stop(() -> {
                    controller.goDown(1000, () -> {
                        cameraController.setZoom(getRightZoom(), djiError -> {
                            controller.goForward(2000, null);
                            seekInstructions();
                        });
                    });
                });
            }
            else if (instruction.getInstruction() == FlyInstruction.TAKEOFF_LAND) {
                controller.land(() -> {
                    showToast(parkourEnded);
                    cameraController.lookDown();
                    setUIState(true);
                });
            }
        });
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int w, int h) {
        if (controller != null && cameraController != null) {
            if (cameraController.getCodecManager() == null)
                cameraController.setCodecManager(new DJICodecManager(this, surfaceTexture, w, h));
        }
        else {
            textureAvailable = true;
            texture = surfaceTexture;
            textureWidth = w;
            textureHeight = h;
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
        if (controller != null && cameraController != null)
            if (cameraController.getCodecManager() != null) {
                cameraController.getCodecManager().cleanSurface();
                cameraController.setCodecManager(null);
            }
        return false;
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int w, int h) { }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) { }

    private void notifyStatusChange() {
        if (mHandler != null) {
            mHandler.removeCallbacks(updateRunnable);
            mHandler.postDelayed(updateRunnable, 500);
        }
    }

    private Runnable updateRunnable = () -> {
        Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
        sendBroadcast(intent);
    };

    private void showToast(final String toastMsg) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show());
    }

    private void setUIState(boolean state) {
        new Handler(Looper.getMainLooper()).post(() -> {
            btnDynamicParkour.setEnabled(state);
            btnFollowLine.setEnabled(state);
            btnBallRescue.setEnabled(state);
        });
    }
}