package devliving.online.cvscanner;

import org.opencv.core.Point;

import java.io.Serializable;

public class DocumentData implements Serializable {
    public final static int V_FILTER_TYPE_COLOR = 0;
    public final static int V_FILTER_TYPE_GRAYSCALE = 1;
    public final static int V_FILTER_TYPE_BLACK_WHITE = 2;
    public final static int V_FILTER_TYPE_PHOTO = 3;

    private String mOriginalImagePath;
    private int mRotation;
    private Point[] mPoints;
    private int mFilterType;
    private String mImagePath;

    public void setRotation(int rotation) {
        mRotation = rotation;
    }

    public int getRotation() {
        return mRotation;
    }

    public void setFilterType(int filterType) {
        mFilterType = filterType;
    }

    public String getImagePath() {
        return mImagePath;
    }
}
