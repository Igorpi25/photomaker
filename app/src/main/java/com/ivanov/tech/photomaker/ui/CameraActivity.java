package com.ivanov.tech.photomaker.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.ivanov.tech.photomaker.CommonSettings;
import com.ivanov.tech.photomaker.R;
import com.ivanov.tech.photomaker.util.BitmapUtils;
import com.ivanov.tech.photomaker.util.FileUtils;

// Working with camera use code from lesson's(132-134) of startandroid.ru. Source code was changed, but original comments(in russian) are saved
public class CameraActivity extends AppCompatActivity implements Camera.PictureCallback{

    SurfaceView sv;
    SurfaceHolder holder;
    HolderCallback holderCallback;
    Camera camera;

    final int CAMERA_ID = 0;//ID of camera in device. Example: 0-back, 1-front(self)
    final boolean FULL_SCREEN = true; //Is preview display fullscreen?

    File mFileUsedToShare; //Here we save result bitmap like PNG-file every time when change current Effect

    final int PERMISSIONS_REQUEST_CAMERA_AND_EXTERNAL_WRITE = 1;//constant to identify permissions callback after user action
    final int REQUEST_EXISTING_IMAGE_OPEN_USING_GALLERY = 1;//constant to identify result image from external App(Gallery, File manager, etc)

    int mRotationOfTakenPhoto =0; //Here we save of rotation in degrees (0,90,180,270).
    // It used when picture is taken in portrait orientation


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

        mFileUsedToShare = FileUtils.getTempFile();

    }

    @Override
    protected void onResume() {
        super.onResume();

        checkPermissionsAndIfAllRightStartCamera(false);

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
            case PERMISSIONS_REQUEST_CAMERA_AND_EXTERNAL_WRITE:

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Good! User grant some permission to App. We don't that all Permissions were granted, so try again to check it and if all right start camera
                    checkPermissionsAndIfAllRightStartCamera(false);

                } else {
                    // User manually denied some permission request
                    // So we will not force him, show NCP(Message) and let him choose photo from Gallery without using camera
                    setGroupNCPVisibility(true);
                }

                return;
        }

    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {

        Log.d("Igor log", "onPictureTaken begin");

        try {
            //Picture came to us in bytes, we have to save it in file. Here we save in original-size (always it extremely large)
            FileOutputStream fos = new FileOutputStream(mFileUsedToShare);
            fos.write(data);
            fos.close();

            //Open saved original file, and reduce size to reqWidth and reqHeight
            //If the display was in portrait orientation, when we click "takePhoto" we should rotate taken picture
            FileInputStream is=new FileInputStream(mFileUsedToShare);
            FileDescriptor fileDescriptor = is.getFD();
            BitmapUtils.saveImageToExternalStorage(this,mFileUsedToShare,BitmapUtils.rotate(BitmapUtils.decodeSampledBitmapFromFile(fileDescriptor, CommonSettings.reqWidth, CommonSettings.reqHeight),
                    mRotationOfTakenPhoto));

            //Open targeted Activity with Image Effects
            Uri contentUri = FileProvider.getUriForFile(this, "com.ivanov.tech.photomaker.fileprovider", mFileUsedToShare);
            openPhotoFiltersActivity(contentUri);

        } catch (Exception e) {

            Log.d("Igor log", "onPictureTaken error e="+e.getMessage());

            e.printStackTrace();
        }

        Log.d("Igor log", "onPictureTaken end");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EXISTING_IMAGE_OPEN_USING_GALLERY && resultCode == RESULT_OK) {

            Uri fullPhotoUri = data.getData();

            Bitmap bitmap=BitmapUtils.uriToBitmap(this,fullPhotoUri);
            BitmapUtils.saveImageToExternalStorage(this,mFileUsedToShare,bitmap);

            Uri contentUri = FileProvider.getUriForFile(this, "com.ivanov.tech.photomaker.fileprovider",mFileUsedToShare );
            openPhotoFiltersActivity(contentUri);

        }
    }

    public void onClickAllowPermissions(View view) {
        checkPermissionsAndIfAllRightStartCamera(true);
    }

    public void onClickTakePicture(View view) {

        //If NCP-group's is visibility it means some permission is not granted. So we can't call takePicture
        if(findViewById(R.id.group_ncp).getVisibility()!=View.VISIBLE) {

            camera.takePicture(null, null, this);//Taken photo will be got in onPictureTaken-method
        }else{
            //Show message about permissions and Allow-button
            Toast.makeText(this,R.string.camera_takephoto_permission_denied_message,Toast.LENGTH_LONG).show();
        }

    }

    //Choose existing picture using device's Gallery app, or other same app
    public void onClickOpenFromGallery(View view) {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Only the system receives the ACTION_OPEN_DOCUMENT, so no need to test.
        startActivityForResult(intent, REQUEST_EXISTING_IMAGE_OPEN_USING_GALLERY);//Choosen picture will be got in onActivityResult-method
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

        // Here we save rotation of display.
        // We'll use this stored rotation when photo will taken from camera in portrait orientation
        mRotationOfTakenPhoto =result;


    }

    public void openPhotoFiltersActivity(Uri imageUri) {
        Intent intent = new Intent(this, PhotoEffectsActivity.class);
        intent.setAction(Intent.ACTION_SEND);
        intent.setDataAndType(imageUri, getContentResolver().getType(imageUri));
        intent.putExtra(Intent.EXTRA_STREAM, imageUri);
        startActivity(intent);
    }

    class HolderCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (Exception e) {
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }

    }

    void checkPermissionsAndIfAllRightStartCamera(boolean should_request_anyway) {

        if ((ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED)
                || (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) ) {

            // Permission is not granted

            Log.d("Igor logs","Permission is not granted");

            // Should we show an NCP-group?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA) && ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) && !should_request_anyway) {
                Log.d("Igor logs","Show NCP group explanation");

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                setGroupNCPVisibility(true);

            } else {

                Log.d("Igor logs","No explanation need");

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_CAMERA_AND_EXTERNAL_WRITE);

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

    //Show or hide group "No Camera Permission"(NCP) when any permission is denied
    void setGroupNCPVisibility(boolean value){
        findViewById(R.id.group_ncp).setVisibility(value?View.VISIBLE:View.GONE);
    }


}
