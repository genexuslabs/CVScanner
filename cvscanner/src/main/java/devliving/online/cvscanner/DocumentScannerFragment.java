package devliving.online.cvscanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.MediaActionSound;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;

import org.opencv.core.Point;

import java.io.IOException;

import online.devliving.mobilevisionpipeline.GraphicOverlay;
import online.devliving.mobilevisionpipeline.Util;
import online.devliving.mobilevisionpipeline.camera.CameraSource;
import online.devliving.mobilevisionpipeline.camera.CameraSourcePreview;

/**
 * Created by Mehedi on 10/23/16.
 */
public class DocumentScannerFragment extends BaseFragment implements DocumentTracker.DocumentDetectionListener {
    public final static int V_COLOR_TYPE_COLOR = 0;
    public final static int V_COLOR_TYPE_GRAYSCALE = 1;
    public final static int V_COLOR_TYPE_BLACK_WHITE = 2;
    public final static int V_COLOR_TYPE_PHOTO = 3;
    private final static String ARG_IS_PASSPORT = "is_passport";
    private final static String ARG_SHOW_FLASH = "show_flash";
    private final static String ARG_DISABLE_AUTOMATIC_CAPTURE = "disable_automatic_capture";
    private final static String ARG_COLOR_TYPE = "color_type";
    private final static String ARG_TORCH_COLOR = "torch_color";
    private final static String ARG_TORCH_COLOR_LIGHT = "torch_color_light";
    private final static String ARG_DOC_BORDER_COLOR = "doc_border_color";
    private final static String ARG_DOC_BODY_COLOR = "doc_body_color";

    final Object mLock = new Object();
    Context mContext;

    private int mTorchTintColor = Color.GRAY, mTorchTintColorLight = Color.YELLOW;
    private int mDocumentBorderColor = -1, mDocumentBodyColor = -1;

    private boolean mShowFlash;
    private ImageButton mFlashToggle;
    private boolean mDisableAutomaticCapture;
    private int mColorType;

    private ImageButton mTakePictureButton;
    private Button mCancelButton;
    private Button mManualButton;
    private Button mDoneButton;
    private ImageButton mColorButton;
    private View mDocumentsButton;
    private View mTopPanel;
    private View mColorPicker;
    private ImageButton mColorPickerButton;
    private Button mColorPickerColorOptionButton;
    private Button mColorPickerGrayscaleOptionButton;
    private Button mColorPickerBlackWhiteOptionButton;

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay<DocumentGraphic> mGraphicOverlay;
    private Util.FrameSizeProvider mFrameSizeProvider;
    private boolean mManual = false;

    // helper objects for detecting taps and pinches.
    private GestureDetector gestureDetector;

    private Detector<Document> IDDetector;
    private MediaActionSound sound = new MediaActionSound();

    private boolean isPassport = false;

    private int mDocumentDetected = 0;
    private Point[] lastQuadPoints;
    private final static int AUTO_SCAN_THRESHOLD = 3;
    private final static double MATCHING_THRESHOLD_SQUARED = 50.0 * 50.0;

    public static DocumentScannerFragment instantiate(boolean isPassport, boolean showFlash, boolean disableAutomaticCapture, int colorType) {
        DocumentScannerFragment fragment = new DocumentScannerFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_PASSPORT, isPassport);
        args.putBoolean(ARG_SHOW_FLASH, showFlash);
        args.putBoolean(ARG_DISABLE_AUTOMATIC_CAPTURE, disableAutomaticCapture);
        args.putInt(ARG_COLOR_TYPE, colorType);
        fragment.setArguments(args);
        return fragment;
    }

    public static DocumentScannerFragment instantiate(boolean isPassport, boolean showFlash, boolean disableAutomaticCapture, int colorType,
                                                      @ColorRes int docBorderColorRes, @ColorRes int docBodyColorRes, @ColorRes int torchColor, @ColorRes int torchColorLight) {
        DocumentScannerFragment fragment = new DocumentScannerFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_PASSPORT, isPassport);
        args.putBoolean(ARG_SHOW_FLASH, showFlash);
        args.putBoolean(ARG_DISABLE_AUTOMATIC_CAPTURE, disableAutomaticCapture);
        args.putInt(ARG_COLOR_TYPE, colorType);
        args.putInt(ARG_DOC_BODY_COLOR, docBodyColorRes);
        args.putInt(ARG_DOC_BORDER_COLOR, docBorderColorRes);
        args.putInt(ARG_TORCH_COLOR, torchColor);
        args.putInt(ARG_TORCH_COLOR_LIGHT, torchColorLight);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(context, attrs, savedInstanceState);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.DocumentScannerFragment);

        mTorchTintColor = array.getColor(R.styleable.DocumentScannerFragment_torchTint, mTorchTintColor);
        mTorchTintColorLight = array.getColor(R.styleable.DocumentScannerFragment_torchTintLight, mTorchTintColorLight);
        Log.d("SCANNER-INFLATE", "resolved torch tint colors");

        Resources.Theme theme = context.getTheme();
        TypedValue borderColor = new TypedValue();
        if (theme.resolveAttribute(android.R.attr.colorPrimary, borderColor, true)) {
            Log.d("SCANNER-INFLATE", "resolved border color from theme");
            mDocumentBorderColor = borderColor.resourceId > 0 ? getResources().getColor(borderColor.resourceId) : borderColor.data;
        }

        mDocumentBorderColor = array.getColor(R.styleable.DocumentScannerFragment_documentBorderColor, mDocumentBorderColor);

        TypedValue bodyColor = new TypedValue();
        if (theme.resolveAttribute(android.R.attr.colorPrimaryDark, bodyColor, true)) {
            Log.d("SCANNER-INFLATE", "resolved body color from theme");
            mDocumentBodyColor = bodyColor.resourceId > 0 ? getResources().getColor(bodyColor.resourceId) : bodyColor.data;
        }

        mDocumentBodyColor = array.getColor(R.styleable.DocumentScannerFragment_documentBodyColor, mDocumentBodyColor);

        array.recycle();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scanner_content, container, false);

        initializeViews(view);

        return view;
    }

    void initializeViews(View view){
        mPreview = view.findViewById(R.id.preview);
        mGraphicOverlay = view.findViewById(R.id.graphicOverlay);
        mFlashToggle = view.findViewById(R.id.flash);
        mTakePictureButton = view.findViewById(R.id.takePicture);
        mCancelButton = view.findViewById(R.id.cancel);
        mManualButton = view.findViewById(R.id.manual);
        mDoneButton = view.findViewById(R.id.done);
        mColorButton = view.findViewById(R.id.color);
        mDocumentsButton = view.findViewById(R.id.documents);
        mTopPanel = view.findViewById(R.id.topPanel);
        mColorPicker = view.findViewById(R.id.colorPicker);
        mColorPickerButton = view.findViewById(R.id.colorImage);
        mColorPickerColorOptionButton = view.findViewById(R.id.colorButton);
        mColorPickerGrayscaleOptionButton = view.findViewById(R.id.grayscaleButton);
        mColorPickerBlackWhiteOptionButton = view.findViewById(R.id.blackWhiteButton);
    }

    @Override
    protected void onAfterViewCreated() {
        Bundle args = getArguments();
        isPassport = args != null && args.getBoolean(ARG_IS_PASSPORT, false);
        mShowFlash = args != null && args.getBoolean(ARG_SHOW_FLASH, true);
        mDisableAutomaticCapture = args != null && args.getBoolean(ARG_DISABLE_AUTOMATIC_CAPTURE, false);
        mColorType = args != null ? args.getInt(ARG_COLOR_TYPE, V_COLOR_TYPE_COLOR) : V_COLOR_TYPE_COLOR;

        Resources.Theme theme = getActivity().getTheme();
        TypedValue borderColor = new TypedValue();
        if(theme.resolveAttribute(android.R.attr.colorPrimary, borderColor, true)){
            mDocumentBorderColor = borderColor.resourceId > 0? getResources().getColor(borderColor.resourceId) : borderColor.data;
        }

        TypedValue bodyColor = new TypedValue();
        if(theme.resolveAttribute(android.R.attr.colorPrimaryDark, bodyColor, true)){
            mDocumentBodyColor = bodyColor.resourceId > 0? getResources().getColor(bodyColor.resourceId) : bodyColor.data;
        }

        mDocumentBodyColor = args.getInt(ARG_DOC_BODY_COLOR, mDocumentBodyColor);
        mDocumentBorderColor = args.getInt(ARG_DOC_BORDER_COLOR, mDocumentBorderColor);
        mTorchTintColor = args.getInt(ARG_TORCH_COLOR, mTorchTintColor);
        mTorchTintColorLight = args.getInt(ARG_TORCH_COLOR_LIGHT, mTorchTintColorLight);

        BorderFrameGraphic frameGraphic = new BorderFrameGraphic(mGraphicOverlay, isPassport);
        mFrameSizeProvider = frameGraphic;
        mGraphicOverlay.addFrame(frameGraphic);

        if (mShowFlash) {
            mFlashToggle.setOnClickListener(v -> {
                if (mCameraSource != null) {
                    if (Camera.Parameters.FLASH_MODE_TORCH.equals(mCameraSource.getFlashMode()))
                        mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    else
                        mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    updateFlashButtonColor();
                }
            });
        } else {
            mFlashToggle.setVisibility(View.GONE);
        }

        mTakePictureButton.setOnClickListener(v -> {
            if (mCameraSource != null)
                mCameraSource.takePicture(() -> sound.play(MediaActionSound.SHUTTER_CLICK), this::detectDocumentManually);
        });

        mColorButton.setOnClickListener(v -> {
            mTopPanel.setVisibility(View.GONE);
            mCancelButton.setVisibility(View.GONE);
            mManualButton.setVisibility(View.GONE);
            mColorPicker.setVisibility(View.VISIBLE);
            mColorPickerColorOptionButton.setTextColor(mColorType == V_COLOR_TYPE_COLOR ? Color.YELLOW : Color.WHITE);
            mColorPickerGrayscaleOptionButton.setTextColor(mColorType == V_COLOR_TYPE_GRAYSCALE ? Color.YELLOW : Color.WHITE);
            mColorPickerBlackWhiteOptionButton.setTextColor(mColorType == V_COLOR_TYPE_BLACK_WHITE ? Color.YELLOW : Color.WHITE);
        });

        mColorPickerButton.setOnClickListener(v -> {
            hideColorPicker();
        });

        mColorPickerColorOptionButton.setOnClickListener(v -> {
            hideColorPicker();
            mColorType = V_COLOR_TYPE_COLOR;
        });

        mColorPickerGrayscaleOptionButton.setOnClickListener(v -> {
            hideColorPicker();
            mColorType = V_COLOR_TYPE_GRAYSCALE;
        });

        mColorPickerBlackWhiteOptionButton.setOnClickListener(v -> {
            hideColorPicker();
            mColorType = V_COLOR_TYPE_BLACK_WHITE;
        });

        if (mDisableAutomaticCapture) {
            mManualButton.setVisibility(View.GONE);
        } else {
            mManualButton.setOnClickListener(v -> {
                if (mManual)
                    mManualButton.setText(R.string.automatic);
                else
                    mManualButton.setText(R.string.manual);
                mManual = !mManual;
            });
        }

        mCancelButton.setOnClickListener(v -> {
            getActivity().finish();
        });
    }

    private void hideColorPicker() {
        mTopPanel.setVisibility(View.VISIBLE);
        mCancelButton.setVisibility(View.VISIBLE);
        mManualButton.setVisibility(View.VISIBLE);
        mColorPicker.setVisibility(View.GONE);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            try {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                if (Build.VERSION.SDK_INT >= 26) {
                    ft.setReorderingAllowed(false);
                }
                ft.detach(this).attach(this).commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context.getApplicationContext();
    }

    void updateFlashButtonColor(){
        if(mCameraSource != null){
            int tintColor = mTorchTintColor;

            if(mCameraSource.getFlashMode() == Camera.Parameters.FLASH_MODE_TORCH){
                tintColor = mTorchTintColorLight;
            }

            DrawableCompat.setTint(mFlashToggle.getDrawable(), tintColor);
        }
    }

    @Override
    protected void onOpenCVConnected() {
        createCameraSource();
        startCameraSource();
    }

    @Override
    protected void onOpenCVConnectionFailed() {
        if(mCallback != null) mCallback.onImageProcessingFailed("Could not load OpenCV", null);
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     *
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private void createCameraSource() {
        if (isPassport)
            IDDetector = new PassportDetector(mFrameSizeProvider);
        else
            IDDetector = new DocumentDetector(getContext());

        /*
        DocumentTrackerFactory factory = new DocumentTrackerFactory(mGraphicOverlay, this);
        IDDetector.setProcessor(
                new MultiProcessor.Builder<>(factory).build());*/
        DocumentGraphic graphic = new DocumentGraphic(mGraphicOverlay, null);
        if(mDocumentBorderColor != -1) graphic.setBorderColor(mDocumentBorderColor);
        if(mDocumentBodyColor != -1) graphic.setFillColor(mDocumentBodyColor);

        DocumentProcessor processor = new DocumentProcessor(IDDetector,
                new DocumentTracker(mGraphicOverlay, graphic, this));
        IDDetector.setProcessor(processor);

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        mCameraSource = new CameraSource.Builder(getActivity().getApplicationContext(), IDDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)
                .setFlashMode(mShowFlash ? Camera.Parameters.FLASH_MODE_AUTO : Camera.Parameters.FLASH_MODE_OFF)
                .setRequestedFps(15.0f)
                .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    public void onResume() {
        super.onResume();
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPreview != null) {
            mPreview.release();
        }

        if(sound != null) sound.release();
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e("SCANNER", "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    void processDocument(Document document){
        synchronized (mLock) {
            saveCroppedImage(document.getImage().getBitmap(), document.getImage().getMetadata().getRotation(), document.detectedQuad.points, mColorType);
            isBusy = true;
        }
    }

    private boolean matchLastQuadPoints(Point[] points) {
         for (int n = 0; n < points.length; n++)
            for (int m = 1; m < points.length; m++)
                if (m != n && points[n].x == points[m].x && points[n].y == points[m].y) {
                    // triangle
                    lastQuadPoints = null;
                    return false;
                }

        if (lastQuadPoints == null) {
            lastQuadPoints = points;
            return true;
        }

        if (points.length != lastQuadPoints.length) {
            lastQuadPoints = null;
            return false;
        }

        for (int n = 0; n < points.length; n++) {
            double xDifference = points[n].x - lastQuadPoints[n].x;
            double yDifference = points[n].y - lastQuadPoints[n].y;
            double distanceSquare = xDifference * xDifference + yDifference * yDifference;
            if (distanceSquare > MATCHING_THRESHOLD_SQUARED) {
                lastQuadPoints = null;
                return false;
            }
        }

        lastQuadPoints = points;
        return true;
    }

    @Override
    public void onDocumentDetected(final Document document) {
        Log.d("Scanner", "document detected");
        if (document != null && !mDisableAutomaticCapture && !mManual) {
            if (!matchLastQuadPoints(document.detectedQuad.points)) {
                mDocumentDetected = 0;
            } else if (++mDocumentDetected >= AUTO_SCAN_THRESHOLD) {
                getActivity().runOnUiThread(() -> {
                    if (mCameraSource != null)
                        mCameraSource.stop();
                    processDocument(document);
                });
            }
        }
    }

    void detectDocumentManually(final byte[] data){
        Log.d("Scanner", "detecting document manually");
        new Thread(() -> {
            Bitmap image = BitmapFactory.decodeByteArray(data, 0, data.length);
            if(image != null){
                final SparseArray<Document> docs = IDDetector.detect(new Frame.Builder()
                        .setBitmap(image)
                        .build());

                if (docs != null && docs.size() > 0) {
                    Log.d("Scanner", "detected document manually");
                    final Document document = docs.get(0);
                    getActivity().runOnUiThread(() -> processDocument(document));
                } else {
                    getActivity().finish();
                }
            }
        }).start();
    }
}
