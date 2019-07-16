package devliving.online.cvscanner.scanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.shapes.PathShape;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import org.opencv.core.Point;

import devliving.online.cvscanner.Document;
import devliving.online.cvscanner.util.CVProcessor;
import online.devliving.mobilevisionpipeline.GraphicOverlay;
import online.devliving.mobilevisionpipeline.Util;

/**
 * Created by user on 10/15/16.
 */
public class DocumentGraphic extends GraphicOverlay.Graphic {
    int Id;
    Document scannedDoc;
    Paint borderPaint, bodyPaint;

    int borderColor = Color.parseColor("#41fa97"), fillColor = Color.parseColor("#69fbad");

    public DocumentGraphic(GraphicOverlay overlay, Document doc) {
        super(overlay);
        scannedDoc = doc;

        borderPaint = new Paint();
        borderPaint.setColor(borderColor);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeCap(Paint.Cap.ROUND);
        borderPaint.setStrokeJoin(Paint.Join.ROUND);
        borderPaint.setStrokeWidth(12);

        bodyPaint = new Paint();
        bodyPaint.setColor(fillColor);
        bodyPaint.setAlpha(180);
        bodyPaint.setStyle(Paint.Style.FILL);
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    void update(Document doc){
        scannedDoc = doc;
        postInvalidate();
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
        borderPaint.setColor(borderColor);
    }

    public void setFillColor(int fillColor) {
        this.fillColor = fillColor;
        bodyPaint.setColor(fillColor);
    }

    /**
     * Draw the graphic on the supplied canvas.  Drawing should use the following methods to
     * convert to view coordinates for the graphics that are drawn:
     * <ol>
     * <li>{@link GraphicOverlay.Graphic#scaleX(float)} and {@link GraphicOverlay.Graphic#scaleY(float)} adjust the size of
     * the supplied value from the preview scale to the view scale.</li>
     * <li>{@link GraphicOverlay.Graphic#translateX(float)} and {@link GraphicOverlay.Graphic#translateY(float)} adjust the
     * coordinate from the preview's coordinate system to the view coordinate system.</li>
     * </ol>
     *
     * @param canvas drawing canvas
     */
    @Override
    public void draw(Canvas canvas) {
        if (scannedDoc != null && scannedDoc.getDetectedQuad() != null) {
            int frameWidth = scannedDoc.getImage().getMetadata().getWidth();
            int frameHeight = scannedDoc.getImage().getMetadata().getHeight();

            final int screenRotation = ((WindowManager) mOverlay.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();

            CVProcessor.Quadrilateral detectedQuad = scannedDoc.getDetectedQuad();

            Path path = new Path();
            path.moveTo(getX(frameWidth, frameHeight, screenRotation, detectedQuad.points[0]), getY(frameWidth, frameHeight, screenRotation, detectedQuad.points[0]));
            path.lineTo(getX(frameWidth, frameHeight, screenRotation, detectedQuad.points[1]), getY(frameWidth, frameHeight, screenRotation, detectedQuad.points[1]));
            path.lineTo(getX(frameWidth, frameHeight, screenRotation, detectedQuad.points[2]), getY(frameWidth, frameHeight, screenRotation, detectedQuad.points[2]));
            path.lineTo(getX(frameWidth, frameHeight, screenRotation, detectedQuad.points[3]), getY(frameWidth, frameHeight, screenRotation, detectedQuad.points[3]));
            path.close();

            boolean isPortrait = screenRotation == Surface.ROTATION_0 || screenRotation == Surface.ROTATION_180;
            PathShape shape = new PathShape(path, isPortrait ? frameHeight : frameWidth, isPortrait ? frameWidth : frameHeight);
            shape.resize(canvas.getWidth(), canvas.getHeight());

            shape.draw(canvas, bodyPaint);
            shape.draw(canvas, borderPaint);
        }
    }

    // See http://zhengrui.github.io/android-coordinates.html
    private float getX(int frameWidth, int frameHeight, int screenRotation, Point point) {
        switch (screenRotation) {
            case Surface.ROTATION_0: // Portrait
            default:
                return (float)(frameHeight - point.y);
            case Surface.ROTATION_180: // Reverse Portrait
                return (float)point.y;
            case Surface.ROTATION_90: // Landscape
                return (float)point.x;
            case Surface.ROTATION_270: // Reverse Landscape
                return (float)(frameWidth - point.x);
        }
    }

    private float getY(int frameWidth, int frameHeight, int screenRotation, Point point) {
        switch (screenRotation) {
            case Surface.ROTATION_0: // Portrait
            default:
                return (float)point.x;
            case Surface.ROTATION_180: // Reverse Portrait
                return (float)(frameWidth - point.x);
            case Surface.ROTATION_90: // Landscape
                return (float)point.y;
            case Surface.ROTATION_270: // Reverse Landscape
                return (float)(frameHeight - point.y);
        }
    }
}
