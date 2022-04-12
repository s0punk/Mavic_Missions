package com.vais.mavicmissions.services;

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

public class CameraController {
    private static final float GIMBAL_DOWN_ANGLE = -90;
    private static final int MIN_OPTICAL_ZOOM = 240;
    private static final int MAX_OPTICAL_ZOOM = 1440;

    public static final int ZOOM_1X = MIN_OPTICAL_ZOOM;
    public static final int ZOOM_1_6X = (int)(MIN_OPTICAL_ZOOM * 1.6);
    public static final int ZOOM_2X = MIN_OPTICAL_ZOOM * 2;
    public static final int ZOOM_2_2X = (int)(MIN_OPTICAL_ZOOM * 2.2);
    public static final int ZOOM_3X = MIN_OPTICAL_ZOOM * 3;
    public static final int ZOOM_4X = MIN_OPTICAL_ZOOM * 4;
    public static final int ZOOM_4_2X = (int)(MIN_OPTICAL_ZOOM * 4.2);
    public static final int ZOOM_5X = MIN_OPTICAL_ZOOM * 5;
    public static final int ZOOM_6X = MIN_OPTICAL_ZOOM * 6;

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

    public void setZoom(final int zoom, CommonCallbacks.CompletionCallback callback) {
        camera.getOpticalZoomSpec(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.OpticalZoomSpec>() {
            @Override
            public void onSuccess(SettingsDefinitions.OpticalZoomSpec opticalZoomSpec) {
                int minZoom = opticalZoomSpec.getMinFocalLength();
                int maxZoom = opticalZoomSpec.getMaxFocalLength();
                camera.setOpticalZoomFocalLength(zoom >= minZoom && zoom <= maxZoom && (zoom % opticalZoomSpec.getFocalLengthStep() == 0) ? zoom : minZoom, callback);
            }
            @Override
            public void onFailure(DJIError djiError) { }
        });
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