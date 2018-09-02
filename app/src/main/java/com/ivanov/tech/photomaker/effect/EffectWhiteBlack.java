package com.ivanov.tech.photomaker.effect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.ivanov.tech.photomaker.R;

public class EffectWhiteBlack implements Effect {

    Context mContext;

    public EffectWhiteBlack(Context context){
        mContext=context;
    }

    @Override
    public String getName() {
        return mContext.getString(R.string.effect_whiteblack_name);
    }

    @Override
    public Bitmap getEffectedBitmap(Bitmap sourceBitmap) {

        Bitmap resultBitmap = Bitmap.createBitmap(sourceBitmap.getWidth(),sourceBitmap.getHeight(), sourceBitmap.getConfig());

        for(int i=0; i<sourceBitmap.getWidth(); i++){
            for(int j=0; j<sourceBitmap.getHeight(); j++){
                int p = sourceBitmap.getPixel(i, j);
                int r = Color.red(p);
                int g = Color.green(p);
                int b = Color.blue(p);

                float m=(r+g+b)/3.0f;

                int branches=5;

                int part=Math.round(m/255*(branches-1));

                int v=part* (255/(branches-1));

                resultBitmap.setPixel(i, j, Color.argb(Color.alpha(p), v, v, v));
            }
        }

        return resultBitmap;
    }

}
