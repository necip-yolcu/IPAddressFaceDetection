package com.camera.simplemjpeg;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FaceOverlayView extends View {

    private Bitmap mBitmap;
    private SparseArray<Face> mFaces;

    List<Float> xvalues = new ArrayList<>();
    List<Integer> motion = new ArrayList<>();
    int giris = 0;
    int cikis = 0;
    Mat frame;

    public FaceOverlayView(Context context) {
        this(context, null);
    }

    public FaceOverlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FaceOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setBitmap( Bitmap bitmap ) {
        mBitmap = bitmap;


        FaceDetector detector = new FaceDetector.Builder( getContext() )
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.FAST_MODE)
                .build();

        if (!detector.isOperational()) {
            //Handle contingency
        } else {
            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
            mFaces = detector.detect(frame);
            detector.release();
        }
        invalidate();
        //requestLayout();
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);


        //if (mBitmap != null) {
        if ((mBitmap != null) && (mFaces != null)){
            double scale = drawBitmap(canvas);
            drawFaceBox(canvas, scale);     ////////////
        }
    }

    private double drawBitmap( Canvas canvas ) {
        double viewWidth = canvas.getWidth();
        double viewHeight = canvas.getHeight();
        double imageWidth = mBitmap.getWidth();
        double imageHeight = mBitmap.getHeight();
        double scale = Math.min( viewWidth / imageWidth, viewHeight / imageHeight );

        //Rect destBounds = new Rect( 0, 0, (int) ( imageWidth * scale ), (int) ( imageWidth * scale ) );
        Rect destBounds = new Rect( 0, 0, (int) ( imageWidth * scale ), (int) ( imageHeight * scale ) );

        // Bitmap to Mat
        frame = new Mat(mBitmap.getHeight(), mBitmap.getWidth(), CvType.CV_8UC1);
        Bitmap bmp32 = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, frame);

        // add line and text
        Imgproc.line(frame, new Point(frame.width()/2, 0), new Point(frame.width()/2, frame.height()), new Scalar(0, 255, 0), 2);   //frame.width()/2 = 160, frame.height() = 720
        Imgproc.putText(frame, String.format("Giris: %s", giris), new Point(10, 20), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 255, 0), 2);
        Imgproc.putText(frame, String.format("Cikis: %s", cikis), new Point(10, 40), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 0, 255), 2);

        // Mat to Bitmap
        mBitmap = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(frame, mBitmap);

        canvas.drawBitmap( mBitmap, null, destBounds, null );
        return scale;
    }

    private void drawFaceBox(Canvas canvas, double scale) {
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        float left = 0;
        float top = 0;
        float right = 0;
        float bottom = 0;
        boolean flag = true;

        Rect rect = null;
        for( int i = 0; i < mFaces.size(); i++ ) {
            Face face = mFaces.valueAt(i);

            left = (float) ( face.getPosition().x * scale );
            top = (float) ( face.getPosition().y * scale );
            right = (float) scale * ( face.getPosition().x + face.getWidth() );
            bottom = (float) scale * ( face.getPosition().y + face.getHeight() );

            //xvalues.add(face.getPosition().x);
            xvalues.add((left+right)/2);
            flag = false;

            canvas.drawRect( left, top, right, bottom, paint );
        }

        int no_x = xvalues.size();
        if (no_x > 2) {
            Float difference = xvalues.get(no_x - 1) - xvalues.get(no_x - 2);
            if (difference > 0)
                motion.add(1);
            else
                motion.add(0);
        }

        if (flag) {
            if (no_x > 5) {
                System.out.println("wait for function");
                int[] find_maj;
                find_maj = majority(motion);
                //if((find_maj[0] == 1) && find_maj[1] >= 15)
                if(find_maj[0] == 1)
                    giris += 1;
                else
                    cikis += 1;
            }
            xvalues.clear();
            motion.clear();
        }
    }

    public static int[] majority(List<Integer> array) {
        Map<Integer, Integer> myMap = new HashMap<Integer, Integer>();
        int[] maximum = new int[]{0,0};
        for (int n : array) {
            if (myMap.containsKey(n))
                myMap.put(n, myMap.get(n) + 1);
            else
                myMap.put(n, 1);

            if (myMap.get(n) > maximum[1]){
                maximum[0] = n;
                maximum[1] = myMap.get(n);
            }
        }
        return maximum;
    }

}