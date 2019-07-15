package devliving.online.cvscanner.browser;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;

import devliving.online.cvscanner.BaseFragment;
import devliving.online.cvscanner.CVScanner;
import devliving.online.cvscanner.DocumentData;
import devliving.online.cvscanner.R;

import static devliving.online.cvscanner.CVScanner.RESULT_DATA_LIST;

public class DocumentBrowserActivity extends FragmentActivity implements BaseFragment.ImageProcessorCallback {
    public static String EXTRA_DATA_LIST = "document_data_list";

    public static final int REQ_SCAN = 11;
    public static final int REQ_CROP_IMAGE = 13;

    private DocumentBrowserFragment mFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scanner_activity);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (getSupportFragmentManager().getFragments() == null || getSupportFragmentManager().getFragments().size() == 0)
            addBrowserFragment();
    }

    private void addBrowserFragment() {
        ArrayList<DocumentData> dataList = getIntent().getParcelableArrayListExtra(EXTRA_DATA_LIST);
        if (dataList == null) {
            setResult(RESULT_CANCELED);
            finish();
        } else {
            mFragment = DocumentBrowserFragment.instantiate(dataList);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, mFragment)
                    .commitAllowingStateLoss();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQ_SCAN: {
                    DocumentData documentData = data.getParcelableExtra(CVScanner.RESULT_DATA);
                    mFragment.loadCurrentData(documentData);
                    break;
                }
                case REQ_CROP_IMAGE: {
                    DocumentData documentData = data.getParcelableExtra(CVScanner.RESULT_DATA);
                    mFragment.loadCurrentData(documentData);
                    break;
                }
            }
        }
    }

    private void setResultAndExit(ArrayList<DocumentData> dataList) {
        Intent data = getIntent();
        data.putExtra(RESULT_DATA_LIST, dataList);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onImageProcessingFailed(String reason, @Nullable Exception error) {
        Toast.makeText(this, "Scanner failed: " + reason, Toast.LENGTH_SHORT).show();
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onImagesProcessed(ArrayList<DocumentData> dataList) {
        setResultAndExit(dataList);
    }
}
