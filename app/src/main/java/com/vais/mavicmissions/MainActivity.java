package com.vais.mavicmissions;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.vais.mavicmissions.application.MavicMissionApp;
import com.vais.mavicmissions.services.AircraftController;
import com.vais.mavicmissions.services.VerificationUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.thirdparty.afinal.core.AsyncTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
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
    private MavicMissionApp app;

    private Button btnStart;
    private Button btnLand;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (MavicMissionApp)getApplication();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkAndRequestPermissions();
        }

        setContentView(R.layout.activity_main);
        mHandler = new Handler(Looper.getMainLooper());

        btnStart = findViewById(R.id.btnTest);
        btnLand = findViewById(R.id.btnTestStop);

        btnStart.setOnClickListener(this);
        btnLand.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        controller.destroy();
        super.onDestroy();

    }

    @Override
    public void onClick(View view) {
        controller.setFlightController(VerificationUnit.getFlightController());
        switch (view.getId()) {
            case R.id.btnTest:
                controller.takeOff();
                controller.goForward(5);
                break;
            case R.id.btnTestStop:
                controller.land();
                break;
        }
    }

    private void checkAndRequestPermissions() {
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }

        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            showToast( getResources().getString(R.string.permissionNeeded));
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }

        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            showToast(getResources().getString(R.string.missingPermissions));
        }
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
                            showToast(registerComplete);
                            DJISDKManager.getInstance().startConnectionToProduct();
                            app.setRegistered(true);
                            controller = new AircraftController(MavicMissionApp.getAircraftInstance(), app, new AircraftController.AircraftListener() {
                                @Override
                                public void onControllerStateChanged(boolean controllerState) {
                                    btnStart.setEnabled(controllerState);
                                    btnLand.setEnabled(controllerState);
                                }
                            });
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
                    }

                    @Override
                    public void onProductChanged(BaseProduct baseProduct) { }

                    @Override
                    public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent, BaseComponent newComponent) {
                        if (newComponent != null) {
                            newComponent.setComponentListener(isConnected -> notifyStatusChange());
                        }
                    }
                    @Override
                    public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) { }

                    @Override
                    public void onDatabaseDownloadProgress(long l, long l1) { }
                });
            });
        }
    }

    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }

    private Runnable updateRunnable = () -> {
        Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
        sendBroadcast(intent);
    };

    private void showToast(final String toastMsg) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show());
    }
}