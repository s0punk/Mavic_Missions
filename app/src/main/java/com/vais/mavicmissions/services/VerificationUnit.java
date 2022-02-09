package com.vais.mavicmissions.services;

import androidx.annotation.Nullable;

import com.vais.mavicmissions.application.MavicMissionApp;

import dji.common.product.Model;
import dji.sdk.accessory.AccessoryAggregation;
import dji.sdk.accessory.beacon.Beacon;
import dji.sdk.accessory.speaker.Speaker;
import dji.sdk.accessory.spotlight.Spotlight;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.flightcontroller.Simulator;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;

public class VerificationUnit {
    public static boolean isProductModuleAvailable() {
        return (null != MavicMissionApp.getProductInstance());
    }

    public static boolean isAircraft() {
        return MavicMissionApp.getProductInstance() instanceof Aircraft;
    }

    public static boolean isHandHeld() {
        return MavicMissionApp.getProductInstance() instanceof HandHeld;
    }

    public static boolean isCameraModuleAvailable() {
        return isProductModuleAvailable() && (null != MavicMissionApp.getProductInstance().getCamera());
    }

    public static boolean isPlaybackAvailable() {
        return isCameraModuleAvailable() && (null != MavicMissionApp.getProductInstance()
                .getCamera()
                .getPlaybackManager());
    }

    public static boolean isMediaManagerAvailable() {
        return isCameraModuleAvailable() && (null != MavicMissionApp.getProductInstance()
                .getCamera()
                .getMediaManager());
    }

    public static boolean isRemoteControllerAvailable() {
        return isProductModuleAvailable() && isAircraft() && (null != MavicMissionApp.getAircraftInstance()
                .getRemoteController());
    }

    public static boolean isFlightControllerAvailable() {
        return isProductModuleAvailable() && isAircraft() && (null != MavicMissionApp.getAircraftInstance()
                .getFlightController());
    }

    public static boolean isCompassAvailable() {
        return isFlightControllerAvailable() && isAircraft() && (null != MavicMissionApp.getAircraftInstance()
                .getFlightController()
                .getCompass());
    }

    public static boolean isFlightLimitationAvailable() {
        return isFlightControllerAvailable() && isAircraft();
    }

    public static boolean isGimbalModuleAvailable() {
        return isProductModuleAvailable() && (null != MavicMissionApp.getProductInstance().getGimbal());
    }

    public static boolean isAirlinkAvailable() {
        return isProductModuleAvailable() && (null != MavicMissionApp.getProductInstance().getAirLink());
    }

    public static boolean isWiFiLinkAvailable() {
        return isAirlinkAvailable() && (null != MavicMissionApp.getProductInstance().getAirLink().getWiFiLink());
    }

    public static boolean isLightbridgeLinkAvailable() {
        return isAirlinkAvailable() && (null != MavicMissionApp.getProductInstance()
                .getAirLink()
                .getLightbridgeLink());
    }

    public static boolean isOcuSyncLinkAvailable() {
        return isAirlinkAvailable() && (null != MavicMissionApp.getProductInstance()
                .getAirLink()
                .getOcuSyncLink());
    }

    public static boolean isPayloadAvailable() {
        return isProductModuleAvailable() && isAircraft() && (null != MavicMissionApp.getAircraftInstance()
                .getPayload());
    }

    public static boolean isRTKAvailable() {
        return isProductModuleAvailable() && isAircraft() && (null != MavicMissionApp.getAircraftInstance()
                .getFlightController().getRTK());
    }

    public static AccessoryAggregation getAccessoryAggregation() {
        Aircraft aircraft = (Aircraft) MavicMissionApp.getProductInstance();

        if (aircraft != null && null != aircraft.getAccessoryAggregation()) {
            return aircraft.getAccessoryAggregation();
        }
        return null;
    }

    public static Speaker getSpeaker() {
        Aircraft aircraft = (Aircraft) MavicMissionApp.getProductInstance();

        if (aircraft != null && null != aircraft.getAccessoryAggregation() && null != aircraft.getAccessoryAggregation().getSpeaker()) {
            return aircraft.getAccessoryAggregation().getSpeaker();
        }
        return null;
    }

    public static Beacon getBeacon() {
        Aircraft aircraft = (Aircraft) MavicMissionApp.getProductInstance();

        if (aircraft != null && null != aircraft.getAccessoryAggregation() && null != aircraft.getAccessoryAggregation().getBeacon()) {
            return aircraft.getAccessoryAggregation().getBeacon();
        }
        return null;
    }

    public static Spotlight getSpotlight() {
        Aircraft aircraft = (Aircraft) MavicMissionApp.getProductInstance();

        if (aircraft != null && null != aircraft.getAccessoryAggregation() && null != aircraft.getAccessoryAggregation().getSpotlight()) {
            return aircraft.getAccessoryAggregation().getSpotlight();
        }
        return null;
    }

    @Nullable
    public static Simulator getSimulator() {
        Aircraft aircraft = MavicMissionApp.getAircraftInstance();
        if (aircraft != null) {
            FlightController flightController = aircraft.getFlightController();
            if (flightController != null) {
                return flightController.getSimulator();
            }
        }
        return null;
    }

    @Nullable
    public static FlightController getFlightController() {
        Aircraft aircraft = MavicMissionApp.getAircraftInstance();
        if (aircraft != null) {
            return aircraft.getFlightController();
        }
        return null;
    }
}