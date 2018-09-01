package com.ivanov.tech.photomaker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity implements Camera.PictureCallback{


    SurfaceView sv;
    SurfaceHolder holder;
    HolderCallback holderCallback;
    Camera camera;
    File photoFile;

    final int CAMERA_ID = 0;
    final boolean FULL_SCREEN = true;
    final int PERMISSIONS_REQUEST_CAMERA = 1;
    final String TEMP_FILE_NAME = "PhotoMakerTemp.jpg";

    final int reqWidth=300,reqHeight=300;


    static final int REQUEST_IMAGE_OPEN = 1;

    int rotationOfPhoto=0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        sv = (SurfaceView) findViewById(R.id.surfaceView);
        holder = sv.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        holderCallback = new HolderCallback();


        File tempFilePath = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        photoFile = new File(tempFilePath, TEMP_FILE_NAME);


    }

    @Override
    protected void onResume() {
        super.onResume();



        checkPermissionsAndStartCamera(false);


    }

    @Override
    protected void onPause() {
        super.onPause();

        if (camera != null) {
            camera.release();
            camera = null;

            holder.removeCallback(holderCallback);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {



                } else {
                    // Show dialog "No camera permission"
                    setGroupNCPVisibility(true);
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {

        Log.d("Igor log", "onPictureTaken begin");

        try {
            FileOutputStream fos = new FileOutputStream(photoFile);
            fos.write(data);
            fos.close();

            FileInputStream is=new FileInputStream(photoFile);
            FileDescriptor fileDescriptor = is.getFD();

            saveImageToExternalStorage(rotate(decodeSampledBitmapFromFile(fileDescriptor,reqWidth, reqHeight),
                    rotationOfPhoto));

            Log.d("Igor log", "onPictureTaken success absolutePath="+photoFile.getAbsolutePath());
            Log.d("Igor log", "onPictureTaken success path="+photoFile.getPath());
            Log.d("Igor log", "onPictureTaken success canonicalPath="+photoFile.getCanonicalPath());


            Uri contentUri = FileProvider.getUriForFile(this, "com.ivanov.tech.photomaker.fileprovider", photoFile);
            openPhotoFiltersActivity(contentUri);

        } catch (Exception e) {

            Log.d("Igor log", "onPictureTaken error e="+e.getMessage());

            e.printStackTrace();
        }

        Log.d("Igor log", "onPictureTaken end");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_OPEN && resultCode == RESULT_OK) {

            Uri fullPhotoUri = data.getData();

            Log.d("Igor log","onActivityResult requestCode="+requestCode+" resultCode="+resultCode);
            Log.d("Igor log","onActivityResult fullPhotoUri="+fullPhotoUri);
            Log.d("Igor log","onActivityResult Path="+fullPhotoUri.getPath());
            Log.d("Igor log","onActivityResult encpdedPath="+fullPhotoUri.getPath());

            /*Log.e("Igor log","bitmap begin");
            try {

                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), fullPhotoUri);

                Log.e("Igor log","bitmap success");
            } catch (IOException e) {
                Log.e("Igor log","bitmap failed e="+e);
            }
            Log.e("Igor log","bitmap end");*/


            Bitmap bitmap=uriToBitmap(fullPhotoUri);
            saveImageToExternalStorage(bitmap);

            Uri contentUri = FileProvider.getUriForFile(this, "com.ivanov.tech.photomaker.fileprovider",photoFile );
            openPhotoFiltersActivity(contentUri);




        }
    }

    public void onClickAllowPermissions(View view) {
        checkPermissionsAndStartCamera(true);
    }

    public void onClickTakePicture(View view) {
        camera.takePicture(null, null, this);

    }

    public void onClickOpenFromGallery(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Only the system receives the ACTION_OPEN_DOCUMENT, so no need to test.
        startActivityForResult(intent, REQUEST_IMAGE_OPEN);
    }

    class HolderCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {

            try {
                camera.stopPreview();
                setCameraDisplayOrientation(CAMERA_ID);
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }

    }

    void setPreviewSize(boolean fullScreen) {

        // получаем размеры экрана
        Display display = getWindowManager().getDefaultDisplay();
        boolean widthIsMax = display.getWidth() > display.getHeight();

        // определяем размеры превью камеры
        Size size = camera.getParameters().getPreviewSize();

        RectF rectDisplay = new RectF();
        RectF rectPreview = new RectF();

        // RectF экрана, соотвествует размерам экрана
        rectDisplay.set(0, 0, display.getWidth(), display.getHeight());

        // RectF первью
        if (widthIsMax) {
            // превью в горизонтальной ориентации
            rectPreview.set(0, 0, size.width, size.height);
        } else {
            // превью в вертикальной ориентации
            rectPreview.set(0, 0, size.height, size.width);
        }

        Matrix matrix = new Matrix();
        // подготовка матрицы преобразования
        if (!fullScreen) {
            // если превью будет "втиснут" в экран (второй вариант из урока)
            matrix.setRectToRect(rectPreview, rectDisplay,
                    Matrix.ScaleToFit.START);
        } else {
            // если экран будет "втиснут" в превью (третий вариант из урока)
            matrix.setRectToRect(rectDisplay, rectPreview,
                    Matrix.ScaleToFit.START);
            matrix.invert(matrix);
        }
        // преобразование
        matrix.mapRect(rectPreview);

        // установка размеров surface из получившегося преобразования
        sv.getLayoutParams().height = (int) (rectPreview.bottom);
        sv.getLayoutParams().width = (int) (rectPreview.right);
    }

    void setCameraDisplayOrientation(int cameraId) {
        // определяем насколько повернут экран от нормального положения
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result = 0;

        // получаем инфо по камере cameraId
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        // задняя камера
        if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
            result = ((360 - degrees) + info.orientation);
        } else
            // передняя камера
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                result = ((360 - degrees) - info.orientation);
                result += 360;
            }
        result = result % 360;
        camera.setDisplayOrientation(result);

        rotationOfPhoto=result;


    }

    void checkPermissionsAndStartCamera(boolean should_request_anyway) {
        // Here, thisActivity is the current activity


        if ((ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED)
                        || (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) ) {

            // Permission is not granted

            Log.d("Igor logs","Permission is not granted");

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA) && ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) && !should_request_anyway) {
                Log.d("Igor logs","Rationale explanation");

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                setGroupNCPVisibility(true);

            } else {
                // No explanation needed; request the permission

                Log.d("Igor logs","No explanation need");

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_CAMERA);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted

            Log.d("Igor logs","Permission has already been granted");

            camera = Camera.open(CAMERA_ID);
            setPreviewSize(FULL_SCREEN);
            holder.addCallback(holderCallback);

            setGroupNCPVisibility(false);
        }

    }

    void setGroupNCPVisibility(boolean value){

        //Show or hide group "No Camera Permission"(NCP)

        findViewById(R.id.group_ncp).setVisibility(value?View.VISIBLE:View.GONE);
    }

    /*public void openPhotoFiltersActivity(String filepath) {
        Intent intent = new Intent(this, PhotoFiltersActivity.class);
        intent.putExtra("file_path", filepath);
        startActivity(intent);
    }*/

    public void openPhotoFiltersActivity(Uri imageUri) {
        Intent intent = new Intent(this, PhotoFiltersActivity.class);
        intent.setAction(Intent.ACTION_SEND);
        intent.setDataAndType(imageUri, getContentResolver().getType(imageUri));
        intent.putExtra(Intent.EXTRA_STREAM, imageUri);
        startActivity(intent);
    }

    public boolean saveImageToExternalStorage(Bitmap image) {

        Log.e("Igor log", "saveImageToExternalStorage begin");

        try {

            OutputStream fOut = null;
            photoFile.createNewFile();
            fOut = new FileOutputStream(photoFile);

            // 100 means no compression, the lower you go, the stronger the compression
            image.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();

            MediaStore.Images.Media.insertImage(this.getContentResolver(), photoFile.getAbsolutePath(), photoFile.getName(), photoFile.getName());

            Log.e("Igor log", "saveImageToExternalStorage success");

            return true;

        } catch (Exception e) {
            Log.e("Igor log", "saveImageToExternalStorage failed");
            Log.e("Igor log", "saveImageToExternalStorage e="+e.getMessage());
            return false;
        }
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
}
