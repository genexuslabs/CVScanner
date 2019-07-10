package devliving.online.cvscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.opencv.core.Point;

import java.io.File;
import java.io.IOException;

import devliving.online.cvscanner.util.Util;

public class DocumentData implements Parcelable {
    public final static int V_FILTER_TYPE_COLOR = 0;
    public final static int V_FILTER_TYPE_GRAYSCALE = 1;
    public final static int V_FILTER_TYPE_BLACK_WHITE = 2;
    public final static int V_FILTER_TYPE_PHOTO = 3;

    private Bitmap mOriginalImage;
    private Uri mOriginalImageUri;
    private int mRotation;
    private Point[] mPoints;
    private int mFilterType;
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
        mFilterType = parcel.readInt();
        String imageUri = parcel.readString();
        if (!TextUtils.isEmpty(imageUri))
            mImageUri = Uri.parse(imageUri);
    }

    public static DocumentData Create(Context context, Document document, int filterType) {
        return Create(context, document.getImage().getBitmap(), document.getImage().getMetadata().getRotation(), document.detectedQuad.points, filterType, true);
    }

    public static DocumentData Create(Context context, Bitmap originalImage, int filterType) {
        return Create(context, originalImage, 0, new Point[0], filterType, true);
    }

    public static DocumentData Create(Context context, Bitmap originalImage, int rotation, Point[] points, int filterType, boolean saveImage) {
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
            return null;
        }
    }

    public static DocumentData Create(Context context, Uri imageUri) {
        int rotation = 0;
        try {
            rotation = Util.getExifRotation(context, imageUri);
        } catch (IOException ignored) { }
        return new DocumentData(null, imageUri, rotation/90, new Point[0], V_FILTER_TYPE_COLOR);
    }

    private DocumentData(Bitmap originalImage, Uri originalImageUri, int rotation, Point[] points, int filterType) {
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
        dest.writeInt(mFilterType);
        dest.writeString(mImageUri != null ? mImageUri.toString() : "");
    }

    public Bitmap useImage() {
        return mOriginalImage;
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

    public int getFilterType() {
        return mFilterType;
    }

    public void setFilterType(int filterType) {
        mFilterType = filterType;
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
