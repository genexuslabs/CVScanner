package devliving.online.cvscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import org.opencv.core.Point;

import java.io.File;
import java.io.IOException;
import java.util.logging.Filter;

import devliving.online.cvscanner.util.Util;

public class DocumentData implements Parcelable {
    private Bitmap mOriginalImage;
    private Uri mOriginalImageUri;
    private int mRotation;
    private Point[] mPoints;
    private Point[] mPreviewPoints;
    private FilterType mFilterType;
    private Uri mImageUri;

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
        mOriginalImageUri = Uri.parse(parcel.readString());
        mRotation = parcel.readInt();
        int pointLength = parcel.readInt();
        mPoints = new Point[pointLength];
        for (int n = 0; n < pointLength; n++)
            mPoints[n] = new Point(parcel.readDouble(), parcel.readDouble());
        mFilterType = FilterType.values()[parcel.readInt()];
        String imageUri = parcel.readString();
        if (!TextUtils.isEmpty(imageUri))
            mImageUri = Uri.parse(imageUri);
    }

    public static DocumentData Create(Context context, Document document, FilterType filterType) {
        return Create(context, document.getImage().getBitmap(), document.getImage().getMetadata().getRotation(), document.detectedQuad.points, filterType, true);
    }

    public static DocumentData Create(Context context, Bitmap originalImage, FilterType filterType) {
        return Create(context, originalImage, 0, new Point[0], filterType, true);
    }

    public static DocumentData Create(Context context, Bitmap originalImage, int rotation, Point[] points, FilterType filterType, boolean saveImage) {
        try {
            Uri originalImageUri;
            if (saveImage) {
                String path = Util.saveImage(context, "IMG_CVScanner_" + System.currentTimeMillis(), originalImage, false);
                File file = new File(path);
                originalImageUri = Util.getUriForFile(context, file);
            }
            else {
                originalImageUri = null;
            }
            return new DocumentData(originalImage, originalImageUri, rotation, points, filterType);
        } catch (IOException e) {
            Log.d("Scanner", "Fail to save image for DocumentData: " + e.getMessage());
            return null;
        }
    }

    public static DocumentData Create(Context context, Uri imageUri) {
        int rotation = 0;
        try {
            rotation = Util.getExifRotation(context, imageUri);
        } catch (IOException ignored) { }
        return new DocumentData(null, imageUri, rotation/90, new Point[0], FilterType.Color);
    }

    private DocumentData(Bitmap originalImage, Uri originalImageUri, int rotation, Point[] points, FilterType filterType) {
        mOriginalImage = originalImage;
        mOriginalImageUri = originalImageUri;
        mRotation = rotation;
        mPoints = points;
        mFilterType = filterType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mOriginalImageUri.toString());
        dest.writeInt(mRotation);
        dest.writeInt(mPoints.length);
        for (int n = 0; n < mPoints.length; n++) {
            dest.writeDouble(mPoints[n].x);
            dest.writeDouble(mPoints[n].y);
        }
        dest.writeInt(mFilterType.ordinal());
        dest.writeString(mImageUri != null ? mImageUri.toString() : "");
    }

    public Bitmap useOriginalImage() {
        Bitmap image = mOriginalImage;
        mOriginalImage = null; // 1 time use, it can be recycled after use
        return image;
    }

    public void setOriginalImage(Bitmap image) {
        mOriginalImage = image;
    }

    public int getRotation() {
        return mRotation;
    }

    public void rotate(int delta) {
        if (delta < 0)
            delta = -delta * 3;
        mRotation += delta;
        mRotation = mRotation % 4;
    }

    public Point[] getPoints() {
        return mPoints;
    }

    public void setPoints(Point[] points) {
        mPoints = points;
    }

    public void setPreviewPoints(Point[] points) {
        mPreviewPoints = points;
    }

    public Point[] getPreviewPoints() {
        return mPreviewPoints;
    }

    public int getWidth() {
        return mOriginalImage.getWidth();
    }

    public int getHeight() {
        return mOriginalImage.getHeight();
    }

    public FilterType getFilterType() {
        return mFilterType;
    }

    public void setFilterType(FilterType filterType) {
        mFilterType = filterType;
    }

    public Uri getOriginalImageUri() {
        return mOriginalImageUri;
    }

    public Uri getImageUri() {
        if (mImageUri == null)
            return mOriginalImageUri;
        else
            return mImageUri;
    }

    public void setImageUri(Uri imageUri) {
        mImageUri = imageUri;
    }
}
