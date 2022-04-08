package com.vais.mavicmissions.services;

import android.graphics.SurfaceTexture;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.common.camera.CameraStreamSettings;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.gimbal.Axis;
import dji.common.gimbal.ResetDirection;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.products.Aircraft;

public class CameraController {
    private static final float GIMBAL_DOWN_ANGLE = -90;

    private Aircraft aircraft;

    private Camera camera;
    private Gimbal gimbal;

    private boolean lookingDown;

    protected DJICodecManager codecManager;
    protected VideoFeeder.VideoDataListener videoReceiver;

    public CameraController(@NonNull Aircraft aircraft) {
        this.aircraft = aircraft;

        camera = aircraft.getCamera();
        gimbal = aircraft.getGimbal();

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

    public void setParameters() {
        camera.setISO(SettingsDefinitions.ISO.ISO_400, null);
        camera.setShutterSpeed(SettingsDefinitions.ShutterSpeed.SHUTTER_SPEED_1_100, null);
    }

    public void destroy() {
        VideoFeeder.getInstance().getPrimaryVideoFeed().removeVideoDataListener(videoReceiver);
        codecManager.cleanSurface();
        codecManager.destroyCodec();
    }

    public void lookForward() {
        Rotation.Builder builder = new Rotation.Builder();
        builder.mode(RotationMode.ABSOLUTE_ANGLE);
        builder.pitch(0);

        gimbal.rotate(builder.build(), DJIError -> lookingDown = false);
    }

    public void lookDown() {
        Rotation.Builder builder = new Rotation.Builder();
        builder.mode(RotationMode.ABSOLUTE_ANGLE);
        builder.pitch(GIMBAL_DOWN_ANGLE);

        gimbal.rotate(builder.build(), DJIError -> lookingDown = true);
    }

    public void subscribeToVideoFeed() {
        VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(videoReceiver);
    }

    public DJICodecManager getCodecManager() {
        return codecManager;
    }

    public void setCodecManager(DJICodecManager codecManager) {
        this.codecManager = codecManager;
    }

    public VideoFeeder.VideoDataListener getVideoReceiver() {
        return videoReceiver;
    }

    public void setVideoReceiver(VideoFeeder.VideoDataListener videoReceiver) {
        this.videoReceiver = videoReceiver;
    }

    public boolean isLookingDown() {
        return lookingDown;
    }
}