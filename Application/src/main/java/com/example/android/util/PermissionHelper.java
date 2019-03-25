package com.example.android.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PermissionHelper extends BaseModelClass {

    public PermissionHelper(Activity activity, Context context){
        super(activity, context);
    }

    public boolean isPermissionsGranted(String[] appPermissions) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            return true;
        } else {
            List<String> listPermissionsNeeded = new ArrayList<>();
            for (String perm : appPermissions) {
                if (ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(perm);
                }
            }

            //Ask for non-granted permissions
            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(activity, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                        Constant.PERMISSION_REQUEST_CODE);
                return false;
            }

            //App has all the permissions
            return true;
        }
    }

    public void showDialog(String title, String msg, String positiveLabel,
                           DialogInterface.OnClickListener positiveOnClick,
                           String negativeLabel, DialogInterface.OnClickListener negativeOnClick) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setCancelable(false);
        builder.setMessage(msg);
        builder.setPositiveButton(positiveLabel, positiveOnClick);
        builder.setNegativeButton(negativeLabel, negativeOnClick);

        AlertDialog alert = builder.create();
        alert.show();
    }

    //image related tasks
    public String browseForImage() {
        String sCameraPhotoPath = "";
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = ImageProcessor.createImageFile();
                takePictureIntent.putExtra(Constant.STRING_PHOTO_PATH, sCameraPhotoPath);
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.d("tttt", "Unable to create Image File", ex);
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                sCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
            } else {
                takePictureIntent = null;
            }
        }

        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("image/*");

        Intent[] intentArray;
        if (takePictureIntent != null) {
            intentArray = new Intent[]{takePictureIntent};
        } else {
            intentArray = new Intent[0];
        }

        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

        activity.startActivityForResult(chooserIntent, Constant.PICK_IMAGE);
        return sCameraPhotoPath;
    }


    public void pickedFromGallery(int resultCode, Intent data, int aspectX, int aspectY) {
        if (resultCode == Activity.RESULT_OK) {
            String imagePathToCropActivity;
            final boolean isCamera;
            if (data == null) {
                isCamera = true;
            } else {
                final String action = data.getAction();
                if (action == null) {
                    isCamera = false;
                } else {
                    isCamera = action.equals(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                }
            }
            Uri selectedImageUri;
            if (isCamera) {
                selectedImageUri = null;
            } else {
                selectedImageUri = data == null ? null : data.getData();
            }
            imagePathToCropActivity = ImageHelpGeneral.getPath(selectedImageUri, activity);

            if (TextUtils.isEmpty(imagePathToCropActivity)) {
                imagePathToCropActivity = ImageHelpGeneral.getPath(context, selectedImageUri);
            }
        }
    }
}
