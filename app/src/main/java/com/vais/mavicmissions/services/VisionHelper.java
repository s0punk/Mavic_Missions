package com.vais.mavicmissions.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import com.vais.mavicmissions.Enum.Color;
import com.vais.mavicmissions.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.android.Utils;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
        lowerYellow = new Scalar(22, 100, 100);
        upperYellow = new Scalar(28, 255, 255);

        // Définir les limites du vert de la balle.
        lowerBallGreen = new Scalar(32, 100, 100);
        upperBallGreen = new Scalar(82, 255, 255);

        lowerBlack = new Scalar(0, 0, 0);
        upperBlack = new Scalar(155, 255, 35);
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
     * Fonction qui applique une érosion sur une matrice.
     * @param src Mat, matrice à transformer.
     * @param maskSize Int, dimension du masque à appliquer.
     * @return Mat, matrice résultante.
     */
    public Mat erode(Mat src, int maskSize) {
        Mat result = new Mat();
        Imgproc.erode(src, result, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(maskSize, maskSize)));

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
     * Source: https://www.tutorialspoint.com/opencv/opencv_canny_edge_detection.htm
     * @param src Mat, matrice à transformer.
     * @return Mat, matrice résultante.
     */
    public Mat prepareContourDetection(Mat src) {
        // Préparer l'image.
        src = toGrayscale(src);
        src = smooth(src, 25);

        Imgproc.Canny(src, src, 60, 60 * 3);
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
        Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);

        return contours;
    }

    /**
     * Fonction qui trouve le contours le plus grand d'une liste.
     * @param contours List<MatOfPoint>, liste de contours à analyzer.
     * @return MatOfPoint, contour le plus grand.
     */
    public MatOfPoint getBiggerContour(Mat source, List<MatOfPoint> contours) {
        MatOfPoint biggerContour = null;
        double biggestArea = 0;
        double area;
        Rect bounds;

        // Parcourir les contours et noter le plus grand.
        for (MatOfPoint contour : contours) {
            bounds = Imgproc.boundingRect(contour);
            area = Imgproc.contourArea(contour);

            if (area > biggestArea && (bounds.width < source.width() && bounds.height < source.height())) {
                biggestArea  = area;
                biggerContour = contour;
            }
        }

        return biggerContour;
    }

    /**
     * Source: https://stackoverflow.com/questions/44501723/how-to-merge-contours-in-opencv
     * @param source
     * @param contours
     * @return
     */
    public MatOfPoint combineContours(Mat source, List<MatOfPoint> contours) {
        List<Point> finalPoints = new ArrayList<>();
        MatOfPoint finalContour = new MatOfPoint();
        Rect bounds;

        for (MatOfPoint c : contours) {
            bounds = Imgproc.boundingRect(c);

            // Si le contour n'est pas le contour de la matrice au complet.
            if (bounds.width < source.width() && bounds.height < source.height())
                finalPoints.addAll(c.toList());
        }

        finalContour.fromList(finalPoints);
        return finalContour;
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

    /**
     * Fonction qui rétressit une matrice par rapport à un ROI.
     * @param source Mat, matrice à transformer.
     * @param contour MatOfPoint, contour à délimiter.
     * @return Mat, matrice résultante.
     */
    public Mat cropToContour(Mat source, MatOfPoint contour) {
        Rect bounds = Imgproc.boundingRect(contour);
        return new Mat(source, bounds);
    }

    /**
     * Fonction qui affiche des contours sur un fond noir.
     * @param source Mat, matrice à transformer.
     * @param contours List<MatOfPoint>, liste des contours à afficher.
     * @return Mat, matrice résultante.
     */
    public Mat drawContour(Mat source, List<MatOfPoint> contours) {
        // Remplir le fond de la matrice.
        List<Point> points = new ArrayList<>();
        points.add(new Point(0, 0));
        points.add(new Point(source.width(), 0));
        points.add(new Point(source.width(), source.height()));
        points.add(new Point(0, source.height()));
        MatOfPoint collection = new MatOfPoint();
        collection.fromList(points);
        List<MatOfPoint> pointMats = new ArrayList<>();
        pointMats.add(collection);

        Imgproc.fillPoly(source, pointMats, new Scalar(0, 0, 0, 255));
        Random r = new Random();

        for (int i = 0; i < contours.size(); i++)
            Imgproc.drawContours(source, contours, i, new Scalar(r.nextInt(255), r.nextInt(255), r.nextInt(255), 255), 5);

        return source;
    }

    /**
     * Fonction qui affiche des contours sur un fond noir.
     * @param source Mat, matrice à transformer.
     * @param contour MatOfPoint, contour à afficher.
     * @return Mat, matrice résultante.
     */
    public Mat drawContour(Mat source, MatOfPoint contour) {
        // Remplir le fond de la matrice.
        List<Point> points = new ArrayList<>();
        points.add(new Point(0, 0));
        points.add(new Point(source.width(), 0));
        points.add(new Point(source.width(), source.height()));
        points.add(new Point(0, source.height()));
        MatOfPoint collection = new MatOfPoint();
        collection.fromList(points);
        List<MatOfPoint> pointMats = new ArrayList<>();
        pointMats.add(collection);

        Imgproc.fillPoly(source, pointMats, new Scalar(0, 0, 0, 255));
        Random r = new Random();

        List<MatOfPoint> contours = new ArrayList<>();
        contours.add(contour);
        for (int i = 0; i < contours.size(); i++)
            Imgproc.drawContours(source, contours, i, new Scalar(r.nextInt(255), r.nextInt(255), r.nextInt(255), 255), 5);

        return source;
    }

    /**
     * Fonction qui permet de comparer une matrice à un patron.
     * Source: https://www.tabnine.com/code/java/methods/org.opencv.imgproc.Imgproc/matchTemplate
     * @param src Mat, matrice à analyzer.
     * @param templateRes Int, ID de la ressource du patron.
     * @return Double, valeur de comparaison maximum détectée.
     */
    public double matchTemplate(Mat src, int templateRes) {
        // Convertir la ressource en matrice.
        Drawable tSource = ContextCompat.getDrawable(context, templateRes);
        Mat template = bitmapToMap(((BitmapDrawable)tSource).getBitmap());
        template = toGrayscale(template);
        src = toGrayscale(src);

        Mat result = new Mat();
        Imgproc.matchTemplate(src, template, result, Imgproc.TM_CCORR);
        Core.MinMaxLocResult locResult = Core.minMaxLoc(result);

        return locResult.maxVal;
    }

    /**
     * Fonction qui permet de comparer des contours.
     * @param srcContour MatOfPoint, contour à analyzer.
     * @param templateRes Int, ID de la ressource du patron.
     * @return Double, valeur de comparaison des formes.
     */
    public double matchShape(MatOfPoint srcContour, int templateRes) {
        // Convertir la ressource en matrice.
        Drawable tSource = ContextCompat.getDrawable(context, templateRes);
        Mat template = bitmapToMap(((BitmapDrawable)tSource).getBitmap());

        // Prendre le contour du template.
        Mat filteredTemplate = prepareContourDetection(template);
        List<MatOfPoint> templateContours = contoursDetection(filteredTemplate);
        MatOfPoint templateContour;

        if (templateRes == R.mipmap.ic_d_foreground)
            templateContour = templateContours.get(3);
        else if (templateRes == R.mipmap.ic_u_foreground)
            templateContour = templateContours.get(3);
        else
            templateContour = null;

        return Imgproc.matchShapes(templateContour, srcContour, Imgproc.TM_CCORR, 0);
    }
}