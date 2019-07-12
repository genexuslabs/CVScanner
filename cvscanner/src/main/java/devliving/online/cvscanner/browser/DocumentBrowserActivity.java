package devliving.online.cvscanner.browser;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;

import devliving.online.cvscanner.BaseFragment;
import devliving.online.cvscanner.DocumentData;
import devliving.online.cvscanner.R;

public class DocumentBrowserActivity extends FragmentActivity implements BaseFragment.ImageProcessorCallback {
    public static String EXTRA_DATA_LIST = "document_data_list";
    public static String RESULT_DATA_LIST = "result_data_list";

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
            Fragment fragment = DocumentBrowserFragment.instantiate(dataList);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .commitAllowingStateLoss();
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
