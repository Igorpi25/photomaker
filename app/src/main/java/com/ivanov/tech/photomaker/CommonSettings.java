package com.ivanov.tech.photomaker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;

// Settings file for global constants
public class CommonSettings {
    public static final String TEMP_FILE_NAME = "Temp.jpg"; //Name temp file. This file is common for both two activities in App
    public static final String APP_DIRECTORY_IN_EXTERNAL_PUBLIC = "CommonSettings";//Subdirectory in Picture folder

    public static final int reqWidth=300,reqHeight=300; //Requirement size limit of result image.
    // In this app we used one algorithm to reduce size of source image for optimization issue. See calculateInSampleSize


}
