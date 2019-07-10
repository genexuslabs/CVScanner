package devliving.online.cvscanner.browser;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.Locale;

import devliving.online.cvscanner.CVScanner;
import devliving.online.cvscanner.DocumentData;
import devliving.online.cvscanner.R;
import devliving.online.cvscanner.scanner.DocumentScannerActivity;

import static devliving.online.cvscanner.DocumentData.V_FILTER_TYPE_BLACK_WHITE;
import static devliving.online.cvscanner.DocumentData.V_FILTER_TYPE_COLOR;
import static devliving.online.cvscanner.DocumentData.V_FILTER_TYPE_GRAYSCALE;
import static devliving.online.cvscanner.DocumentData.V_FILTER_TYPE_PHOTO;

public class DocumentBrowserActivity extends FragmentActivity {
    private View mFiltersPanel;
    private ViewPager mPager;
    private ImagesPagerAdapter mImagesAdapter;

    public static String EXTRA_DATA_LIST = "document_data_list";

    public static final int REQ_CROP_IMAGE = 13;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser_activity);

        ArrayList<DocumentData> dataList = getIntent().getParcelableArrayListExtra(EXTRA_DATA_LIST);

        final TextView numbersTextView = findViewById(R.id.numbers);
        mFiltersPanel = findViewById(R.id.filtersPanel);
        mPager = findViewById(R.id.pager);

        mImagesAdapter = new ImagesPagerAdapter(getSupportFragmentManager(), dataList);
        mPager.setAdapter(mImagesAdapter);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                String text = String.format(Locale.US, "%d of %d", position + 1, mImagesAdapter.getCount());
                numbersTextView.setText(text);
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });

        String text = String.format(Locale.US, "1 of %d", mImagesAdapter.getCount());
        numbersTextView.setText(text);
    }

    private static class ImagesPagerAdapter extends FragmentStatePagerAdapter {
        private ArrayList<DocumentData> mDataList;

        public ImagesPagerAdapter(FragmentManager fm, ArrayList<DocumentData> dataList) {
            super(fm);
            mDataList = dataList;
        }

        @Override
        public Fragment getItem(int position) {
            return ImageFragment.instantiate(mDataList.get(position).getImageUri());
        }

        @Override
        public int getCount() {
            return mDataList.size();
        }

        DocumentData getData(int position) {
            return mDataList.get(position);
        }

        void setFilterType(int position, int filterType) {
            mDataList.get(position).setFilterType(filterType);
            notifyDataSetChanged();
        }

        void rotate(int position) {
            DocumentData data = mDataList.get(position);
            data.rotate(1);
            notifyDataSetChanged();
        }

        void remove(int position) {
            mDataList.remove(position);
            notifyDataSetChanged();
        }
    }

    public static class ImageFragment extends Fragment {
        private final static String ARG_IMAGE_PATH = "image_path";

        public static Fragment instantiate(Uri imageUri) {
            Fragment fragment = new ImageFragment();
            Bundle args = new Bundle();
            args.putString(ARG_IMAGE_PATH, imageUri.toString());
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            String imageUri = getArguments().getString(ARG_IMAGE_PATH);
            ImageView imageView = new ImageView(getContext());
            imageView.setImageURI(Uri.parse(imageUri));
            return imageView;
        }
    }

    public void onDoneClick(View v) {
        finish();
    }

    public void onRetakeClick(View v) {
        Intent intent = new Intent(getApplicationContext(), DocumentScannerActivity.class);
        startActivity(intent);
    }

    private void setFilterType(int filterType) {
        mImagesAdapter.setFilterType(mPager.getCurrentItem(), filterType);
    }

    public void onColorClick(View v) {
        setFilterType(V_FILTER_TYPE_COLOR);
    }

    public void onGrayscaleClick(View v) {
        setFilterType(V_FILTER_TYPE_GRAYSCALE);
    }

    public void onBlackWhiteClick(View v) {
        setFilterType(V_FILTER_TYPE_BLACK_WHITE);
    }

    public void onPhotoClick(View v) {
        setFilterType(V_FILTER_TYPE_PHOTO);
    }

    public void onFiltersClick(View v) {
        if (mFiltersPanel.getVisibility() == View.GONE)
            mFiltersPanel.setVisibility(View.VISIBLE);
        else
            mFiltersPanel.setVisibility(View.GONE);
    }

    public void onCropClick(View v) {
        DocumentData data = mImagesAdapter.getData(mPager.getCurrentItem());
        CVScanner.startManualCropper(this, data, REQ_CROP_IMAGE);
    }

    public void onRotateClick(View v) {
        mImagesAdapter.rotate(mPager.getCurrentItem());
    }

    public void onEraseClick(View v) {
        mImagesAdapter.remove(mPager.getCurrentItem());
    }
}
