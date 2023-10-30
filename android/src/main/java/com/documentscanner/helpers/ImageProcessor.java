package com.documentscanner.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.shapes.PathShape;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.os.Bundle;

import com.facebook.react.bridge.Arguments;

import com.documentscanner.views.ScannerView;
import com.documentscanner.views.HUDCanvasView;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.oned.ITFReader;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class ImageProcessor extends Handler {
    private static final String TAG = "ImageProcessor";
    private final ScannerView mMainActivity;
    private boolean mBugRotate;
    private double colorGain = 1; // contrast
    private double colorBias = 10; // bright
    private Size mPreviewSize;
    private Point[] mPreviewPoints;
    private int numOfSquares = 0;
    private int numOfRectangles = 10;
    private double lastCaptureTime = 0;
    private double durationBetweenCaptures = 0;
    private String currentBarcode = null;
    private String currentBarcodeWest = null;
    private int numOfBarcodes = 0;
    private String rotate = null;

    public ImageProcessor(Looper looper, ScannerView mainActivity, Context context) {
        super(looper);
        this.mMainActivity = mainActivity;
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        mBugRotate = sharedPref.getBoolean("bug_rotate", false);
    }

    public void setNumOfRectangles(int numOfRectangles) {
        this.numOfRectangles = numOfRectangles;
    }

    public void setDurationBetweenCaptures(double durationBetweenCaptures) {
        this.durationBetweenCaptures = durationBetweenCaptures;
    }

    public void setBrightness(double brightness) {
        this.colorBias = brightness;
    }

    public void setContrast(double contrast) {
        this.colorGain = contrast;
    }


    public void handleMessage(Message msg) {
        if (msg.obj.getClass() == ImageProcessorMessage.class) {

            ImageProcessorMessage obj = (ImageProcessorMessage) msg.obj;
            String command = obj.getCommand();

            Log.d(TAG, "Message Received: " + command + " - " + obj.getObj().toString());
            // TODO: Manage command.equals("colorMode" || "filterMode"), return boolean

            if (command.equals("previewFrame")) {
                processPreviewFrame((PreviewFrame) obj.getObj());
            } else if (command.equals("pictureTaken")) {
                processPicture((Mat) obj.getObj());
            }
        }
    }

    private void decodeBarcode(Mat frame, String region) {
        try {
            int w = frame.width();
            int h = frame.height();
            frame = frame.submat(0, h / 4, w / 2 + h / 4, w);

            Bitmap bMap = Bitmap.createBitmap(frame.width(), frame.height(), Bitmap.Config.ARGB_8888);
            org.opencv.android.Utils.matToBitmap(frame, bMap);
            //frame.release();
            int[] intArray = new int[bMap.getWidth()*bMap.getHeight()];
            //copy pixel data from the Bitmap into the 'intArray' array
            bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());

            LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(),intArray);

            BinaryBitmap bBitmap = new BinaryBitmap(new HybridBinarizer(source));

            Reader reader = new ITFReader();
            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            Result barcode = reader.decode(bBitmap, hints);
            Log.d("BARCODE", "Barcode: " + barcode.getText());
            if(!barcode.getText().isEmpty()) {
                currentBarcode = barcode.getText();
                numOfBarcodes++;
                if(region == "northEast") {
                    rotate = "northEast";
                } else {
                    rotate = "southWest";
                }
            } else {
                numOfBarcodes = 0;
            }
        } catch (Exception e) {
            currentBarcode = null;
            numOfBarcodes = 0;
        }
    }

    private void decodeBarcodeWest(Mat frame, String region) {
        try {
            int w = frame.width();
            int h = frame.height();
            frame = frame.submat(0, h / 4, w / 2 + h / 4, w);

            Bitmap bMap = Bitmap.createBitmap(frame.width(), frame.height(), Bitmap.Config.ARGB_8888);
            org.opencv.android.Utils.matToBitmap(frame, bMap);
            int[] intArray = new int[bMap.getWidth()*bMap.getHeight()];
            //copy pixel data from the Bitmap into the 'intArray' array
            bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());

            LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(),intArray);

            BinaryBitmap bBitmap = new BinaryBitmap(new HybridBinarizer(source));

            Reader reader = new ITFReader();
            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            Result barcode = reader.decode(bBitmap, hints);
            Log.d("BARCODE", "Barcode: " + barcode.getText());
            if(!barcode.getText().isEmpty()) {
                currentBarcodeWest = barcode.getText();
                numOfBarcodes++;
                if(region == "northEast") {
                    rotate = "northEast";
                } else {
                    rotate = "southWest";
                }
            } else {
                numOfBarcodes = 0;
            }
        } catch (Exception e) {
            currentBarcodeWest = null;
            numOfBarcodes = 0;
        }
    }

    private void processPreviewFrame(PreviewFrame previewFrame) {
        Mat frame = previewFrame.getFrame();
        boolean focused = mMainActivity.isFocused();

        if (detectRectangleInFrame(frame) && focused) {
            try {
                Mat northEast = detectDocumentDoc(frame);
                decodeBarcode(northEast, "northEast");
                if(currentBarcode == null) {
                    Mat southWest = northEast.clone();
                    Core.flip(southWest, southWest, 1);
                    Core.flip(southWest, southWest, 0);
                    decodeBarcodeWest(southWest, "southWest");
                }
                //numOfSquares++;
                double now = (double)(new Date()).getTime() / 1000.0;
                if (numOfBarcodes == numOfRectangles && now > lastCaptureTime + durationBetweenCaptures && currentBarcode != null || currentBarcodeWest != null) {
                    lastCaptureTime = now;
                    numOfBarcodes = 0;
                    mMainActivity.requestPicture();
                }
            } catch (Exception ignored){
                Log.d("BARCODE", String.valueOf(ignored));
            }
        } else {
            numOfBarcodes = 0;
        }

        frame.release();
        mMainActivity.setImageProcessorBusy(false);
    }

    private void processPicture(Mat picture) {
        Mat img = Imgcodecs.imdecode(picture, Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);
        picture.release();

        Log.d(TAG, "processPicture - imported image " + img.size().width + "x" + img.size().height);

        if (mBugRotate) {
            Core.flip(img, img, 1);
            Core.flip(img, img, 0);
        }

        ScannedDocument doc = detectDocument(img);
        mMainActivity.getHUD().clear();
        mMainActivity.invalidateHUD();
        mMainActivity.saveDocument(doc, currentBarcode != null ? currentBarcode : currentBarcodeWest, rotate);
        doc.release();
        picture.release();
        currentBarcode = null;

        mMainActivity.setImageProcessorBusy(false);
        mMainActivity.waitSpinnerInvisible();
    }

    private ScannedDocument detectDocument(Mat inputRgba) {
        ScannedDocument sd = new ScannedDocument(inputRgba);
        ArrayList<MatOfPoint> contours = findContours(inputRgba);

        sd.originalSize = inputRgba.size();
        Quadrilateral quad = getQuadrilateral(contours, sd.originalSize);

        double ratio = sd.originalSize.height / 500;
        sd.heightWithRatio = Double.valueOf(sd.originalSize.width / ratio).intValue();
        sd.widthWithRatio = Double.valueOf(sd.originalSize.height / ratio).intValue();

        Mat doc;
        if (quad != null) {
            sd.originalPoints = new Point[4];

            // TopLeft
            sd.originalPoints[0] = new Point(
              (sd.widthWithRatio - quad.points[3].y), 
              quad.points[3].x);

            // TopRight
            sd.originalPoints[1] = new Point(
              (sd.widthWithRatio - quad.points[0].y), 
              quad.points[0].x);
            
            // BottomRight
            sd.originalPoints[2] = new Point(
              (sd.widthWithRatio - quad.points[1].y), 
              quad.points[1].x);

            // BottomLeft
            sd.originalPoints[3] = new Point(
              (sd.widthWithRatio - quad.points[2].y), 
              quad.points[2].x);

            sd.quadrilateral = quad;
            sd.previewPoints = mPreviewPoints;
            sd.previewSize = mPreviewSize;

            doc = fourPointTransform(inputRgba, quad.points);
        } else {
            doc = new Mat(inputRgba.size(), CvType.CV_8UC4);
            inputRgba.copyTo(doc);
        }
        enhanceDocument(doc);
        return sd.setProcessed(doc);
    }

    private Mat detectDocumentDoc(Mat inputRgba) {
        ScannedDocument sd = new ScannedDocument(inputRgba);
        ArrayList<MatOfPoint> contours = findContours(inputRgba);

        sd.originalSize = inputRgba.size();
        Quadrilateral quad = getQuadrilateral(contours, sd.originalSize);

        double ratio = sd.originalSize.height / 500;
        sd.heightWithRatio = Double.valueOf(sd.originalSize.width / ratio).intValue();
        sd.widthWithRatio = Double.valueOf(sd.originalSize.height / ratio).intValue();

        Mat doc;
        if (quad != null) {
            sd.originalPoints = new Point[4];

            // TopLeft
            sd.originalPoints[0] = new Point(
                    (sd.widthWithRatio - quad.points[3].y),
                    quad.points[3].x);

            // TopRight
            sd.originalPoints[1] = new Point(
                    (sd.widthWithRatio - quad.points[0].y),
                    quad.points[0].x);

            // BottomRight
            sd.originalPoints[2] = new Point(
                    (sd.widthWithRatio - quad.points[1].y),
                    quad.points[1].x);

            // BottomLeft
            sd.originalPoints[3] = new Point(
                    (sd.widthWithRatio - quad.points[2].y),
                    quad.points[2].x);

            sd.quadrilateral = quad;
            sd.previewPoints = mPreviewPoints;
            sd.previewSize = mPreviewSize;

            doc = fourPointTransform(inputRgba, quad.points);
        } else {
            doc = new Mat(inputRgba.size(), CvType.CV_8UC4);
            inputRgba.copyTo(doc);
        }
        enhanceDocument(doc);
        return doc;
    }

    private final HashMap<String, Long> pageHistory = new HashMap<>();

    private boolean checkQR(String qrCode) {
        return !(pageHistory.containsKey(qrCode) && pageHistory.get(qrCode) > new Date().getTime() / 1000 - 15);
    }

    private boolean detectRectangleInFrame(Mat inputRgba) {
        ArrayList<MatOfPoint> contours = findContours(inputRgba);
        Quadrilateral quad = getQuadrilateral(contours, inputRgba.size());

        mPreviewPoints = null;
        mPreviewSize = inputRgba.size();
        Bundle data = new Bundle();

        if (quad != null) {
            Point[] rescaledPoints = new Point[4];
            double ratio = inputRgba.size().height / 500;

            for (int i = 0; i < 4; i++) {
                int x = Double.valueOf(quad.points[i].x * ratio).intValue();
                int y = Double.valueOf(quad.points[i].y * ratio).intValue();
                if (mBugRotate) {
                    rescaledPoints[(i + 2) % 4] = new Point(
                      Math.abs(x - mPreviewSize.width),
                      Math.abs(y - mPreviewSize.height)
                    );
                } else {
                    rescaledPoints[i] = new Point(x, y);
                }
            }

            mPreviewPoints = rescaledPoints;
            drawDocumentBox(mPreviewPoints, mPreviewSize);

            Bundle quadMap = quad.toBundle();
            data.putBundle("detectedRectangle", quadMap);
            mMainActivity.rectangleWasDetected(Arguments.fromBundle(data));
            return true;
        }

        data.putBoolean("detectedRectangle", false);
        mMainActivity.getHUD().clear();
        mMainActivity.invalidateHUD();
        mMainActivity.rectangleWasDetected(Arguments.fromBundle(data));
        return false;
    }

    private void drawDocumentBox(Point[] points, Size stdSize) {
        Path path = new Path();
        HUDCanvasView hud = mMainActivity.getHUD();
        // ATTENTION: axis are swapped

        float previewWidth = (float) stdSize.height;
        float previewHeight = (float) stdSize.width;

        path.moveTo(previewWidth - (float) points[0].y, (float) points[0].x);
        path.lineTo(previewWidth - (float) points[1].y, (float) points[1].x);
        path.lineTo(previewWidth - (float) points[2].y, (float) points[2].x);
        path.lineTo(previewWidth - (float) points[3].y, (float) points[3].x);
        path.close();

        PathShape newBox = new PathShape(path, previewWidth, previewHeight);
        Paint paint = new Paint();
        paint.setColor(mMainActivity.parsedOverlayColor());

        Paint border = new Paint();
        border.setColor(mMainActivity.parsedOverlayColor());
        border.setStrokeWidth(5);

        hud.clear();
        hud.addShape(newBox, paint, border);
        mMainActivity.invalidateHUD();
    }

    private Quadrilateral getQuadrilateral(ArrayList<MatOfPoint> contours, Size srcSize) {
        double ratio = srcSize.height / 500;
        int height = Double.valueOf(srcSize.height / ratio).intValue();
        int width = Double.valueOf(srcSize.width / ratio).intValue();
        Size size = new Size(width, height);

        Log.i("COUCOU", "Size----->" + size);
        for (MatOfPoint c : contours) {
            MatOfPoint2f c2f = new MatOfPoint2f(c.toArray());
            double peri = Imgproc.arcLength(c2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true);

            Point[] points = approx.toArray();
            // select biggest 4 angles polygon
            // if (points.length == 4) {
            Point[] foundPoints = sortPoints(points);

            if (insideArea(foundPoints, size)) {
                return new Quadrilateral(c, foundPoints, new Size(srcSize.width, srcSize.height));
            }
            // }
        }

        return null;
    }

    private Point[] sortPoints(Point[] src) {
        ArrayList<Point> srcPoints = new ArrayList<>(Arrays.asList(src));
        Point[] result = { null, null, null, null };

        Comparator<Point> sumComparator = new Comparator<Point>() {
            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.compare(lhs.y + lhs.x, rhs.y + rhs.x);
            }
        };

        Comparator<Point> diffComparator = new Comparator<Point>() {
            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.compare(lhs.y - lhs.x, rhs.y - rhs.x);
            }
        };

        // top-left corner = minimal sum
        result[0] = Collections.min(srcPoints, sumComparator);

        // bottom-right corner = maximal sum
        result[2] = Collections.max(srcPoints, sumComparator);

        // top-right corner = minimal difference
        result[1] = Collections.min(srcPoints, diffComparator);

        // bottom-left corner = maximal difference
        result[3] = Collections.max(srcPoints, diffComparator);

        return result;
    }

    private boolean insideArea(Point[] rp, Size size) {
        int width = Double.valueOf(size.width).intValue();
        int height = Double.valueOf(size.height).intValue();

        int minimumSize = width / 10;

        boolean isANormalShape = rp[0].x != rp[1].x && rp[1].y != rp[0].y && rp[2].y != rp[3].y && rp[3].x != rp[2].x;
        boolean isBigEnough = ((rp[1].x - rp[0].x >= minimumSize) && (rp[2].x - rp[3].x >= minimumSize)
                && (rp[3].y - rp[0].y >= minimumSize) && (rp[2].y - rp[1].y >= minimumSize));

        double leftOffset = rp[0].x - rp[3].x;
        double rightOffset = rp[1].x - rp[2].x;
        double bottomOffset = rp[0].y - rp[1].y;
        double topOffset = rp[2].y - rp[3].y;

        boolean isAnActualRectangle = ((leftOffset <= minimumSize && leftOffset >= -minimumSize)
                && (rightOffset <= minimumSize && rightOffset >= -minimumSize)
                && (bottomOffset <= minimumSize && bottomOffset >= -minimumSize)
                && (topOffset <= minimumSize && topOffset >= -minimumSize));

        return isANormalShape && isAnActualRectangle && isBigEnough;
    }

    private void enhanceDocument(Mat src) {
        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2GRAY);
        src.convertTo(src, CvType.CV_8UC1, colorGain, colorBias);
    }

    private Mat fourPointTransform(Mat src, Point[] pts) {
        double ratio = src.size().height / 500;
        Point tl = pts[0];
        Point tr = pts[1];
        Point br = pts[2];
        Point bl = pts[3];

        double widthA = Math.sqrt(Math.pow(br.x - bl.x, 2) + Math.pow(br.y - bl.y, 2));
        double widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2) + Math.pow(tr.y - tl.y, 2));

        double dw = Math.max(widthA, widthB) * ratio;
        int maxWidth = Double.valueOf(dw).intValue();

        double heightA = Math.sqrt(Math.pow(tr.x - br.x, 2) + Math.pow(tr.y - br.y, 2));
        double heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2) + Math.pow(tl.y - bl.y, 2));

        double dh = Math.max(heightA, heightB) * ratio;
        int maxHeight = Double.valueOf(dh).intValue();

        Mat doc = new Mat(maxHeight, maxWidth, CvType.CV_8UC4);

        Mat src_mat = new Mat(4, 1, CvType.CV_32FC2);
        Mat dst_mat = new Mat(4, 1, CvType.CV_32FC2);

        src_mat.put(0, 0, tl.x * ratio, tl.y * ratio, tr.x * ratio, tr.y * ratio, br.x * ratio, br.y * ratio,
                bl.x * ratio, bl.y * ratio);
        dst_mat.put(0, 0, 0.0, 0.0, dw, 0.0, dw, dh, 0.0, dh);

        Mat m = Imgproc.getPerspectiveTransform(src_mat, dst_mat);

        Imgproc.warpPerspective(src, doc, m, doc.size());

        return doc;
    }

    private ArrayList<MatOfPoint> findContours(Mat src) {
        Mat grayImage;
        Mat cannedImage;
        Mat resizedImage;

        double ratio = src.size().height / 500;
        int height = Double.valueOf(src.size().height / ratio).intValue();
        int width = Double.valueOf(src.size().width / ratio).intValue();
        Size size = new Size(width, height);

        resizedImage = new Mat(size, CvType.CV_8UC4);
        grayImage = new Mat(size, CvType.CV_8UC4);
        cannedImage = new Mat(size, CvType.CV_8UC1);

        Imgproc.resize(src, resizedImage, size);
        Imgproc.cvtColor(resizedImage, grayImage, Imgproc.COLOR_RGBA2GRAY, 4);
        Imgproc.GaussianBlur(grayImage, grayImage, new Size(5, 5), 0);
        Imgproc.Canny(grayImage, cannedImage, 0, 255, 3, false);

        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(cannedImage, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        hierarchy.release();

        Collections.sort(contours, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint lhs, MatOfPoint rhs) {
                return Double.compare(Imgproc.contourArea(rhs), Imgproc.contourArea(lhs));
            }
        });

        resizedImage.release();
        grayImage.release();
        cannedImage.release();

        return contours;
    }

    public void setBugRotate(boolean bugRotate) {
        mBugRotate = bugRotate;
    }
}
