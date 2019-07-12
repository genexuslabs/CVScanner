package devliving.online.cvscanner.scanner;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;

import devliving.online.cvscanner.BaseFragment;
import devliving.online.cvscanner.CVScanner;
import devliving.online.cvscanner.DocumentData;
import devliving.online.cvscanner.R;

import static devliving.online.cvscanner.DocumentData.V_FILTER_TYPE_COLOR;

/**
 * Created by Mehedi on 10/15/16.
 */
public class DocumentScannerActivity extends AppCompatActivity implements BaseFragment.ImageProcessorCallback {
    private static final String TAG = "ID-reader";

    // intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    // constants used to pass extra data in the intent
    public static final String EXTRA_DOCUMENT_BORDER_COLOR = "border_color";
    public static final String EXTRA_DOCUMENT_BODY_COLOR = "body_color";
    public static final String EXTRA_TORCH_TINT_COLOR = "torch_tint_color";
    public static final String EXTRA_TORCH_TINT_COLOR_LIGHT = "torch_tint_color_light";
    public static final String EXTRA_IS_PASSPORT = "is_passport";
    public static final String EXTRA_SHOW_FLASH = "show_flash";
    public static final String EXTRA_DISABLE_AUTOMATIC_CAPTURE = "disable_automatic_capture";
    public static final String EXTRA_FILTER_TYPE = "filter_type";
    public static final String EXTRA_SINGLE_DOCUMENT = "single_document";

    public static final int REQ_DOCUMENT_BROWSE = 12;

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
            checkCameraPermission();
    }

    private void checkCameraPermission(){
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED)
            checkPlayServices();
        else
            requestCameraPermission();
    }

    private void checkPlayServices(){
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        } else {
            addScannerFragment();
        }
    }

    private void addScannerFragment() {
        Bundle extras = getIntent().getExtras();
        boolean isScanningPassport = extras != null && getIntent().getBooleanExtra(EXTRA_IS_PASSPORT, false);
        boolean showFlash = extras != null && getIntent().getBooleanExtra(EXTRA_SHOW_FLASH, true);
        boolean disableAutomaticCapture = extras != null && getIntent().getBooleanExtra(EXTRA_DISABLE_AUTOMATIC_CAPTURE, false);
        int filterType = extras != null ? getIntent().getIntExtra(EXTRA_FILTER_TYPE, V_FILTER_TYPE_COLOR) : V_FILTER_TYPE_COLOR;
        boolean singleDocument = extras != null && getIntent().getBooleanExtra(EXTRA_SINGLE_DOCUMENT, false);

        DocumentScannerFragment fragment = null;

        if (extras != null) {
            int borderColor = extras.getInt(EXTRA_DOCUMENT_BORDER_COLOR, -1);
            int bodyColor = extras.getInt(EXTRA_DOCUMENT_BODY_COLOR, -1);
            int torchTintColor = extras.getInt(EXTRA_TORCH_TINT_COLOR, getResources().getColor(R.color.dark_gray));
            int torchTintLightColor = extras.getInt(EXTRA_TORCH_TINT_COLOR_LIGHT, getResources().getColor(R.color.torch_yellow));

            fragment = DocumentScannerFragment.instantiate(isScanningPassport, showFlash, disableAutomaticCapture, filterType, singleDocument, borderColor, bodyColor, torchTintColor, torchTintLightColor);
        } else {
            fragment = DocumentScannerFragment.instantiate(isScanningPassport, showFlash, disableAutomaticCapture, filterType, singleDocument);
        }

        getSupportFragmentManager().beginTransaction()
                .add(R.id.container, fragment)
                .commitAllowingStateLoss();
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        new AlertDialog.Builder(this)
                .setTitle("Camera Permission Required")
                .setMessage("access to device camera is required for scanning")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(thisActivity, permissions,
                                RC_HANDLE_CAMERA_PERM);
                    }
                }).show();
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            checkPlayServices();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Access to Camera Denied")
                .setMessage("Cannot scan document without using the camera")
                .setPositiveButton("OK", listener)
                .show();
    }

    private void setResultAndExit(ArrayList<DocumentData> dataList) {
        String[] list = new String[dataList.size()];
        for (int i = 0; i < list.length; i++)
            list[i] = dataList.get(i).getImageUri().toString();
        Intent data = getIntent();
        data.putExtra(CVScanner.RESULT_IMAGES_PATH, list);
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
