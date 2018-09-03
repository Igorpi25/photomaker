package com.ivanov.tech.photomaker.ui;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


import android.graphics.Bitmap;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.github.chrisbanes.photoview.PhotoView;
import com.ivanov.tech.photomaker.R;
import com.ivanov.tech.photomaker.adapter.EffectsListAdapter;
import com.ivanov.tech.photomaker.effect.OnProgressListener;
import com.ivanov.tech.photomaker.util.BitmapUtils;
import com.ivanov.tech.photomaker.util.FileUtils;
import com.ivanov.tech.photomaker.effect.Effect;
import com.ivanov.tech.photomaker.effect.EffectBright;
import com.ivanov.tech.photomaker.effect.EffectGray;
import com.ivanov.tech.photomaker.effect.EffectNothing;
import com.ivanov.tech.photomaker.effect.EffectLimitedGray;

import java.io.File;
import java.util.ArrayList;

public class PhotoEffectsActivity extends AppCompatActivity implements View.OnClickListener {

    final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;//constant to identify permission for SD card

    ShareActionProvider mShareActionProvider;
    Intent mShareIntent;
    PhotoView mImageView;

    private Bitmap mSourceBitmap;
    File mFileUsedToShare; //Here we save result bitmap like PNG-file every time when change current Effect

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    ArrayList<Effect> mListOfEffects;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_effects);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        mImageView = (PhotoView) findViewById(R.id.photo_view);

        mListOfEffects =new ArrayList<Effect>();
        mListOfEffects.add(new EffectGray(this));
        mListOfEffects.add(new EffectBright(this));
        mListOfEffects.add(new EffectLimitedGray(this));
        mListOfEffects.add(new EffectNothing(this));

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.offsetChildrenVertical(8);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new EffectsListAdapter(mListOfEffects,this);
        mRecyclerView.setAdapter(mAdapter);

        mShareIntent = new Intent();
        mShareIntent.setAction(Intent.ACTION_SEND);
        mShareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file

    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d("Igor log","onStart");

        checkPermissionSD();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        Log.d("Igor log","onCreateOptionsMenu");

        // Inflate menu resource file.
        getMenuInflater().inflate(R.menu.menu_photo_filters_activity, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_item_share);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        if((mShareIntent!=null)&&(mSourceBitmap!=null))
            mShareActionProvider.setShareIntent(mShareIntent);


        // Return true to display menu
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_share:
                // User chose the "Settings" item, show the app settings UI...
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Good! User grant some permission to App. We don't that all Permissions were granted, so try again to check it and if all right start mCamera

                    Log.d("Igor logs","onRequestPermissionsResult.SD User grant permission to App");
                    Log.d("Igor logs","onRequestPermissionsResult.SD now check camera permission");

                    loadPictureFromIntent();

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
    public void onClick(View view) {

        Integer index=(Integer)view.getTag(EffectsListAdapter.TAG_INDEX);

        Log.d("Igor log","ViewBinder click we get in Activity index="+index);

        //Here I use algorithm of clicked item Effect, and apply result bitmap to ImageView asynchronously
        new EffectApplyTask().execute(index);


    }

    void sharedImageChanged(Bitmap bitmap){

        //Image has changed! Update the intent
        //P.S: Not in my case, cause i use only one file. The name of file will never change. but it may be needed in future


        BitmapUtils.saveImageToExternalStorage(this,mFileUsedToShare,bitmap);
        Uri contentUri = FileProvider.getUriForFile(this, "com.ivanov.tech.photomaker.fileprovider", mFileUsedToShare);
        mShareIntent.setDataAndType(contentUri, "image/");
        mShareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);

        if(mShareActionProvider!=null)
            mShareActionProvider.setShareIntent(mShareIntent);
    }

    void loadPictureFromIntent(){

        mFileUsedToShare = FileUtils.getTempFile();

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {
                    mSourceBitmap = BitmapUtils.uriToBitmap(this,imageUri);

                    mImageView.setImageBitmap(mSourceBitmap);

                    //In case if user click Share before use any Effect
                    sharedImageChanged(mSourceBitmap);

                }
            }
        }
    }

    private class EffectApplyTask extends AsyncTask<Integer, Integer, Bitmap> implements OnProgressListener{

        ProgressBar mProgressBar;
        View mGroupProgress;

        int mLastProgress;

        protected void onPreExecute() {
            mProgressBar=findViewById(R.id.progressBar);
            mProgressBar.setProgress(0);

            mGroupProgress=findViewById(R.id.group_progressbar);
            mGroupProgress.setVisibility(View.VISIBLE);
        }

        protected Bitmap doInBackground(Integer... indexes) {

            Bitmap effectedBitmap= mListOfEffects.get(indexes[0]).getEffectedBitmap(mSourceBitmap,this);

            // SHARE-code-part
            sharedImageChanged(effectedBitmap);

            return effectedBitmap;
        }

        protected void onProgressUpdate(Integer... progress) {
            mProgressBar.setProgress(progress[0]);
        }

        protected void onPostExecute(Bitmap effectedBitmap) {


//            float scaleX=mImageView.getScaleX();
//            float scaleY=mImageView.getScaleY();
//            float scale=mImageView.getScale();
//            mImageView.setImageBitmap(effectedBitmap);
//            mImageView.setScale(scale,scaleX,scaleY,false);

            Matrix matrix=new Matrix();
            mImageView.getSuppMatrix(matrix);
            mImageView.setImageBitmap(effectedBitmap);
            mImageView.setSuppMatrix(matrix);

            mGroupProgress.setVisibility(View.GONE);
        }

        @Override
        public void onProgressChanged(int changed) {
            if(changed!=mLastProgress) {
                publishProgress(changed);
                mLastProgress=changed;
            }
        }
    }

    //--------------Permissions proccessing---------------


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

                ActivityCompat.requestPermissions(PhotoEffectsActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }

        } else {
            // Permission has already been granted
            Log.d("Igor logs","checkPermissionSD Permission has already been granted");
            Log.d("Igor logs","checkPermissionSD now check camera permission");

            loadPictureFromIntent();
        }

    }

    void showPermissionSDDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(R.string.dialog_permission_sd_message)
                .setTitle(R.string.dialog_permission_sd_title);

        // Add the buttons
        builder.setPositiveButton(R.string.dialog_permission_sd_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                dialog.dismiss();

                ActivityCompat.requestPermissions(PhotoEffectsActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

            }
        });
        builder.setNegativeButton(R.string.dialog_permission_sd_no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                PhotoEffectsActivity.this.finish();
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
                PhotoEffectsActivity.this.finish();
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();

        dialog.show();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
    }

}
