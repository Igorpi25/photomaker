package com.ivanov.tech.photomaker.util;

import android.os.Environment;

import com.ivanov.tech.photomaker.CommonSettings;

import java.io.File;

//
public class FileUtils {

    //Create dir in Picture folder, create Temp.png if not exists and return
    static public File getTempFile(){

        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), CommonSettings.APP_DIRECTORY_IN_EXTERNAL_PUBLIC);
        if (!dir.exists())
        {
            dir.mkdirs();
        }

        File tempFile = new File(dir, CommonSettings.TEMP_FILE_NAME);

        return tempFile;
    }
}
