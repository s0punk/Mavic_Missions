package com.vais.mavicmissions.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import com.vais.mavicmissions.Enum.Color;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.android.Utils;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe qui permet d'effectuer des opérations de traitement d'image.
 */
public class VisionHelper {
    /**
     * Int, theshold de la détection de contours.
     */
    private static final int CONTOURS_THRESHOLD = 150;

    /**
     * Context, contexte de l'activité principale.
     */
    private Context context;
    /**
     * BaseLoaderCallback, callback appelé lorsque OpenCV est chargé.
     */
    private BaseLoaderCallback cvLoaderCallback;

    /**
     * Scaler, valeur de vert la plus basse acceptée.
     */
    private Scalar lowerGreen;
    /**
     * Scaler, valeur de vert la plus haute acceptée.
     */
    private Scalar upperGreen;

    /**
     * Scaler, valeur de jaune la plus basse acceptée.
     */
    private Scalar lowerYellow;
    /**
     * Scaler, valeur de jaune la plus basse acceptée.
     */
    private Scalar upperYellow;

    /**
     * Scaler, valeur de vert de la balle la plus basse acceptée.
     */
    private Scalar lowerBallGreen;
    /**
     * Scaler, valeur de vert de la balle la plus basse acceptée.
     */
    private Scalar upperBallGreen;

    /**
     * Scaler, valeur de noir la plus basse acceptée.
     */
    private Scalar lowerBlack;
    /**
     * Scaler, valeur de noir la plus basse acceptée.
     */
    private Scalar upperBlack;

    /**
     * Contructeur de la classe VisionHelper, créé l'objet et initialise ses données membres.
     * @param context Context, context de l'activité principale.
     */
    public VisionHelper(Context context) {
        this.context = context;

        // Charger le module d'OpenCV.
        cvLoaderCallback = new BaseLoaderCallback(context) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);
                if (status != LoaderCallbackInterface.SUCCESS)
                    super.onManagerConnected(status);
            }
        };

        // Définir les limites du vert.
        lowerGreen = new Scalar(32, 40, 40);
        upperGreen = new Scalar(82, 240, 240);

        // Définir les limites du jaune.
        lowerYellow = new Scalar(20, 100, 100);
        upperYellow = new Scalar(30, 255, 255);

        // Définir les limites du vert de la balle.
        lowerBallGreen = new Scalar(32, 100, 100);
        upperBallGreen = new Scalar(82, 255, 255);

        lowerBlack = new Scalar(0, 0, 0);
        upperBlack = new Scalar(145, 255, 30);
    }

    /**
     * Méthode qui permet de charger le module d'OpenCV.
     * Source: OpenCV.
     */
    public void initCV() {
        if (!OpenCVLoader.initDebug())
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, context, cvLoaderCallback);
        else
            cvLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    /**
     * Fonction qui permet de transformer une matrice en bitmap.
     * @param src Mat, matrice à transformer.
     * @return Bitmap, bitmap résultant.
     */
    public Bitmap matToBitmap(Mat src) {
        Bitmap result = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src, result);
        return result;
    }

    /**
     * Fonction qui permet de transformer un bitmap en matrice.
     * @param src Bitmap, bitmap à transformer.
     * @return Mat, matrice résultante.
     */
    public Mat bitmapToMap(Bitmap src) {
        Mat result = new Mat();
        Utils.bitmapToMat(src, result);
        return result;
    }

    /**
     * Fonction qui permet de transformer une matrice en nuances de gris.
     * @param src Mat, mat à transformer.
     * @return Mat, matrice résultante.
     */
    public Mat toGrayscale(Mat src) {
        Mat result = new Mat();
        Imgproc.cvtColor(src, result, Imgproc.COLOR_RGB2GRAY);

        return result;
    }

    /**
     * Fonction qui applique une convolution sur une matrice.
     * @param src Mat, matrice à transformer.
     * @param maskSize Int, dimensions du masque à appliquer.
     * @return Mat, matrice résultante.
     */
    public Mat smooth(Mat src, int maskSize) {
        Mat result = new Mat();
        Imgproc.GaussianBlur(src, result, new Size(maskSize, maskSize), 0, 0);

        return result;
    }

    /**
     * Fonction qui applique une dilattion sur une matrice.
     * @param src Mat, matrice à transformer.
     * @param maskSize Int, dimensions du masque à appliquer.
     * @return Mat, matrice résultante.
     */
    public Mat dilate(Mat src, int maskSize) {
        Mat result = new Mat();
        Imgproc.dilate(src, result, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(maskSize, maskSize)));

        return result;
    }

    /**
     * Fonction qui prépare une matrice pour effectuer une détection de contours.
     * @param src Mat, matrice à transformer.
     * @return Mat, matrice résultante.
     */
    public Mat prepareContourDetection(Mat src) {
        // Préparer l'image.
        src = smooth(src, 15);
        src = dilate(src, 5);
        return src;
    }

    /**
     * Fonction qui prépare une matrice pour effectuer une détection de coins.
     * @param src Mat, matrice à transformer.
     * @return Mat, matrice résultante.
     */
    public Mat prepareCornerDetection(Mat src) {
        Mat result = toGrayscale(src);
        result = smooth(result, 15);

        return result;
    }

    /**
     * Fonction qui effectue une détection de coins.
     * @param src Mat, matrice à analyzer.
     * @param maxCorner Int, nombre de coins maximum à trouver.
     * @param minDistance Int, distance maximum entre les coins.
     * @return MatOfPoint, matrice contenant les points trouvés.
     */
    public MatOfPoint detectCorners(Mat src, int maxCorner, int minDistance) {
        // Détecter les coins.
        MatOfPoint corners = new MatOfPoint();
        Imgproc.goodFeaturesToTrack(src, corners, maxCorner, 0.01, minDistance);

        return corners;
    }

    /**
     * Fonction qui effectue une détection de coins.
     * @param src Mat, matrice à analyzer.
     * @param maxCorner Int, nombre de coins maximum à trouver.
     * @param quality Int, qualité minimum des coins.
     * @param minDistance Int, distance maximum entre les coins.
     * @return MatOfPoint, matrice contenant les points trouvés.
     */
    public MatOfPoint detectCorners(Mat src, int maxCorner, float quality, int minDistance) {
        // Détecter les coins.
        MatOfPoint corners = new MatOfPoint();
        Imgproc.goodFeaturesToTrack(src, corners, maxCorner, quality, minDistance);

        return corners;
    }

    /**
     * Fonction qui effectue une détection de contours.
     * @param src Mat, matrice à analyzer.
     * @return List<MatOfPoint>, liste des contours trouvés.
     */
    public List<MatOfPoint> contoursDetection(Mat src) {
        // Trouver les contours.
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Mat binary = new Mat();
        Imgproc.threshold(src, binary, CONTOURS_THRESHOLD, CONTOURS_THRESHOLD, Imgproc.THRESH_BINARY_INV);
        Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        return contours;
    }

    /**
     * Fonction qui trouve le contours le plus grand d'une liste.
     * @param contours List<MatOfPoint>, liste de contours à analyzer.
     * @return MatOfPoint, contour le plus grand.
     */
    public MatOfPoint getBiggerContour(List<MatOfPoint> contours) {
        MatOfPoint biggerContour = null;
        int biggerRowCount = 0;

        // Parcourir les contours et noter le plus grand.
        for (MatOfPoint contour : contours) {
            if (contour.rows() > biggerRowCount) {
                biggerRowCount = contour.rows();
                biggerContour = contour;
            }
        }

        return biggerContour;
    }

    /**
     * Fonction qui trouve le contours le plus au centre de sa matrice parmis une liste.
     * @param source Mat, matrice à analyzer.
     * @param contours List<MatOfPoint>, liste des contours à analyzer.
     * @return MatOfPoint, contour le plus au centre de la matrice.
     */
    public MatOfPoint getCenteredContour(Mat source, List<MatOfPoint> contours) {
        MatOfPoint centeredContour = null;

        int targetPos = (int)(source.width() / 2);
        double smallestDifference = source.width();
        Point avg;

        // Parcourir les contours et noter le plus au centre.
        for (MatOfPoint contour : contours) {
            avg = Detector.getAveragePoint(contour.toArray());
            double difference = Math.abs(targetPos - avg.x);
            if (difference < smallestDifference) {
                centeredContour = contour;
                smallestDifference = difference;
            }
        }

        return centeredContour;
    }

    /**
     * Fonction qui filtre une matrice selon une couleur.
     * @param src Matrice à transformer.
     * @param color Color, couleur à filtrer.
     * @return Mat, masque résultant.
     */
    public Mat filterColor(Mat src, Color color) {
        Mat colorMask = new Mat();

        src = smooth(src, 3);

        // Transformer en HSV.
        Mat hsv = new Mat();
        Imgproc.cvtColor(src, hsv, Imgproc.COLOR_RGB2HSV);

        // Définir les limites de couleurs.
        Scalar lower = null, upper = null;

        if (color == Color.YELLOW) {
            lower = lowerYellow;
            upper = upperYellow;
        }
        else if (color == Color.LINE_GREEN) {
            lower = lowerGreen;
            upper = upperGreen;
        }
        else if (color == Color.BALL_GREEN) {
            lower = lowerBallGreen;
            upper = upperBallGreen;
        }
        else if (color == Color.BLACK) {
            lower = lowerBlack;
            upper = upperBlack;
        }
        else {
            lower = lowerBlack;
            upper = upperBlack;
        }

        Core.inRange(hsv, lower, upper, colorMask);

        return colorMask;
    }
}