package com.ivanov.tech.photomaker.ui;

import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
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
import android.widget.ImageView;

import com.ivanov.tech.photomaker.R;
import com.ivanov.tech.photomaker.adapter.EffectsListAdapter;
import com.ivanov.tech.photomaker.util.BitmapUtils;
import com.ivanov.tech.photomaker.util.FileUtils;
import com.ivanov.tech.photomaker.effect.Effect;
import com.ivanov.tech.photomaker.effect.EffectBright;
import com.ivanov.tech.photomaker.effect.EffectGray;
import com.ivanov.tech.photomaker.effect.EffectNothing;
import com.ivanov.tech.photomaker.effect.EffectWhiteBlack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class PhotoEffectsActivity extends AppCompatActivity implements View.OnClickListener {

    ShareActionProvider mShareActionProvider;
    Intent mShareIntent;

    ImageView mImageView;

    private Bitmap mSourceBitmapFromIntent;

    File mFileUsedToShare; //Here we save result bitmap like PNG-file every time when change current Effect

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    ArrayList<Effect> mListOfEffects;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_filters);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        mImageView = (ImageView) findViewById(R.id.imageView);


        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        mFileUsedToShare = FileUtils.getTempFile();


        mListOfEffects =new ArrayList<Effect>();
        mListOfEffects.add(new EffectGray(this));
        mListOfEffects.add(new EffectBright(this));
        mListOfEffects.add(new EffectWhiteBlack(this));
        mListOfEffects.add(new EffectNothing(this));

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new EffectsListAdapter(mListOfEffects,this);
        mRecyclerView.setAdapter(mAdapter);

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {
                    mSourceBitmapFromIntent = BitmapUtils.uriToBitmap(this,imageUri);
                    mImageView.setImageBitmap(mSourceBitmapFromIntent);

                    saveImageToExternalStorage(mSourceBitmapFromIntent);//In case if user click Share before use any Effect
                }
            }
        }

    }

    void sharedImageChanged(Bitmap bitmap){

        //Image has changed! Update the intent
        //P.S: Not in my case, cause i use only one file. The name of file will never change. but it may be needed in future

        saveImageToExternalStorage(bitmap);
        Uri contentUri = FileProvider.getUriForFile(this, "com.ivanov.tech.photomaker.fileprovider", mFileUsedToShare);
        mShareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        mShareActionProvider.setShareIntent(mShareIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu resource file.
        getMenuInflater().inflate(R.menu.menu_photo_filters_activity, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_item_share);

        // Fetch and store ShareActionProvider
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

        Uri contentUri = FileProvider.getUriForFile(this, "com.ivanov.tech.photomaker.fileprovider", mFileUsedToShare);

        mShareIntent = new Intent();
        mShareIntent.setAction(Intent.ACTION_SEND);
        mShareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
        mShareIntent.setDataAndType(contentUri, "image/");
        mShareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);

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
    public void onClick(View view) {

        Integer index=(Integer)view.getTag(EffectsListAdapter.TAG_INDEX);

        Log.d("Igor log","ViewBinder click we get in Activity index="+index);

        //Here I use algorithm of clicked item Effect, and apply result bitmap to ImageView
        Bitmap effectedBitmap= mListOfEffects.get(index).getEffectedBitmap(mSourceBitmapFromIntent);
        mImageView.setImageBitmap(effectedBitmap);


        // SHARE-code-part
        sharedImageChanged(effectedBitmap);
    }

    public boolean saveImageToExternalStorage(Bitmap image) {

        try {


            OutputStream fOut = null;
            mFileUsedToShare.createNewFile();
            fOut = new FileOutputStream(mFileUsedToShare);

            // 100 means no compression, the lower you go, the stronger the compression
            image.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();

            MediaStore.Images.Media.insertImage(this.getContentResolver(), mFileUsedToShare.getAbsolutePath(), mFileUsedToShare.getName(), mFileUsedToShare.getName());

            return true;

        } catch (Exception e) {
            Log.e("saveToExternalStorage()", e.getMessage());
            return false;
        }
    }


}
