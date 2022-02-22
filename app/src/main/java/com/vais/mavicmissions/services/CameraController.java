package com.vais.mavicmissions.services;

import android.graphics.SurfaceTexture;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import dji.common.camera.CameraStreamSettings;
import dji.common.camera.SettingsDefinitions;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.products.Aircraft;

public class CameraController {
    private Aircraft aircraft;

    private Camera camera;
    private Gimbal gimbal;

    private boolean cameraReady;

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
    }

    public void destroy() {
        VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(null);
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
}