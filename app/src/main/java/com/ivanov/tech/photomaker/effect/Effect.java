package com.ivanov.tech.photomaker.effect;

import android.graphics.Bitmap;

public interface Effect {
    String getName();
    Bitmap getEffectedBitmap(Bitmap sourceBitmap);
}
