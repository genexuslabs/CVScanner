package devliving.online.cvscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Point;

import java.util.ArrayList;

import devliving.online.cvscanner.util.ImageSaveTask;

/**
 * Created by Mehedi Hasan Khan <mehedi.mailing@gmail.com> on 8/29/17.
 */

public abstract class BaseFragment extends Fragment implements ImageSaveTask.SaveCallback {

    public interface ImageProcessorCallback {
        void onImageProcessingFailed(String reason, @Nullable Exception error);
        void onImagesProcessed(ArrayList<DocumentData> dataList);
    }

    private ImageProcessorCallback mCallback = null;
    protected boolean mIsBusy = false;
    protected ArrayList<DocumentData> mDataList;

    protected void loadOpenCV(){
        if (!OpenCVLoader.initDebug()){
            //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, getActivity().getApplicationContext(), mLoaderCallback);
        }
        else{
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    protected abstract void onOpenCVConnected();
    protected abstract void onAfterViewCreated();

    private void onOpenCVConnectionFailed() {
        if (mCallback != null)
            mCallback.onImageProcessingFailed("Could not load OpenCV", null);
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(getActivity()) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS)
                onOpenCVConnected();
            else
                onOpenCVConnectionFailed();
        }
    };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        onAfterViewCreated();
        loadOpenCV();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof ImageProcessorCallback)
            mCallback = (ImageProcessorCallback) context;
    }

    @Override
    public void onSaveTaskStarted() {
        mIsBusy = true;
    }

    @Override
    public void onSaved(String path) {
        Log.d("BASE", "saved at: " + path);
        mIsBusy = false;
    }

    @Override
    public void onSaveFailed(Exception error) {
        if (mCallback != null)
            mCallback.onImageProcessingFailed("Failed to save image", error);
        mIsBusy = false;
    }

    protected void done() {
        if (mCallback != null)
            mCallback.onImagesProcessed(mDataList);
    }

    protected synchronized void saveCroppedImage(DocumentData data) {
        if (!mIsBusy) {
            new ImageSaveTask(getContext(), data, this).execute();
            mIsBusy = true;
        }
    }
}
