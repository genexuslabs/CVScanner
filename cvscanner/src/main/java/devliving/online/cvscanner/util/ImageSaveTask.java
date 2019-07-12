package devliving.online.cvscanner.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;

import java.io.IOException;

import devliving.online.cvscanner.Document;
import devliving.online.cvscanner.DocumentData;

import static devliving.online.cvscanner.DocumentData.V_FILTER_TYPE_BLACK_WHITE;
import static devliving.online.cvscanner.DocumentData.V_FILTER_TYPE_COLOR;
import static devliving.online.cvscanner.DocumentData.V_FILTER_TYPE_GRAYSCALE;
import static devliving.online.cvscanner.DocumentData.V_FILTER_TYPE_PHOTO;

/**
 * Created by Mehedi Hasan Khan <mehedi.mailing@gmail.com> on 8/30/17.
 */

public class ImageSaveTask extends AsyncTask<Void, Void, String> {
    private DocumentData mData;
    private Context mContext;
    private SaveCallback mCallback;

    public ImageSaveTask(Context context, DocumentData data, SaveCallback callback) {
        this.mData = data;
        this.mContext = context;
        this.mCallback = callback;
    }

    @Override
    protected void onPreExecute() {
        mCallback.onSaveTaskStarted();
    }

    /**
     * Override this method to perform a computation on a background thread. The
     * specified parameters are the parameters passed to {@link #execute}
     * by the caller of this task.
     * <p/>
     * This method can call {@link #publishProgress} to publish updates
     * on the UI thread.
     *
     * @param params The parameters of the task.
     * @return A result, defined by the subclass of this task.
     * @see #onPreExecute()
     * @see #onPostExecute
     * @see #publishProgress
     */
    @Override
    protected String doInBackground(Void... params) {
        Bitmap image = mData.useOriginalImage();
        Size imageSize = new Size(image.getWidth(), image.getHeight());
        Mat imageMat = new Mat(imageSize, CvType.CV_8UC4);
        Utils.bitmapToMat(image, imageMat);
        image.recycle();

        Mat croppedImage;
        if (mData.getPoints().length == 4) {
            croppedImage = CVProcessor.fourPointTransform(imageMat, mData.getPoints());
            imageMat.release();
        } else {
            croppedImage = imageMat;
        }

        Mat enhancedImage;

        switch (mData.getFilterType()) {
            case V_FILTER_TYPE_COLOR:
            default:
                Mat adjustedImage = CVProcessor.adjustBrightnessAndContrast(croppedImage, 1);
                croppedImage.release();
                enhancedImage = CVProcessor.sharpenImage(adjustedImage);
                adjustedImage.release();
                break;
            case V_FILTER_TYPE_GRAYSCALE:
                enhancedImage = CVProcessor.convertGrayscale(croppedImage);
                croppedImage.release();
                break;
            case V_FILTER_TYPE_BLACK_WHITE:
                enhancedImage = CVProcessor.convertBlackAndWhite(croppedImage);
                croppedImage.release();
                break;
            case V_FILTER_TYPE_PHOTO:
                enhancedImage = croppedImage;
        }

        String imagePath = null;
        try {
            imagePath = Util.saveImage(mContext,
                    "IMG_CVScanner_" + System.currentTimeMillis(), enhancedImage, false);
            enhancedImage.release();
            mData.setImageUri(Util.getUriFromPath(imagePath));
            Util.setExifRotation(mContext, Util.getUriFromPath(imagePath), mData.getRotation());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imagePath;
    }

    @Override
    protected void onPostExecute(String path) {
        if (path != null)
            mCallback.onSaved(path);
        else
            mCallback.onSaveFailed(new Exception("could not save image"));
    }

    public interface SaveCallback {
        void onSaveTaskStarted();
        void onSaved(String savedPath);
        void onSaveFailed(Exception error);
    }
}