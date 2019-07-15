package devliving.online.cvscannersample;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.getbase.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;

import devliving.online.cvscanner.CVScanner;
import devliving.online.cvscanner.util.Util;

import static devliving.online.cvscanner.DocumentData.V_FILTER_TYPE_COLOR;

public class MainActivity extends AppCompatActivity {

    private final int REQ_SCAN = 11;

    private static final int REQUEST_TAKE_PHOTO = 121;
    private static final int REQUEST_PICK_PHOTO = 123;
    private static final int REQ_CROP_IMAGE = 122;
    private static final int REQ_PERMISSIONS = 120;

    Uri currentPhotoUri = null;
    RecyclerView mList;
    ImageAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mList = findViewById(R.id.image_list);
        mList.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new ImageAdapter();
        mList.setAdapter(mAdapter);

        FloatingActionButton fabScan = findViewById(R.id.action_scan);
        fabScan.setOnClickListener(view ->
                CVScanner.startScanner(this, false, true, false, V_FILTER_TYPE_COLOR, false, REQ_SCAN));

        FloatingActionButton fabCrop = findViewById(R.id.action_crop);
        fabCrop.setOnClickListener(view -> new AlertDialog.Builder(MainActivity.this)
                .setMessage("Choose photo to crop")
                .setPositiveButton("With Camera", (dialog, which) -> startCameraIntent())
                .setNeutralButton("From Device", (dialog, which) -> startImagePickerIntent())
                .show());
    }

    void startScannerIntent(boolean isPassport, boolean showFlash, boolean disableAutomaticCapture, int filterType, boolean singleDocument) {
        CVScanner.startScanner(this, isPassport, showFlash, disableAutomaticCapture, filterType, singleDocument, REQ_SCAN);
    }

    private void startCameraIntent(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            try {
                currentPhotoUri = CVScanner.startCameraIntent(this, REQUEST_TAKE_PHOTO);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else{
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_PERMISSIONS);
        }
    }

    private void startImagePickerIntent(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Pick an image"), REQUEST_PICK_PHOTO);
    }

    private void startImageCropIntent(){
        CVScanner.startManualCropper(this, currentPhotoUri, REQ_CROP_IMAGE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_step_by_step) {
            new AlertDialog.Builder(this)
                    .setTitle("Choose one")
                    .setItems(new String[]{"Document (ID, license etc.)" ,
                            "Passport - HoughLines algorithm",
                            "Passport - MRZ based algorithm",
                        "Passport - MRZ based retrial"}, (dialog, which) -> {
                            Intent intent = new Intent(MainActivity.this, StepByStepTestActivity.class);
                            intent.putExtra(StepByStepTestActivity.EXTRA_SCAN_TYPE, StepByStepTestActivity.CVCommand.values()[which]);
                            startActivity(intent);
                        })
                    .setCancelable(true)
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("MAIN", "got activity result, code: " + requestCode + ", result: " + (resultCode == RESULT_OK));

        if(resultCode == RESULT_OK){
            switch (requestCode){
                case REQ_SCAN:
                    Log.d("MAIN", "got intent data");
                    if (data != null && data.getExtras() != null) {
                        String[] pathList = data.getStringArrayExtra(CVScanner.RESULT_IMAGES_PATH);
                        for (String s : pathList) {
                            mAdapter.add(Uri.parse(s));
                            Log.d("MAIN", "added: " + s);
                        }
                    }
                    break;

                case REQ_CROP_IMAGE:
                    if (data != null && data.getExtras() != null) {
                        String path = data.getStringExtra(CVScanner.RESULT_IMAGE_PATH);
                        mAdapter.add(Uri.parse(path));
                        Log.d("MAIN", "added: " + path);
                    }
                    break;

                case REQUEST_TAKE_PHOTO:
                    startImageCropIntent();
                    break;

                case REQUEST_PICK_PHOTO:
                    if(data.getData() != null){
                        currentPhotoUri = data.getData();
                        startImageCropIntent();
                    }
                    break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQ_PERMISSIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    startCameraIntent();
                }
            }
        }
    }
}
