package com.vais.mavicmissions;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import com.vais.mavicmissions.services.VerificationUnit;
import com.vais.mavicmissions.services.VisionHelper;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.android.Utils;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.util.CommonCallbacks;
import dji.internal.util.Util;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.BluetoothDevice;
import dji.sdk.sdkmanager.BluetoothProductConnector;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.sdkmanager.LDMManager;
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

    private Handler mHandler;

    private AircraftController controller;
    private CameraController cameraController;
    private VisionHelper visionHelper;

    private MavicMissionApp app;

    private Button btnStart;
    private Button btnLand;
    private Button btnCamera;
    private Button btnScreenshot;

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

        btnStart = findViewById(R.id.btnTest);
        btnLand = findViewById(R.id.btnTestStop);
        btnCamera = findViewById(R.id.btnCamera);
        btnScreenshot = findViewById(R.id.btnScreenshot);
        cameraSurface = findViewById(R.id.cameraPreviewSurface);
        ivResult = findViewById(R.id.iv_result);

        btnStart.setOnClickListener(this);
        btnLand.setOnClickListener(this);
        btnCamera.setOnClickListener(this);
        btnScreenshot.setOnClickListener(this);
        cameraSurface.setSurfaceTextureListener(this);

        visionHelper = new VisionHelper(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (controller != null)
            controller.destroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mHandler = new Handler(Looper.getMainLooper());
        checkAndRequestPermissions();

        if (cameraController != null)
            cameraController.subscribeToVideoFeed();

        visionHelper.initCV();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnTest:
                /*btnStart.setEnabled(false);
                btnLand.setEnabled(false);

                if (!controller.isCalibrated())
                    controller.calibrate(() -> doSquare());
                else
                    doSquare();*/
                break;
            case R.id.btnTestStop:
                /*btnStart.setEnabled(false);
                btnLand.setEnabled(false);

                if (!controller.isCalibrated())
                    controller.calibrate(() -> doSquareRotations());
                else
                    doSquareRotations();
                break;*/
            case R.id.btnCamera:
                if (cameraController.isLookingDown())
                    cameraController.lookForward();
                else
                    cameraController.lookDown();
                break;
            case R.id.btnScreenshot:
                // Détecter la pancarte.
                Bitmap source = cameraSurface.getBitmap();
                Mat matSource = visionHelper.bitmapToMap(source);
                List<MatOfPoint> contours = visionHelper.contoursDetection(visionHelper.prepareContourDetection(matSource));

                MatOfPoint biggerContour = visionHelper.getBiggerContour(contours);
                if (biggerContour == null)
                    return;
                Bitmap output = cameraSurface.getBitmap();
                Shape detectedShape = Detector.detect(biggerContour);

                if (detectedShape == Shape.ARROW) {
                    // Détecter le coins de la flèche.
                    Mat arr = visionHelper.prepareCornerDetection(matSource);
                    MatOfPoint corners = visionHelper.detectCorners(arr, 3, 90);

                    // Détecter le sens de la flèche.
                    double angle = Detector.detectArrowDirection(arr, visionHelper, corners.toArray());
                    showToast(angle + "");
                    output = visionHelper.matToBitmap(arr);
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
                    btnStart.setEnabled(true);
                    btnLand.setEnabled(true);
                    btnCamera.setEnabled(true);
                    btnScreenshot.setEnabled(true);

                    cameraController = new CameraController(controller.getAircraft());
                    if (textureAvailable)
                        onSurfaceTextureAvailable(texture, textureWidth, textureHeight);
                });
            }
        });
    }

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

    private void showToast(final String toastMsg, int length) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(getApplicationContext(), toastMsg, length).show());
    }

    private void doSquare() {
        showToast("Parcours carrée");
        controller.takeOff(() -> {
            controller.goForward(2000, () -> {
                controller.goRight(2000, () -> {
                    controller.goBack(2000, () -> {
                        controller.goLeft(2000, () -> {
                            controller.land(() -> {
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    btnStart.setEnabled(true);
                                    btnLand.setEnabled(true);
                                    btnCamera.setEnabled(true);
                                    btnScreenshot.setEnabled(true);
                                });
                                showToast("Fin du parcours carrée");
                            });
                        });
                    });
                });
            });
        });
    }

    private void doSquareRotations() {
        showToast("Parcours carrée avec rotation du drone");
        controller.takeOff(() -> {
            controller.goForward(2000, () -> {
                controller.faceRight(() -> {
                    controller.goForward(2000, () -> {
                        controller.faceBack(() -> {
                            controller.goForward(2000, () -> {
                                controller.faceLeft(() -> {
                                    controller.goForward(2000, () -> {
                                        controller.faceFront(() -> {
                                            controller.land(() -> {
                                                new Handler(Looper.getMainLooper()).post(() -> {
                                                    btnStart.setEnabled(true);
                                                    btnLand.setEnabled(true);
                                                    btnCamera.setEnabled(true);
                                                    btnScreenshot.setEnabled(true);
                                                });
                                                showToast("Fin du parcours carrée avec rotation du drone");
                                            });
                                        });
                                    });
                                });
                            });
                        });
                    });
                });
            });
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
}