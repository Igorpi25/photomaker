package com.ivanov.tech.photomaker.effect;

import android.graphics.Bitmap;
import android.os.AsyncTask;

//Basic level of Effects. Every new effect must implement this interface
public interface Effect {
    String getName(); //Title-string name of Effect. Used in EffectsList
    Bitmap getEffectedBitmap(Bitmap sourceBitmap); //Here we must incapsulate algorithm of Effect
    Bitmap getEffectedBitmap(Bitmap sourceBitmap, OnProgressListener progressListener);
}

