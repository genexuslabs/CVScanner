package devliving.online.cvscanner.browser;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import devliving.online.cvscanner.BaseFragment;
import devliving.online.cvscanner.CVScanner;
import devliving.online.cvscanner.Document;
import devliving.online.cvscanner.DocumentData;
import devliving.online.cvscanner.R;
import devliving.online.cvscanner.crop.CropImageActivity;
import devliving.online.cvscanner.scanner.DocumentScannerActivity;
import devliving.online.cvscanner.util.Util;

import static devliving.online.cvscanner.DocumentData.V_FILTER_TYPE_BLACK_WHITE;
import static devliving.online.cvscanner.DocumentData.V_FILTER_TYPE_COLOR;
import static devliving.online.cvscanner.DocumentData.V_FILTER_TYPE_GRAYSCALE;
import static devliving.online.cvscanner.DocumentData.V_FILTER_TYPE_PHOTO;
import static devliving.online.cvscanner.browser.DocumentBrowserActivity.REQ_CROP_IMAGE;
import static devliving.online.cvscanner.browser.DocumentBrowserActivity.REQ_SCAN;

public class DocumentBrowserFragment extends BaseFragment {
    private TextView mNumbersTextView;
    private View mFiltersPanel;
    private ViewPager mPager;
    private ImagesPagerAdapter mImagesAdapter;

    private static String ARG_DATA_LIST = "document_data_list";

    public static DocumentBrowserFragment instantiate(ArrayList<DocumentData> dataList) {
        DocumentBrowserFragment fragment = new DocumentBrowserFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_DATA_LIST, dataList);
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

        void setFilterType(int position, int filterType) {
            getData(position).setFilterType(filterType);
        }

        void rotate(int position) {
            getData(position).rotate(1);
            notifyDataSetChanged();
        }

        void remove(int position) {
            mDataList.remove(position);
            notifyDataSetChanged();
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
        CVScanner.startScanner(getActivity(), false, true, false, data.getFilterType(), true, REQ_SCAN);
    }

    private void setFilterType(int filterType) {
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
        setFilterType(V_FILTER_TYPE_COLOR);
    }

    private void onGrayscaleClick(View v) {
        setFilterType(V_FILTER_TYPE_GRAYSCALE);
    }

    private void onBlackWhiteClick(View v) {
        setFilterType(V_FILTER_TYPE_BLACK_WHITE);
    }

    private void onPhotoClick(View v) {
        setFilterType(V_FILTER_TYPE_PHOTO);
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
