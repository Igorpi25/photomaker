package com.ivanov.tech.photomaker.ui;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
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
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ivanov.tech.photomaker.CommonSettings;
import com.ivanov.tech.photomaker.R;
import com.ivanov.tech.photomaker.effect.OnProgressListener;
import com.ivanov.tech.photomaker.util.BitmapUtils;
import com.ivanov.tech.photomaker.util.FileUtils;

// Working with mCamera use code from lesson's(132-134) of startandroid.ru. Source code was changed, but original comments(in russian) are saved
public class CameraActivity extends AppCompatActivity implements Camera.PictureCallback{

    SurfaceView mSurfaceView;
    SurfaceHolder mSurfaceHolder;
    HolderCallback mHolderCallback;
    Camera mCamera;

    final int CAMERA_ID = 0;//ID of mCamera in device. Example: 0-back, 1-front(self)
    final boolean FULL_SCREEN = true; //Is preview display fullscreen?

    File mFileUsedToShare; //Here we save result bitmap like PNG-file every time when change current Effect

    final int PERMISSIONS_REQUEST_CAMERA = 1;//constant to identify permissions for camera
    final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;//constant to identify permission for SD card

    final int REQUEST_EXISTING_IMAGE_OPEN_USING_GALLERY = 1;//constant to identify result image from external App(Gallery, File manager, etc)

    int mRotationOfTakenPhoto =0; //Here we save of rotation in degrees (0,90,180,270).
    // It used when picture is taken in portrait orientation


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mSurfaceView.setVisibility(View.INVISIBLE);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mHolderCallback = new HolderCallback();

        mFileUsedToShare = FileUtils.getTempFile();

    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d("Igor log","onStart");

        checkPermissionSD();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d("Igor log","onResume");

        if((ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)== PackageManager.PERMISSION_GRANTED)
        &&(ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED)){
            // Permission is not granted
            Log.d("Igor logs","onResume all permissions are grant ");

            mCamera = Camera.open(CAMERA_ID);
            setPreviewSize(FULL_SCREEN);

            mSurfaceView.setVisibility(View.VISIBLE);
            mSurfaceHolder.addCallback(mHolderCallback);
        }else{
            Log.d("Igor logs","onResume all permissions are not grant ");

        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCamera != null) {
            mCamera.release();
            mCamera = null;

            mSurfaceHolder.removeCallback(mHolderCallback);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA:

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Good! User grant some permission to App. We don't that all Permissions were granted, so try again to check it and if all right start mCamera

                    Log.d("Igor logs","onRequestPermissionsResult.Camera User grant permission to App");

                    setPermissionCameraMessageVisibible(false);
                } else {

                    Log.d("Igor logs","onRequestPermissionsResult.Camera User manually denied permission request");

                    // User manually denied some permission request

                    Log.d("Igor logs","onRequestPermissionsResult.Camera set camera prmission text-message visible");
                    setPermissionCameraMessageVisibible(true);

                }
                break;

            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Good! User grant some permission to App. We don't that all Permissions were granted, so try again to check it and if all right start mCamera

                    Log.d("Igor logs","onRequestPermissionsResult.SD User grant permission to App");
                    Log.d("Igor logs","onRequestPermissionsResult.SD now check camera permission");
                    checkPermissionCamera();

                } else {

                    Log.d("Igor logs","onRequestPermissionsResult.SD User manually denied permission request");

                    // User manually denied some permission request
                    // So we will not force him

                    Log.d("Igor logs","onRequestPermissionsResult.SD show App Close dialog");
                    showCloseAppMessage();

                }
                break;



        }

    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {

        Log.d("Igor log", "onPictureTaken begin");

        new TakePhotoTask().execute(data);

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

    public void onClickTakePicture(View view) {


        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)== PackageManager.PERMISSION_GRANTED) {

            mCamera.takePicture(null, null, this);//Taken photo will be got in onPictureTaken-method
        }else{
            //Show message about permissions and Allow-button
            Toast.makeText(this,R.string.takephoto_camera_permission_denied_message,Toast.LENGTH_LONG).show();
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
        Size size = mCamera.getParameters().getPreviewSize();

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
        mSurfaceView.getLayoutParams().height = (int) (rectPreview.bottom);
        mSurfaceView.getLayoutParams().width = (int) (rectPreview.right);
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
        mCamera.setDisplayOrientation(result);

        // Here we save rotation of display.
        // We'll use this stored rotation when photo will taken from mCamera in portrait orientation
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
                Log.d("Igor logs","surfaceCreated begin");
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
                Log.d("Igor logs","surfaceCreated success");
            } catch (Exception e) {
                Log.d("Igor logs","surfaceCreated error="+e.toString());
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {

            try {
                Log.d("Igor logs","surfaceChanged begin");
                mCamera.stopPreview();
                setCameraDisplayOrientation(CAMERA_ID);
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
                Log.d("Igor logs","surfaceChanged success");
            } catch (Exception e) {
                Log.d("Igor logs","surfaceChanged error="+e.toString());
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d("Igor logs","surfaceDestroyed");
        }

    }

    private class TakePhotoTask extends AsyncTask<byte[], Integer, File> {

        ProgressBar mProgressBar;
        View mGroupProgress;

        int mLastProgress;

        protected void onPreExecute() {
            mProgressBar=findViewById(R.id.progressBar);
            mProgressBar.setProgress(0);

            mGroupProgress=findViewById(R.id.group_progressbar);
            mGroupProgress.setVisibility(View.VISIBLE);
        }

        protected File doInBackground(byte[]... datas) {

            try {
                //Picture came to us in bytes, we have to save it in file. Here we save in original-size (always it extremely large)
                FileOutputStream fos = new FileOutputStream(mFileUsedToShare);
                fos.write(datas[0]);
                fos.close();

                publishProgress(10);

                //Open saved original file, and reduce size to reqWidth and reqHeight
                //If the display was in portrait orientation, when we click "takePhoto" we should rotate taken picture
                FileInputStream is=new FileInputStream(mFileUsedToShare);
                FileDescriptor fileDescriptor = is.getFD();
                BitmapUtils.saveImageToExternalStorage(CameraActivity.this,mFileUsedToShare,BitmapUtils.rotate(BitmapUtils.decodeSampledBitmapFromFile(fileDescriptor, CommonSettings.reqWidth, CommonSettings.reqHeight),
                        mRotationOfTakenPhoto));
                publishProgress(100);


                //Open targeted Activity with Image Effects
                Uri contentUri = FileProvider.getUriForFile(CameraActivity.this, "com.ivanov.tech.photomaker.fileprovider", mFileUsedToShare);
                openPhotoFiltersActivity(contentUri);


            } catch (Exception e) {

                Log.d("Igor log", "onPictureTaken doInBackground error e="+e.getMessage());

                e.printStackTrace();
            }


            return mFileUsedToShare;
        }

        protected void onProgressUpdate(Integer... progress) {
            mProgressBar.setProgress(progress[0]);
        }


        protected void onPostExecute(File sharedFile) {

            mGroupProgress.setVisibility(View.GONE);

        }

    }

    //---------------Permissions processing--------------

    void checkPermissionCamera() {

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            Log.d("Igor logs","checkPermissionCamera Permission is not granted ");

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
                Log.d("Igor logs","checkPermissionCamera show NCP");

                showPermissionCameraDialog();

            }else{
                Log.d("Igor logs","checkPermissionCamera Make request");

                ActivityCompat.requestPermissions(CameraActivity.this,
                        new String[]{Manifest.permission.CAMERA},
                        PERMISSIONS_REQUEST_CAMERA);
            }

        } else {
            // Permission has already been granted
            Log.d("Igor logs","checkPermissionCamera Permission has already been granted");

            Log.d("Igor logs","checkPermissionCamera hide NPC group");
            setPermissionCameraMessageVisibible(false);
        }

    }

    void checkPermissionSD() {

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            Log.d("Igor logs","checkPermissionSD Permission is not granted ");

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Log.d("Igor logs","checkPermissionSD show NCP");

                showPermissionSDDialog();

            }else{
                Log.d("Igor logs","checkPermissionSD Make request");

                ActivityCompat.requestPermissions(CameraActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }

        } else {
            // Permission has already been granted
            Log.d("Igor logs","checkPermissionSD Permission has already been granted");
            Log.d("Igor logs","checkPermissionSD now check camera permission");
            checkPermissionCamera();
        }

    }

    //Show or hide group "No Camera Permission"(NCP) when any permission is denied
    void setPermissionCameraMessageVisibible(boolean value){
        findViewById(R.id.textView_ncp_messsage).setVisibility(value?View.VISIBLE:View.GONE);
    }

    void showPermissionCameraDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(R.string.dialog_permission_camera_message)
                .setTitle(R.string.dialog_permission_camera_title);

        // Add the buttons
        builder.setPositiveButton(R.string.dialog_permission_camera_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                dialog.dismiss();

                ActivityCompat.requestPermissions(CameraActivity.this,
                        new String[]{Manifest.permission.CAMERA},
                        PERMISSIONS_REQUEST_CAMERA);

            }
        });
        builder.setNegativeButton(R.string.dialog_permission_camera_no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                setPermissionCameraMessageVisibible(true);
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();

        dialog.show();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);



    }

    void showPermissionSDDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(R.string.dialog_permission_sd_message)
                .setTitle(R.string.dialog_permission_sd_title);

        // Add the buttons
        builder.setPositiveButton(R.string.dialog_permission_sd_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                dialog.dismiss();

                ActivityCompat.requestPermissions(CameraActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

            }
        });
        builder.setNegativeButton(R.string.dialog_permission_sd_no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                CameraActivity.this.finish();
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();

        dialog.show();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

    }

    void showCloseAppMessage(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(R.string.dialog_permission_close_app_message)
                .setTitle(R.string.dialog_permission_close_app_title);

        builder.setPositiveButton(R.string.dialog_permission_close_app_quit, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                CameraActivity.this.finish();
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();

        dialog.show();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
    }

}
