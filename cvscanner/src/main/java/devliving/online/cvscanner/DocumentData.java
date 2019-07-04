package devliving.online.cvscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import org.opencv.core.Point;

import java.io.IOException;

import devliving.online.cvscanner.util.Util;

public class DocumentData implements Parcelable {
    public final static int V_FILTER_TYPE_COLOR = 0;
    public final static int V_FILTER_TYPE_GRAYSCALE = 1;
    public final static int V_FILTER_TYPE_BLACK_WHITE = 2;
    public final static int V_FILTER_TYPE_PHOTO = 3;

    private Bitmap mOriginalImage;
    private String mOriginalImagePath;
    private int mRotation;
    private Point[] mPoints;
    private int mFilterType;
    private String mImagePath;

    public static final Parcelable.Creator<DocumentData> CREATOR = new Parcelable.Creator<DocumentData>() {
        @Override
        public DocumentData createFromParcel(Parcel source) {
            return new DocumentData(source);
        }

        @Override
        public DocumentData[] newArray(int size) {
            return new DocumentData[size];
        }
    };

    DocumentData(Parcel parcel) {
        mOriginalImagePath = parcel.readString();
        mRotation = parcel.readInt();
        int pointLength = parcel.readInt();
        mPoints = new Point[pointLength];
        for (int n = 0; n < pointLength; n++)
            mPoints[n] = new Point(parcel.readDouble(), parcel.readDouble());
        mFilterType = parcel.readInt();
        mImagePath = parcel.readString();
    }

    public static DocumentData Create(Context context, Document document, int filterType) {
        return Create(context, document.getImage().getBitmap(), document.getImage().getMetadata().getRotation(), document.detectedQuad.points, filterType, true);
    }

    public static DocumentData Create(Context context, Bitmap originalImage, int filterType) {
        return Create(context, originalImage, 0, new Point[0], filterType, true);
    }

    public static DocumentData Create(Context context, Bitmap originalImage, int rotation, Point[] points, int filterType, boolean saveImage) {
        try {
            String originalImagePath;
            if (saveImage)
                originalImagePath = Util.saveImage(context, "IMG_CVScanner_" + System.currentTimeMillis(), originalImage, false);
            else
                originalImagePath = null;
            return new DocumentData(originalImage, originalImagePath, rotation, points, filterType);
        } catch (IOException e) {
            return null;
        }
    }

    private DocumentData(Bitmap originalImage, String originalImagePath, int rotation, Point[] points, int filterType) {
        mOriginalImage = originalImage;
        mOriginalImagePath = originalImagePath;
        mRotation = rotation;
        mPoints = points;
        mFilterType = filterType;
        mImagePath = null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mOriginalImagePath);
        dest.writeInt(mRotation);
        dest.writeInt(mPoints.length);
        for (int n = 0; n < mPoints.length; n++) {
            dest.writeDouble(mPoints[n].x);
            dest.writeDouble(mPoints[n].y);
        }
        dest.writeInt(mFilterType);
        dest.writeString(mImagePath);
    }

    public Bitmap useImage() {
        return mOriginalImage;
    }

    public int getRotation() {
        return mRotation;
    }

    public void setRotation(int rotation) {
        mRotation = rotation;
    }

    public Point[] getPoints() {
        return mPoints;
    }

    public int getFilterType() {
        return mFilterType;
    }

    public void setFilterType(int filterType) {
        mFilterType = filterType;
    }

    public String getImagePath() {
        return mImagePath;
    }

    public void setImagePath(String imagePath) {
        mImagePath = imagePath;
    }
}
