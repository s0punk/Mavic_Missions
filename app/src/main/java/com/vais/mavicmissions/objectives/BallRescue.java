package com.vais.mavicmissions.objectives;

import android.os.Handler;
import com.vais.mavicmissions.MainActivity;
import com.vais.mavicmissions.R;
import com.vais.mavicmissions.services.drone.AircraftController;
import com.vais.mavicmissions.services.drone.CameraController;
import com.vais.mavicmissions.services.VisionHelper;
import org.opencv.core.Mat;

public class BallRescue extends Objectif {
    public BallRescue(MainActivity caller, AircraftController controller, CameraController cameraController, VisionHelper visionHelper) {
        super(caller, controller, cameraController, visionHelper);
    }

    public void startBallRescue() {
        setStopButton(caller.btnBallRescue);

        caller.showToast(caller.getResources().getString(R.string.ballRescueStart));

        startObjectif(djiError -> {
            // Commencer la recherche de la balle.
            search();
        });
    }

    private void search() {
        if (!objectifStarted)
            return;

        // Capturer le flux vidéo.
        Mat matSource = getFrame();

        // Isoler le jaune.
        Mat yellow = visionHelper.filterYellow(matSource);

        showFrame(yellow);

        new Handler().postDelayed(this::search, 500);
    }

    private void rescue() {

    }
}