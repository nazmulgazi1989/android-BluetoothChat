package com.example.android.util;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by user on 3/14/2017.
 */

public class ImageProcessor {
//    public static boolean saveBackgroundImage(Context context, Bitmap bitmapImage) {
//        boolean result = true;
//        ContextWrapper cw = new ContextWrapper(context);
//        // path to /data/data/yourapp/app_data/imageDir
//        File directory = cw.getDir("splash", Context.MODE_PRIVATE);
//        // Create imageDir
//        File mypath = new File(directory, "personalized.jpg");
//
//        FileOutputStream fos = null;
//        try {
//            fos = new FileOutputStream(mypath);
//            // Use the compress method on the BitMap object to write image to the OutputStream
//            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
//        } catch (Exception e) {
//            result = false;
//            e.printStackTrace();
//        } finally {
//            try {
//                fos.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        return result;
//    }

    public static boolean saveImageLocally(Context context, Bitmap bitmapImage, String profilePicName) {
        boolean result = true;
        ContextWrapper cw = new ContextWrapper(context);
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("splash", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath = new File(directory, profilePicName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            result = false;
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static Bitmap loadBitmapFromStorage(Context context, String imageName) {
        Bitmap loadedBitmap = null;
        if(TextUtils.isEmpty(imageName))
            return loadedBitmap;
        ContextWrapper cw = new ContextWrapper(context);
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("splash", Context.MODE_PRIVATE);

        try {
            File path = new File(directory, imageName);
            loadedBitmap = BitmapFactory.decodeStream(new FileInputStream(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return loadedBitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return loadedBitmap;
        }
        return loadedBitmap;
    }

    public static Drawable loadDrawableFromStorage(Context context, String imageName) {
        Drawable loadedDrawable = null;
        ContextWrapper cw = new ContextWrapper(context);
        File directory = cw.getDir("splash", Context.MODE_PRIVATE);

        try {
            File path = new File(directory, imageName);
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(path));
            loadedDrawable = new BitmapDrawable(context.getResources(), b);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return loadedDrawable;
        }
        return loadedDrawable;
    }

    public static boolean deleteImage(Context context) {
        ContextWrapper cw = new ContextWrapper(context);
        File directory = cw.getDir("splash", Context.MODE_PRIVATE);
        File path = new File(directory, "personalized.jpg");
        return (path.exists() && path.delete());
    }

    public static File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    public static String getProfilePicUniqueName(){
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return "Pro_" + timeStamp + ".jpg";
    }

    //to make the image bitmap circular
    public static Bitmap getCircularBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        int width = bitmap.getWidth();
        if(bitmap.getWidth()>bitmap.getHeight())
            width = bitmap.getHeight();
        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, width, width);
        final RectF rectF = new RectF(rect);
        final float roundPx = width / 2;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }
}
