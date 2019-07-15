/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package devliving.online.cvscanner.crop;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import devliving.online.cvscanner.BaseFragment;
import devliving.online.cvscanner.DocumentData;
import devliving.online.cvscanner.R;

import static devliving.online.cvscanner.CVScanner.RESULT_DATA;
import static devliving.online.cvscanner.CVScanner.RESULT_IMAGE_PATH;

/**
 * The activity can crop specific region of interest from an image.
 */
public class CropImageActivity extends AppCompatActivity implements BaseFragment.ImageProcessorCallback {
    public static final String EXTRA_DATA = "input_data";

    public static final String EXTRA_ROTATE_LEFT_IMAGE_RES = "rotateLeft_imageRes";
    public static final String EXTRA_SAVE_IMAGE_RES = "save_imageRes";
    public static final String EXTRA_ROTATE_RIGHT_IMAGE_RES = "rotateRight_imageRes";

    public static final String EXTRA_SAVE_BTN_COLOR_RES = "save_imageColorRes";
    public static final String EXTRA_ROTATE_BTN_COLOR_RES = "rotate_imageColorRes";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        setContentView(R.layout.scanner_activity);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (getSupportFragmentManager().getFragments() == null || getSupportFragmentManager().getFragments().size() == 0)
            addCropperFragment();
    }

    private void addCropperFragment(){
        DocumentData data = null;
        Bundle extras = getIntent().getExtras();
        if (extras != null)
            data = extras.getParcelable(EXTRA_DATA);

        if (data == null) {
            setResult(RESULT_CANCELED);
            finish();
        } else {
            int rtlImageResId = extras.getInt(EXTRA_ROTATE_LEFT_IMAGE_RES, R.drawable.ic_rotate_left);
            int rtrImageResId = extras.getInt(EXTRA_ROTATE_RIGHT_IMAGE_RES, R.drawable.ic_rotate_right);
            int saveImageResId = extras.getInt(EXTRA_SAVE_IMAGE_RES, R.drawable.ic_check_circle);
            int rtColorResId = extras.getInt(EXTRA_ROTATE_BTN_COLOR_RES, R.color.colorPrimary);
            int saveColorResId = extras.getInt(EXTRA_SAVE_BTN_COLOR_RES, R.color.colorAccent);

            Fragment fragment = ImageCropperFragment.instantiate(data, saveColorResId, rtColorResId, rtlImageResId, rtrImageResId, saveImageResId);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .commitAllowingStateLoss();
        }
    }

    void setResultAndExit(DocumentData documentData) {
        Intent data = getIntent();
        data.putExtra(RESULT_IMAGE_PATH, documentData.getImageUri().toString());
        data.putExtra(RESULT_DATA, documentData);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onImageProcessingFailed(String reason, @Nullable Exception error) {
        Log.d("CROP-ACTIVITY", "image processing failed: " + reason);
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onImagesProcessed(ArrayList<DocumentData> dataList) {
        Log.d("CROP-ACTIVITY", "image processed");
        setResultAndExit(dataList.get(0));
    }
}
