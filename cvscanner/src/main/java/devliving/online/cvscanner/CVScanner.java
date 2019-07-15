package devliving.online.cvscanner;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import java.io.IOException;

import devliving.online.cvscanner.crop.CropImageActivity;
import devliving.online.cvscanner.scanner.DocumentScannerActivity;
import devliving.online.cvscanner.util.Util;

/**
 * Created by Mehedi Hasan Khan <mehedi.mailing@gmail.com> on 9/14/17.
 */

public final class CVScanner {
    public static String RESULT_IMAGE_PATH = "result_image_path";
    public static String RESULT_IMAGES_PATH = "result_images_path";
    public static String RESULT_DATA = "result_data";
    public static String RESULT_DATA_LIST = "result_data_list";

    public static String getFileproviderName(Context context){
        return context.getPackageName() + ".cvscanner.fileprovider";
    }

    public static @Nullable Uri startCameraIntent(Activity context, int reqCode) throws IOException {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
            // Create the File where the photo should go
            Uri photoUri = Util.createTempFile(context,
                    "IMG_" + System.currentTimeMillis() + "_CVS", ".jpg",
                    true);

            // Continue only if the File was successfully created
            if (photoUri != null) {
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                context.startActivityForResult(takePictureIntent, reqCode);

                return photoUri;
            }
        }

        return null;
    }

    public static void startScanner(Activity activity, boolean isPassport, boolean showFlash, boolean disableAutomaticCapture, int filterType, boolean singleDocument, int reqCode) {
        Intent i = new Intent(activity, DocumentScannerActivity.class);
        i.putExtra(DocumentScannerActivity.EXTRA_IS_PASSPORT, isPassport);
        i.putExtra(DocumentScannerActivity.EXTRA_SHOW_FLASH, showFlash);
        i.putExtra(DocumentScannerActivity.EXTRA_DISABLE_AUTOMATIC_CAPTURE, disableAutomaticCapture);
        i.putExtra(DocumentScannerActivity.EXTRA_FILTER_TYPE, filterType);
        i.putExtra(DocumentScannerActivity.EXTRA_SINGLE_DOCUMENT, singleDocument);
        activity.startActivityForResult(i, reqCode);
    }

    public static void startScanner(Activity activity, boolean isPassport, boolean showFlash, boolean disableAutomaticCapture, int filterType, boolean singleDocument, int reqCode,
                                    @ColorRes int docBorderColorRes, @ColorRes int docBodyColorRes, @ColorRes int torchColor, @ColorRes int torchColorLight) {
        Intent i = new Intent(activity, DocumentScannerActivity.class);
        i.putExtra(DocumentScannerActivity.EXTRA_IS_PASSPORT, isPassport);
        i.putExtra(DocumentScannerActivity.EXTRA_SHOW_FLASH, showFlash);
        i.putExtra(DocumentScannerActivity.EXTRA_DISABLE_AUTOMATIC_CAPTURE, disableAutomaticCapture);
        i.putExtra(DocumentScannerActivity.EXTRA_FILTER_TYPE, filterType);
        i.putExtra(DocumentScannerActivity.EXTRA_SINGLE_DOCUMENT, singleDocument);
        i.putExtra(DocumentScannerActivity.EXTRA_DOCUMENT_BODY_COLOR, docBodyColorRes);
        i.putExtra(DocumentScannerActivity.EXTRA_DOCUMENT_BORDER_COLOR, docBorderColorRes);
        i.putExtra(DocumentScannerActivity.EXTRA_TORCH_TINT_COLOR, torchColor);
        i.putExtra(DocumentScannerActivity.EXTRA_TORCH_TINT_COLOR_LIGHT, torchColorLight);
        
        activity.startActivityForResult(i, reqCode);
    }

    public static void startManualCropper(Activity activity, Uri imageUri, int reqCode) {
        DocumentData data = DocumentData.Create(activity, imageUri);
        Intent intent = new Intent(activity, CropImageActivity.class);
        intent.putExtra(CropImageActivity.EXTRA_DATA, data);
        activity.startActivityForResult(intent, reqCode);
    }

    public static void startManualCropper(Activity activity, DocumentData data, int reqCode) {
    }

    public  static void startManualCropper(Activity activity, DocumentData data, int reqCode, @ColorRes int buttonTint,
                                           @ColorRes int buttonTintSecondary, @DrawableRes int rotateLeftIconRes,
                                           @DrawableRes int rotateRightIconRes, @DrawableRes int saveButtonIconRes){
        Intent intent = new Intent(activity, CropImageActivity.class);

        intent.putExtra(CropImageActivity.EXTRA_DATA, data);
        intent.putExtra(CropImageActivity.EXTRA_ROTATE_BTN_COLOR_RES, buttonTintSecondary);
        intent.putExtra(CropImageActivity.EXTRA_ROTATE_LEFT_IMAGE_RES, rotateLeftIconRes);
        intent.putExtra(CropImageActivity.EXTRA_ROTATE_RIGHT_IMAGE_RES, rotateRightIconRes);
        intent.putExtra(CropImageActivity.EXTRA_SAVE_BTN_COLOR_RES, buttonTint);
        intent.putExtra(CropImageActivity.EXTRA_SAVE_IMAGE_RES, saveButtonIconRes);

        activity.startActivityForResult(intent, reqCode);
    }
}
