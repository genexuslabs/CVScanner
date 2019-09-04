package devliving.online.cvscanner.browser;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import devliving.online.cvscanner.BaseFragment;
import devliving.online.cvscanner.CVScanner;
import devliving.online.cvscanner.DocumentData;
import devliving.online.cvscanner.FilterType;
import devliving.online.cvscanner.R;
import devliving.online.cvscanner.crop.CropImageActivity;
import devliving.online.cvscanner.util.Util;

import static devliving.online.cvscanner.browser.DocumentBrowserActivity.REQ_CROP_IMAGE;
import static devliving.online.cvscanner.browser.DocumentBrowserActivity.REQ_SCAN;

public class DocumentBrowserFragment extends BaseFragment {
    private TextView mNumbersTextView;
    private View mFiltersPanel;
    private ViewPager mPager;
    private ImagesPagerAdapter mImagesAdapter;
    private boolean mShowFlash;
    private boolean mDisableAutomaticCapture;
    private boolean mAllowFilterSelection;
    private double mAspectRatio;

    private final static String ARG_DATA_LIST = "document_data_list";
    private final static String ARG_SHOW_FLASH = "show_flash";
    private final static String ARG_DISABLE_AUTOMATIC_CAPTURE = "disable_automatic_capture";
    private final static String ARG_ALLOW_FILTER_SELECTION = "allow_filter_selection";
    private final static String ARG_ASPECT_RATIO = "aspect_ratio";

    public static DocumentBrowserFragment instantiate(ArrayList<DocumentData> dataList, boolean showFlash, boolean disableAutomaticCapture, boolean allowFilterSelection, double aspectRatio) {
        DocumentBrowserFragment fragment = new DocumentBrowserFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_DATA_LIST, dataList);
        args.putBoolean(ARG_SHOW_FLASH, showFlash);
        args.putBoolean(ARG_DISABLE_AUTOMATIC_CAPTURE, disableAutomaticCapture);
        args.putBoolean(ARG_ALLOW_FILTER_SELECTION, allowFilterSelection);
        args.putDouble(ARG_ASPECT_RATIO, aspectRatio);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void onOpenCVConnected() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.browser_content, container, false);

        initializeViews(view);

        return view;
    }

    private void initializeViews(View view) {
        mNumbersTextView = view.findViewById(R.id.numbers);
        mFiltersPanel = view.findViewById(R.id.filtersPanel);
        mPager = view.findViewById(R.id.pager);

        Bundle extras = getArguments();
        assert extras != null;
        mDataList = extras.getParcelableArrayList(ARG_DATA_LIST);
        mShowFlash = extras.getBoolean(ARG_SHOW_FLASH, true);
        mDisableAutomaticCapture = extras.getBoolean(ARG_DISABLE_AUTOMATIC_CAPTURE, false);
        mAllowFilterSelection = extras.getBoolean(ARG_ALLOW_FILTER_SELECTION, true);
        mAspectRatio = extras.getDouble(ARG_ASPECT_RATIO, 0);

        view.findViewById(R.id.done).setOnClickListener(this::onDoneClick);
        view.findViewById(R.id.retake).setOnClickListener(this::onRetakeClick);
        view.findViewById(R.id.crop).setOnClickListener(this::onCropClick);
        view.findViewById(R.id.filters).setOnClickListener(this::onFiltersClick);
        view.findViewById(R.id.rotate).setOnClickListener(this::onRotateClick);
        view.findViewById(R.id.erase).setOnClickListener(this::onEraseClick);
        view.findViewById(R.id.color).setOnClickListener(this::onColorClick);
        view.findViewById(R.id.grayscale).setOnClickListener(this::onGrayscaleClick);
        view.findViewById(R.id.blackWhite).setOnClickListener(this::onBlackWhiteClick);
        view.findViewById(R.id.photo).setOnClickListener(this::onPhotoClick);
    }

    @Override
    protected void onAfterViewCreated() {
        mImagesAdapter = new ImagesPagerAdapter(getActivity().getSupportFragmentManager(), mDataList);
        mPager.setAdapter(mImagesAdapter);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                String text = String.format(Locale.US, "%d of %d", position + 1, mImagesAdapter.getCount());
                mNumbersTextView.setText(text);
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });

        String text = String.format(Locale.US, "1 of %d", mImagesAdapter.getCount());
        mNumbersTextView.setText(text);
    }

    void loadCurrentData(DocumentData data) {
        mDataList.set(mPager.getCurrentItem(), data);
        mImagesAdapter.notifyDataSetChanged();
    }

    private static class ImagesPagerAdapter extends FragmentStatePagerAdapter {
        private ArrayList<DocumentData> mDataList;

        ImagesPagerAdapter(FragmentManager fm, ArrayList<DocumentData> dataList) {
            super(fm);
            mDataList = dataList;
        }

        @Override
        public Fragment getItem(int position) {
            DocumentData data = getData(position);
            return ImageFragment.instantiate(data.getImageUri());
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            ImageFragment f = ((ImageFragment)object);
            String imageUri = f.getImageUri();
            for (DocumentData data : mDataList)
                if (data.getImageUri().toString().equals(imageUri))
                    return POSITION_UNCHANGED;
            return POSITION_NONE; // so it reloads after Cropping or Rotate
        }

        @Override
        public int getCount() {
            return mDataList.size();
        }

        DocumentData getData(int position) {
            return mDataList.get(position);
        }

        void setFilterType(int position, FilterType filterType) {
            getData(position).setFilterType(filterType);
        }

        void rotate(int position) {
            getData(position).rotate(1);
            notifyDataSetChanged();
        }

        void remove(int position) {
            if (position < mDataList.size()) {
                mDataList.remove(position);
                notifyDataSetChanged();
            }
        }
    }

    public static class ImageFragment extends Fragment {
        private final static String ARG_IMAGE_PATH = "image_path";

        static Fragment instantiate(Uri imageUri) {
            Fragment fragment = new ImageFragment();
            Bundle args = new Bundle();
            args.putString(ARG_IMAGE_PATH, imageUri.toString());
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            AppCompatImageView imageView = new AppCompatImageView(Objects.requireNonNull(getContext()));
            Uri imageUri = Uri.parse(getImageUri());
            imageView.setImageURI(imageUri);
            return imageView;
        }

        String getImageUri() {
            assert getArguments() != null;
            return getArguments().getString(ARG_IMAGE_PATH);
        }
    }

    private void onDoneClick(View v) {
        done();
    }

    private void onRetakeClick(View v) {
        DocumentData data = mDataList.get(mPager.getCurrentItem());
        CVScanner.startScanner(getActivity(), false, mShowFlash, mDisableAutomaticCapture, data.getFilterType(), mAllowFilterSelection, mAspectRatio, true, 0, REQ_SCAN);
    }

    private void setFilterType(FilterType filterType) {
        mImagesAdapter.setFilterType(mPager.getCurrentItem(), filterType);
        updateImage();
    }

    private void updateImage() {
        DocumentData data = mDataList.get(mPager.getCurrentItem());
        try {
            data.setOriginalImage(Util.loadBitmapFromUri(Objects.requireNonNull(getContext()), 1, data.getOriginalImageUri()));
            saveCroppedImage(data);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void onColorClick(View v) {
        setFilterType(FilterType.Color);
    }

    private void onGrayscaleClick(View v) {
        setFilterType(FilterType.Grayscale);
    }

    private void onBlackWhiteClick(View v) {
        setFilterType(FilterType.BlackWhite);
    }

    private void onPhotoClick(View v) {
        setFilterType(FilterType.Photo);
    }

    private void onFiltersClick(View v) {
        if (mFiltersPanel.getVisibility() == View.GONE)
            mFiltersPanel.setVisibility(View.VISIBLE);
        else
            mFiltersPanel.setVisibility(View.GONE);
    }

    private void onCropClick(View v) {
        DocumentData data = mImagesAdapter.getData(mPager.getCurrentItem());
        Intent intent = new Intent(getContext(), CropImageActivity.class);
        intent.putExtra(CropImageActivity.EXTRA_DATA, data);
        getActivity().startActivityForResult(intent, REQ_CROP_IMAGE);
    }

    private void onRotateClick(View v) {
        mImagesAdapter.rotate(mPager.getCurrentItem());
        updateImage();
    }

    private void onEraseClick(View v) {
        mImagesAdapter.remove(mPager.getCurrentItem());
        if (mImagesAdapter.getCount() == 0)
            done();
    }

    @Override
    public void onSaved(String path) {
        super.onSaved(path);
        mImagesAdapter.notifyDataSetChanged();
    }
}
