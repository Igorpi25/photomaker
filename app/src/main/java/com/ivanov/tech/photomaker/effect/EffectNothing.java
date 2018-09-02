package com.ivanov.tech.photomaker.effect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.ivanov.tech.photomaker.R;

public class EffectNothing implements Effect {

    Context mContext;

    public EffectNothing(Context context){
        mContext=context;
    }

    @Override
    public String getName() {
        return mContext.getString(R.string.effect_nothhing_name);
    }

    @Override
    public Bitmap getEffectedBitmap(Bitmap sourceBitmap) {

        //We should create new bitmap anyway to save origin bitmap (sourceBitmap)

        Bitmap resultBitmap = Bitmap.createBitmap(sourceBitmap.getWidth(),sourceBitmap.getHeight(), sourceBitmap.getConfig());

        return resultBitmap;
    }

}
