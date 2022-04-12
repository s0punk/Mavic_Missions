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
import com.vais.mavicmissions.Enum.Shape;
import com.vais.mavicmissions.application.MavicMissionApp;
import com.vais.mavicmissions.services.AircraftController;
import com.vais.mavicmissions.services.CameraController;
import com.vais.mavicmissions.services.Detector;
import com.vais.mavicmissions.services.VisionHelper;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

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
    private boolean permissionsAccepted;

    private AircraftController controller;
    private CameraController cameraController;
    private VisionHelper visionHelper;

    private MavicMissionApp app;

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
        permissionsAccepted = false;

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

        if (!permissionsAccepted)
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
                startDynamicParcour();
                break;
            case R.id.btnFollowLine:
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
            case R.id.btnBallRescue:
                // Détecter la pancarte.
                Bitmap source = cameraSurface.getBitmap();
                Mat matSource = visionHelper.bitmapToMap(source);
                List<MatOfPoint> contours = visionHelper.contoursDetection(visionHelper.prepareContourDetection(matSource));

                MatOfPoint biggerContour = visionHelper.getBiggerContour(contours);
                if (biggerContour == null)
                    return;
                Bitmap output = cameraSurface.getBitmap();
                Shape detectedShape = Detector.detect(visionHelper.prepareContourDetection(matSource), visionHelper, biggerContour);

                if (detectedShape == Shape.ARROW) {
                    // Détecter le coins de la flèche.
                    Mat arr = visionHelper.prepareCornerDetection(matSource);
                    MatOfPoint corners = visionHelper.detectCorners(arr, 3, 70);

                    Mat arrow = Detector.detectArrow(arr, visionHelper, corners.toArray());
                    if (arrow != null) {
                        Point head = Detector.findArrowHead(Detector.findCenterMass(arrow), visionHelper.detectCorners(arrow, 3, 30).toArray());
                        double angle = Detector.detectAngle(arrow, head);

                        // TEMP.
                        output = visionHelper.matToBitmap(arrow);
                        showToast(angle + "");
                    }
                }
                else if (detectedShape == Shape.U) {
                    showToast("VA EN HAUT");
                }
                else if (detectedShape == Shape.D) {
                    showToast("VA EN BAS");
                }
                else if (detectedShape == Shape.H) {
                    showToast("ATTÉRIT/DÉCOLLE");
                }
                else {
                    showToast("NON-RECONNUE");
                }

                ivResult.setImageBitmap(output);
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
        permissionsAccepted = true;

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

    private void startDynamicParcour() {
        setUIState(false);
        controller.setCurrentSpeed(AircraftController.AIRCRAFT_SEEKING_MODE_SPEED);

        showToast(getResources().getString(R.string.dynamicParcourStart));

        // Vérifier l'état du drone.
        controller.checkVirtualStick(() -> {
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
        });
    }

    private void seekInstructions() {
        Shape detectedShape;
        boolean seek = true;

        // Capturer le flux vidéo.
        Bitmap source = cameraSurface.getBitmap();
        Mat matSource = visionHelper.bitmapToMap(source);

        // Effectuer une détection de contours et isoler le plus gros.
        List<MatOfPoint> contours = visionHelper.contoursDetection(visionHelper.prepareContourDetection(matSource));
        MatOfPoint biggerContour = visionHelper.getBiggerContour(contours);

        // Détecter l'instruction.
        if (biggerContour != null) {
            detectedShape = Detector.detect(visionHelper.prepareContourDetection(matSource), visionHelper, biggerContour);

            // Exécuter l'action selon l'instruction.
            if (detectedShape == Shape.ARROW) {
                seek = false;

                controller.stop(() -> {
                    // Détecter les coins de la flèche.
                    Mat arr = visionHelper.prepareCornerDetection(matSource);
                    MatOfPoint corners = visionHelper.detectCorners(arr, 3, 90);

                    // Détecter le sens de la flèche.
                    /*double angle = Detector.detectArrowDirection(arr, visionHelper, corners.toArray());
                    controller.faceAngle((int)angle, () -> {
                        controller.goForward(AircraftController.INFINITE_COMMAND, null);
                        seekInstructions();
                    });*/
                });
            }
            else if (detectedShape == Shape.U) {
                seek = false;
                controller.stop(() -> {
                    controller.goUp(1000, () -> {
                        controller.goForward(AircraftController.INFINITE_COMMAND, null);
                        seekInstructions();
                    });
                });
            }
            else if (detectedShape == Shape.D) {
                seek = false;
                controller.stop(() -> {
                    controller.goDown(1000, () -> {
                        controller.goForward(AircraftController.INFINITE_COMMAND, null);
                        seekInstructions();
                    });
                });
            }
            else if (detectedShape == Shape.H) {
                seek = false;
                controller.land(() -> {
                    showToast(parkourEnded);
                    cameraController.lookDown();
                    setUIState(true);
                });
            }
        }

        // Continuer la recherche si rien n'a été trouvé.
        if (seek) {
            controller.goForward(AircraftController.INFINITE_COMMAND, null);
            new Handler().postDelayed(this::seekInstructions, 500);
        }
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