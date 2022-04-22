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

import com.vais.mavicmissions.application.MavicMissionApp;
import com.vais.mavicmissions.objectives.FollowLine;
import com.vais.mavicmissions.services.AircraftController;
import com.vais.mavicmissions.services.CameraController;
import com.vais.mavicmissions.services.Detector;
import com.vais.mavicmissions.objectives.DynamicParkour;
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
    private MainActivity self;

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

    private Handler mHandler;

    private AircraftController controller;
    private CameraController cameraController;
    private VisionHelper visionHelper;

    private MavicMissionApp app;
    private DynamicParkour parkourManager;
    private FollowLine lineFollower;

    public Button btnDynamicParkour;
    public Button btnFollowLine;
    public Button btnBallRescue;

    public TextureView cameraSurface;
    public ImageView ivResult;

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
        self = this;
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
        self = this;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnDynamicParcour:
                if (!parkourManager.isDynamicParkourStarted())
                    parkourManager.startDynamicParkour();
                else {
                    setUIState(false);
                    showToast(getResources().getString(R.string.dynamicParourEnded));
                    parkourManager.setDynamicParkourStarted(false);
                    btnDynamicParkour.setText(getResources().getString(R.string.dynamicParcour));

                    // Arrêter le drone.
                    controller.land(() -> {
                        cameraController.lookDown();
                        setUIState(true);
                    });
                }
                break;
            case R.id.btnFollowLine:
                if (!lineFollower.isFollowLineStarted())
                    lineFollower.startFollowLine();
                else {
                    setUIState(false);
                    showToast(getResources().getString(R.string.followLineEnded));
                    lineFollower.setFollowLineStarted(false);
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

    public int getRightZoom() {
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

    public void showToast(final String toastMsg) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show());
    }

    public void setUIState(boolean state) {
        new Handler(Looper.getMainLooper()).post(() -> {
            btnDynamicParkour.setEnabled(state);
            btnFollowLine.setEnabled(state);
            btnBallRescue.setEnabled(state);
        });
    }

    public void setUIState(boolean state, Button exception) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (exception != btnDynamicParkour)
                btnDynamicParkour.setEnabled(state);
            if (exception != btnFollowLine)
                btnFollowLine.setEnabled(state);
            if (exception != btnBallRescue)
                btnBallRescue.setEnabled(state);
        });
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

                    parkourManager = new DynamicParkour(self, controller, cameraController, visionHelper);
                    lineFollower = new FollowLine(self, controller, cameraController, visionHelper);
                });
            }
        });
    }
}