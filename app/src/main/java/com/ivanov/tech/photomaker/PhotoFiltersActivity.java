package com.ivanov.tech.photomaker;

import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PhotoFiltersActivity extends AppCompatActivity {

    Button b1, b2, b3;
    ImageView im;

    private Bitmap bmp;
    private Bitmap operation;

    File photoFile;
    final String TEMP_FILE_NAME = "PhotoMakerTemp.jpg";

    final int reqWidth=300,reqHeight=300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_filters);

        b1 = (Button) findViewById(R.id.button);
        b2 = (Button) findViewById(R.id.button2);
        b3 = (Button) findViewById(R.id.button3);
        im = (ImageView) findViewById(R.id.imageView);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {
                    bmp = uriToBitmap(imageUri);
                    normal(null);
                }
            }
        }


    }

    public void share(View view) {
        Log.d("Igor log","share begin");

        saveImageToExternalStorage(operation);

        Uri contentUri = FileProvider.getUriForFile(this, "com.ivanov.tech.photomaker.fileprovider", photoFile);

        if (contentUri != null) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
            shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            startActivity(Intent.createChooser(shareIntent, "Choose an app"));
        }

        Log.d("Igor log","share end");
    }

    public void gray(View view) {
        operation = Bitmap.createBitmap(bmp.getWidth(),bmp.getHeight(), bmp.getConfig());


        for (int i = 0; i < bmp.getWidth(); i++) {
            for (int j = 0; j < bmp.getHeight(); j++) {
                int p = bmp.getPixel(i, j);
                int r = Color.red(p);
                int g = Color.green(p);
                int b = Color.blue(p);

                int m=(int)(((float)r+g+b)/3);

                operation.setPixel(i, j, Color.argb(Color.alpha(p), m, m, m));
            }
        }
        im.setImageBitmap(operation);
    }

    public void bright(View view){
        operation= Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(),bmp.getConfig());

        for(int i=0; i<bmp.getWidth(); i++){
            for(int j=0; j<bmp.getHeight(); j++){
                int p = bmp.getPixel(i, j);
                int r = Color.red(p);
                int g = Color.green(p);
                int b = Color.blue(p);

                float rate=2f;

                r = (int)(r*rate);
                g = (int)(g*rate);
                b = (int)(b*rate);

                r=(r>255)?255:r;
                g=(g>255)?255:g;
                b=(b>255)?255:b;

                operation.setPixel(i, j, Color.argb(Color.alpha(p), r, g, b));
            }
        }
        im.setImageBitmap(operation);
    }

    public void whiteAndBlack(View view){
        operation= Bitmap.createBitmap(bmp.getWidth(),bmp.getHeight(),bmp.getConfig());

        for(int i=0; i<bmp.getWidth(); i++){
            for(int j=0; j<bmp.getHeight(); j++){
                int p = bmp.getPixel(i, j);
                int r = Color.red(p);
                int g = Color.green(p);
                int b = Color.blue(p);

                float m=(r+g+b)/3.0f;

                int branches=5;

                int part=Math.round(m/255*(branches-1));

                int v=part* (255/(branches-1));

                operation.setPixel(i, j, Color.argb(Color.alpha(p), v, v, v));
            }
        }

        im.setImageBitmap(operation);
    }

    public void normal(View view) {

        operation=Bitmap.createBitmap(bmp);

        im.setImageBitmap(operation);
    }

    private Bitmap uriToBitmap(Uri selectedFileUri) {
        try {

            Log.e("Igor log","uriToBitmap begin");

            ParcelFileDescriptor parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(selectedFileUri, "r");

            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

            Bitmap image = decodeSampledBitmapFromFile(fileDescriptor,reqWidth,reqHeight);


            parcelFileDescriptor.close();
            Log.e("Igor log","uriToBitmap success");
            return image;


        } catch (IOException e) {
            Log.e("Igor log","uriToBitmap error e="+e.toString());
            e.printStackTrace();
        }
        return null;
    }

    public boolean saveImageToExternalStorage(Bitmap image) {

        try {
            File tempFilePath = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            photoFile = new File(tempFilePath, TEMP_FILE_NAME);

            OutputStream fOut = null;
            photoFile.createNewFile();
            fOut = new FileOutputStream(photoFile);

            // 100 means no compression, the lower you go, the stronger the compression
            image.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();

            MediaStore.Images.Media.insertImage(this.getContentResolver(), photoFile.getAbsolutePath(), photoFile.getName(), photoFile.getName());

            return true;

        } catch (Exception e) {
            Log.e("saveToExternalStorage()", e.getMessage());
            return false;
        }
    }

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

}
