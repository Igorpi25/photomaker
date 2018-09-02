package com.ivanov.tech.photomaker.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import com.ivanov.tech.photomaker.CommonSettings;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class BitmapUtils {

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromFile(FileDescriptor descr,
                                                     int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(descr, null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        Log.d("Igor log", "decodeSampledBitmapFromFile inSampleSize="+options.inSampleSize);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(descr, null, options);
    }

    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.postRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }


    public static Bitmap uriToBitmap(Context context, Uri selectedFileUri) {
        try {

            Log.e("Igor log","uriToBitmap begin");

            ParcelFileDescriptor parcelFileDescriptor =
                    context.getContentResolver().openFileDescriptor(selectedFileUri, "r");

            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

            Bitmap image = decodeSampledBitmapFromFile(fileDescriptor, CommonSettings.reqWidth, CommonSettings.reqHeight);


            parcelFileDescriptor.close();
            Log.e("Igor log","uriToBitmap success");
            return image;


        } catch (IOException e) {
            Log.e("Igor log","uriToBitmap error e="+e.toString());
            e.printStackTrace();
        }
        return null;
    }

    public static boolean saveImageToExternalStorage(Context context, File file, Bitmap image) {

        Log.e("Igor log", "saveImageToExternalStorage begin");

        try {

            OutputStream fOut = null;
            file.createNewFile();
            fOut = new FileOutputStream(file);

            // 100 means no compression, the lower you go, the stronger the compression
            image.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();

            MediaStore.Images.Media.insertImage(context.getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());

            Log.e("Igor log", "saveImageToExternalStorage success");

            return true;

        } catch (Exception e) {
            Log.e("Igor log", "saveImageToExternalStorage failed");
            Log.e("Igor log", "saveImageToExternalStorage e="+e.getMessage());
            return false;
        }
    }

}
