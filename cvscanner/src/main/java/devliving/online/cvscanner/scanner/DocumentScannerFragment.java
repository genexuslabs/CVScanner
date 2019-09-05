package devliving.online.cvscanner.scanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.common.images.Size;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;

import org.opencv.core.Point;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import devliving.online.cvscanner.BaseFragment;
import devliving.online.cvscanner.Document;
import devliving.online.cvscanner.FilterType;
import devliving.online.cvscanner.browser.DocumentBrowserActivity;
import devliving.online.cvscanner.DocumentData;
import devliving.online.cvscanner.R;
import devliving.online.cvscanner.util.CVProcessor;
import online.devliving.mobilevisionpipeline.GraphicOverlay;
import online.devliving.mobilevisionpipeline.Util;
import online.devliving.mobilevisionpipeline.camera.CameraSource;
import online.devliving.mobilevisionpipeline.camera.CameraSourcePreview;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static devliving.online.cvscanner.browser.DocumentBrowserActivity.EXTRA_DATA_LIST;
import static devliving.online.cvscanner.scanner.DocumentScannerActivity.EXTRA_ALLOW_FILTER_SELECTION;
import static devliving.online.cvscanner.scanner.DocumentScannerActivity.EXTRA_ASPECT_RATIO;
import static devliving.online.cvscanner.scanner.DocumentScannerActivity.EXTRA_DISABLE_AUTOMATIC_CAPTURE;
import static devliving.online.cvscanner.scanner.DocumentScannerActivity.EXTRA_SHOW_FLASH;
import static devliving.online.cvscanner.scanner.DocumentScannerActivity.REQ_DOCUMENT_BROWSE;

/**
 * Created by Mehedi on 10/23/16.
 */
public class DocumentScannerFragment extends BaseFragment implements DocumentTracker.DocumentDetectionListener {
    private final static String ARG_IS_PASSPORT = "is_passport";
    private final static String ARG_SHOW_FLASH = "show_flash";
    private final static String ARG_DISABLE_AUTOMATIC_CAPTURE = "disable_automatic_capture";
    private final static String ARG_FILTER_TYPE = "filter_type";
    private final static String ARG_ALLOW_FILTER_SELECTION = "allow_filter_selection";
    private final static String ARG_ASPECT_RATIO = "aspect_ratio";
    private final static String ARG_SINGLE_DOCUMENT = "single_document";
    private final static String ARG_MAXIMUM_SCANS = "maximum_scans";
    private final static String ARG_TORCH_COLOR = "torch_color";
    private final static String ARG_TORCH_COLOR_LIGHT = "torch_color_light";
    private final static String ARG_DOC_BORDER_COLOR = "doc_border_color";
    private final static String ARG_DOC_BODY_COLOR = "doc_body_color";

    private final Object mLock = new Object();

    private int mTorchTintColor = Color.GRAY, mTorchTintColorLight = Color.YELLOW;
    private int mDocumentBorderColor = -1, mDocumentBodyColor = -1;

    private boolean mShowFlash;
    private ImageButton mFlashToggle;
    private boolean mDisableAutomaticCapture;
    private FilterType mFilterType;
    private boolean mAllowFilterSelection;
    private double mAspectRatio;
    private boolean mSingleDocument;
    private int mMaximumScans;

    private ImageButton mTakePictureButton;
    private Button mCancelButton;
    private Button mManualButton;
    private Button mDoneButton;
    private ImageButton mFiltersButton;
    private ImageButton mDocumentsButton;
    private View mTopPanel;
    private View mFiltersPanel;
    private ImageButton mFiltersCloseButton;
    private Button mFilterColorButton;
    private Button mFilterGrayscaleButton;
    private Button mFilterBlackWhiteButton;
    private AppCompatImageView mAnimationImage;

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

    public static DocumentScannerFragment instantiate(boolean isPassport, boolean showFlash, boolean disableAutomaticCapture, FilterType filterType, boolean allowFilterSelection, double aspectRatio, boolean singleDocument, int maximumScans) {
        DocumentScannerFragment fragment = new DocumentScannerFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_PASSPORT, isPassport);
        args.putBoolean(ARG_SHOW_FLASH, showFlash);
        args.putBoolean(ARG_DISABLE_AUTOMATIC_CAPTURE, disableAutomaticCapture);
        args.putInt(ARG_FILTER_TYPE, filterType.ordinal());
        args.putBoolean(ARG_ALLOW_FILTER_SELECTION, allowFilterSelection);
        args.putDouble(ARG_ASPECT_RATIO, aspectRatio);
        args.putBoolean(ARG_SINGLE_DOCUMENT, singleDocument);
        args.putInt(ARG_MAXIMUM_SCANS, maximumScans);
        fragment.setArguments(args);
        return fragment;
    }

    public static DocumentScannerFragment instantiate(boolean isPassport, boolean showFlash, boolean disableAutomaticCapture, FilterType filterType, boolean allowFilterSelection, double aspectRatio, boolean singleDocument, int maximumScans,
                                                      @ColorRes int docBorderColorRes, @ColorRes int docBodyColorRes, @ColorRes int torchColor, @ColorRes int torchColorLight) {
        DocumentScannerFragment fragment = new DocumentScannerFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_PASSPORT, isPassport);
        args.putBoolean(ARG_SHOW_FLASH, showFlash);
        args.putBoolean(ARG_DISABLE_AUTOMATIC_CAPTURE, disableAutomaticCapture);
        args.putInt(ARG_FILTER_TYPE, filterType.ordinal());
        args.putBoolean(ARG_ALLOW_FILTER_SELECTION, allowFilterSelection);
        args.putDouble(ARG_ASPECT_RATIO, aspectRatio);
        args.putBoolean(ARG_SINGLE_DOCUMENT, singleDocument);
        args.putInt(ARG_MAXIMUM_SCANS, maximumScans);
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scanner_content, container, false);

        initializeViews(view);

        return view;
    }

    private void initializeViews(View view) {
        mPreview = view.findViewById(R.id.preview);
        mGraphicOverlay = view.findViewById(R.id.graphicOverlay);
        mFlashToggle = view.findViewById(R.id.flash);
        mTakePictureButton = view.findViewById(R.id.takePicture);
        mCancelButton = view.findViewById(R.id.cancel);
        mManualButton = view.findViewById(R.id.manual);
        mDoneButton = view.findViewById(R.id.done);
        mFiltersButton = view.findViewById(R.id.filters);
        mDocumentsButton = view.findViewById(R.id.documents);
        mTopPanel = view.findViewById(R.id.topPanel);
        mFiltersPanel = view.findViewById(R.id.filtersPanel);
        mFiltersCloseButton = view.findViewById(R.id.filterClose);
        mFilterColorButton = view.findViewById(R.id.color);
        mFilterGrayscaleButton = view.findViewById(R.id.grayscale);
        mFilterBlackWhiteButton = view.findViewById(R.id.blackWhite);
        mAnimationImage = view.findViewById(R.id.animationImage);
    }

    @Override
    protected void onAfterViewCreated() {
        Bundle args = getArguments();
        isPassport = args != null && args.getBoolean(ARG_IS_PASSPORT, false);
        mShowFlash = args == null || args.getBoolean(ARG_SHOW_FLASH, true);
        mDisableAutomaticCapture = args != null && args.getBoolean(ARG_DISABLE_AUTOMATIC_CAPTURE, false);
        mFilterType = args != null ? FilterType.values()[args.getInt(ARG_FILTER_TYPE, FilterType.Color.ordinal())] : FilterType.Color;
        mAllowFilterSelection = args == null || args.getBoolean(ARG_ALLOW_FILTER_SELECTION, true);
        mAspectRatio = args != null ? args.getDouble(ARG_ASPECT_RATIO, 0) : 0;
        mSingleDocument = args != null && args.getBoolean(ARG_SINGLE_DOCUMENT, true);
        mMaximumScans = args != null ? args.getInt(ARG_MAXIMUM_SCANS, 0) : 0;

        Resources.Theme theme = Objects.requireNonNull(getActivity()).getTheme();
        TypedValue borderColor = new TypedValue();
        if (theme.resolveAttribute(android.R.attr.colorPrimary, borderColor, true)) {
            mDocumentBorderColor = borderColor.resourceId > 0? getResources().getColor(borderColor.resourceId) : borderColor.data;
        }

        TypedValue bodyColor = new TypedValue();
        if (theme.resolveAttribute(android.R.attr.colorPrimaryDark, bodyColor, true)) {
            mDocumentBodyColor = bodyColor.resourceId > 0? getResources().getColor(bodyColor.resourceId) : bodyColor.data;
        }

        assert args != null;
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
            mFlashToggle.setVisibility(GONE);
        }

        mTakePictureButton.setOnClickListener(v -> {
            mTakePictureButton.setVisibility(GONE);
            if (mCameraSource != null)
                mCameraSource.takePicture(() -> sound.play(MediaActionSound.SHUTTER_CLICK), this::detectDocumentManually);
        });

        if (!mAllowFilterSelection) {
            mFiltersButton.setVisibility(GONE);
        } else {
            mFiltersButton.setOnClickListener(v -> {
                mTopPanel.setVisibility(GONE);
                mCancelButton.setVisibility(GONE);
                mManualButton.setVisibility(GONE);
                mFiltersPanel.setVisibility(VISIBLE);
                mFilterColorButton.setTextColor(mFilterType == FilterType.Color ? Color.YELLOW : Color.WHITE);
                mFilterGrayscaleButton.setTextColor(mFilterType == FilterType.Grayscale ? Color.YELLOW : Color.WHITE);
                mFilterBlackWhiteButton.setTextColor(mFilterType == FilterType.BlackWhite ? Color.YELLOW : Color.WHITE);
            });

            mFiltersCloseButton.setOnClickListener(v -> {
                hideColorPicker();
            });

            mFilterColorButton.setOnClickListener(v -> {
                hideColorPicker();
                mFilterType = FilterType.Color;
            });

            mFilterGrayscaleButton.setOnClickListener(v -> {
                hideColorPicker();
                mFilterType = FilterType.Grayscale;
            });

            mFilterBlackWhiteButton.setOnClickListener(v -> {
                hideColorPicker();
                mFilterType = FilterType.BlackWhite;
            });
        }

        if (mDisableAutomaticCapture) {
            mManualButton.setVisibility(GONE);
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

        if (!mSingleDocument) {
            mDocumentsButton.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), DocumentBrowserActivity.class);
                intent.putParcelableArrayListExtra(EXTRA_DATA_LIST, mDataList);
                intent.putExtra(EXTRA_SHOW_FLASH, mShowFlash);
                intent.putExtra(EXTRA_DISABLE_AUTOMATIC_CAPTURE, mDisableAutomaticCapture);
                intent.putExtra(EXTRA_ALLOW_FILTER_SELECTION, mAllowFilterSelection);
                intent.putExtra(EXTRA_ASPECT_RATIO, mAspectRatio);
                getActivity().startActivityForResult(intent, REQ_DOCUMENT_BROWSE);
            });

            mDoneButton.setOnClickListener(v -> {
                done();
            });

            if (mDataList != null && mDataList.size() > 0) {
                mDocumentsButton.setVisibility(VISIBLE);
                mDoneButton.setVisibility(VISIBLE);
                mDocumentsButton.setImageURI(mDataList.get(mDataList.size() - 1).getImageUri());
            }
        }
    }

    private void setButtonVisibility(int visible) {
        if (mShowFlash)
            mFlashToggle.setVisibility(visible);
        if (mAllowFilterSelection)
            mFiltersButton.setVisibility(visible);
        if (!mDisableAutomaticCapture)
            mManualButton.setVisibility(visible);
    }

    private void hideColorPicker() {
        mTopPanel.setVisibility(VISIBLE);
        mCancelButton.setVisibility(VISIBLE);
        mManualButton.setVisibility(VISIBLE);
        mFiltersPanel.setVisibility(GONE);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            try {
                assert getFragmentManager() != null;
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    ft.setReorderingAllowed(false);
                ft.detach(this).attach(this).commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateFlashButtonColor(){
        if (mCameraSource != null) {
            int tintColor = mTorchTintColor;

            if (mCameraSource.getFlashMode() == Camera.Parameters.FLASH_MODE_TORCH)
                tintColor = mTorchTintColorLight;

            DrawableCompat.setTint(mFlashToggle.getDrawable(), tintColor);
        }
    }

    @Override
    protected void onOpenCVConnected() {
        createCameraSource();
        startCameraSource();
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
            IDDetector = new DocumentDetector(mAspectRatio, getContext());

        /*
        DocumentTrackerFactory factory = new DocumentTrackerFactory(mGraphicOverlay, this);
        IDDetector.setProcessor(
                new MultiProcessor.Builder<>(factory).build());*/

        DocumentGraphic graphic = new DocumentGraphic(mGraphicOverlay, null);
        if (mDocumentBorderColor != -1)
            graphic.setBorderColor(mDocumentBorderColor);

        if (mDocumentBodyColor != -1)
            graphic.setFillColor(mDocumentBodyColor);

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

        if (sound != null)
            sound.release();
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

    private void processDocument(Document document, boolean manual) {
        DocumentData data = DocumentData.Create(getContext(), document, mFilterType);
        if (manual) {
            double scaleFactor;
            final int screenRotation = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
            if (DocumentGraphic.isPortrait(screenRotation))
                scaleFactor = mCameraSource.getPreviewSize().getWidth() / (double)data.getHeight();
            else
                scaleFactor = mCameraSource.getPreviewSize().getHeight() / (double)data.getHeight();
            Point[] points = CVProcessor.getUpscaledPoints(data.getPoints(), scaleFactor);
            data.setPreviewPoints(points);
        }
        processDocumentData(data);
    }

    private void processImage(Bitmap image) {
        processDocumentData(DocumentData.Create(getContext(), image, mFilterType));
    }

    private void processDocumentData(DocumentData data) {
        synchronized (mLock) {
            if (data != null) {
                if (mMaximumScans == 0 || mDataList == null || mDataList.size() < mMaximumScans) {
                    addDocument(data);
                    saveCroppedImage(data);
                }
                if (mMaximumScans > 0 && mDataList != null && mDataList.size() >= mMaximumScans) {
                    mTakePictureButton.setVisibility(GONE);
                    setButtonVisibility(GONE);
                }
            }
        }
    }

    @Override
    public void onSaved(String path) {
        super.onSaved(path);
        if (mSingleDocument)
            done();
        else {
            // Add animation
            final Bitmap bitmap = BitmapFactory.decodeFile(path);

            float centerX, centerY, rotation;
            float scaleStart, scaleMiddle, scaleEnd;
            FrameLayout.LayoutParams documentButtonParams = (FrameLayout.LayoutParams) mDocumentsButton.getLayoutParams();

            Point center = new Point();
            boolean needTransformCenter = false;
            Point[] points = mDataList.get(mDataList.size() - 1).getPreviewPoints(); // Manually captured
            if (points == null) {
                points = mDataList.get(mDataList.size() - 1).getPoints();
                needTransformCenter = true;
            }
            if (points.length > 0) {
                for (Point point : points) {
                    center.x += point.x;
                    center.y += point.y;
                }
                center.x /= points.length;
                center.y /= points.length;

                if (needTransformCenter) {
                    final int screenRotation = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
                    Size cameraSize = mCameraSource.getPreviewSize();
                    if (DocumentGraphic.isPortrait(screenRotation))
                        cameraSize = new Size(cameraSize.getHeight(), cameraSize.getWidth());

                    centerX = DocumentGraphic.getX(mPreview.getHeight(), mPreview.getWidth(), screenRotation, center) * mPreview.getWidth() / (float) cameraSize.getWidth();
                    centerY = DocumentGraphic.getY(mPreview.getHeight(), mPreview.getWidth(), screenRotation, center) * mPreview.getHeight() / (float) cameraSize.getHeight();
                } else {
                    centerX = (float)center.x;
                    centerY = (float)center.y;
                }

                int captureWidth = (int) (Math.max(points[1].x, points[2].x) - Math.min(points[0].x, points[3].x));
                int captureHeight = (int) (Math.max(points[2].y, points[3].y) - Math.min(points[0].y, points[1].y));
                Size captureSize = new Size(captureHeight, captureWidth);

                rotation = 0;
                if (points[0].x != points[1].x)
                    rotation = (float) Math.toDegrees(Math.atan((points[1].y - points[0].y) / (points[1].x - points[0].x)));

                scaleStart = (captureSize.getWidth() / (float) bitmap.getWidth() + captureSize.getHeight() / (float) bitmap.getHeight()) / 2f;
                scaleMiddle = Math.min(mPreview.getWidth() * .9f / bitmap.getWidth(), mPreview.getHeight() * .7f / bitmap.getHeight());
            } else {
                centerX = mPreview.getWidth() / 2f;
                centerY = mPreview.getHeight() / 2f;
                rotation = 0;
                scaleStart = .8f;
                scaleMiddle = .9f;
            }
            scaleEnd = Math.max(documentButtonParams.width / (float) bitmap.getWidth(), documentButtonParams.height / (float) bitmap.getHeight());

            mAnimationImage.setVisibility(VISIBLE);
            mAnimationImage.setImageBitmap(bitmap);
            mAnimationImage.setTranslationX(centerX - mPreview.getWidth() / 2f);
            mAnimationImage.setTranslationY(centerY - mPreview.getHeight() / 2f);
            mAnimationImage.setScaleX(scaleStart);
            mAnimationImage.setScaleY(scaleStart);
            mAnimationImage.setRotation(rotation);

            mAnimationImage.animate()
                    .translationX(0)
                    .translationY(0)
                    .scaleX(scaleMiddle)
                    .scaleY(scaleMiddle)
                    .rotation(0)
                    .setDuration(400)
                    .withEndAction(() -> mAnimationImage.animate()
                            .translationX((documentButtonParams.leftMargin + documentButtonParams.width / 2f) - mPreview.getWidth() / 2f)
                            .translationY((mPreview.getHeight() - documentButtonParams.bottomMargin - documentButtonParams.height / 2f) - mPreview.getHeight() / 2f)
                            .scaleX(scaleEnd)
                            .scaleY(scaleEnd)
                            .setDuration(400)
                            .withEndAction(() -> {
                                // Enable buttons after animation
                                mDocumentsButton.setVisibility(VISIBLE);
                                mDoneButton.setVisibility(VISIBLE);
                                if (mDataList.size() < mMaximumScans)
                                    mTakePictureButton.setVisibility(VISIBLE);
                                mDocumentsButton.setImageBitmap(bitmap);
                                mAnimationImage.setVisibility(GONE);
                            }));
        }
    }

    private void addDocument(DocumentData data) {
        if (mDataList == null)
            mDataList = new ArrayList<>();
        mDataList.add(data);
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
        if (document != null && !mDisableAutomaticCapture && !mManual && !mIsBusy) {
            if (!matchLastQuadPoints(document.getDetectedQuad().points)) {
                mDocumentDetected = 0;
            } else if (mDocumentDetected != -1 && ++mDocumentDetected >= AUTO_SCAN_THRESHOLD) {
                mDocumentDetected = -1; // Don't process twice
                sound.play(MediaActionSound.SHUTTER_CLICK);
                assert getActivity() != null;
                getActivity().runOnUiThread(() -> {
                    processDocument(document, false);
                });
            }
        }
    }

    private void detectDocumentManually(final byte[] data) {
        Log.d("Scanner", "detecting document manually");
        new Thread(() -> {
            Bitmap image = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (image != null) {
                final SparseArray<Document> docs = IDDetector.detect(new Frame.Builder()
                        .setBitmap(image)
                        .build());

                assert getActivity() != null;
                if (docs != null && docs.size() > 0) {
                    Log.d("Scanner", "detected document manually");
                    final Document document = docs.get(0);
                    getActivity().runOnUiThread(() -> processDocument(document, true));
                } else {
                    getActivity().runOnUiThread(() -> processImage(image));
                }
            }
        }).start();
    }

    void setDataList(ArrayList<DocumentData> dataList) {
        mDataList = dataList;
        if (dataList.size() == 0) {
            mDocumentsButton.setVisibility(GONE);
            mDoneButton.setVisibility(GONE);
        } else {
            mDocumentsButton.setImageURI(dataList.get(dataList.size() - 1).getImageUri());
        }
        if (mMaximumScans != 0 && dataList.size() < mMaximumScans) {
            mTakePictureButton.setVisibility(VISIBLE);
            setButtonVisibility(VISIBLE);
        }
    }
}
