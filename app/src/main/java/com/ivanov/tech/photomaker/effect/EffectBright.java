package com.ivanov.tech.photomaker.effect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.ivanov.tech.photomaker.R;

public class EffectBright implements Effect {

    Context mContext;

    public EffectBright(Context context){
        mContext=context;
    }

    @Override
    public String getName() {
        return mContext.getString(R.string.effect_bright_name);
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

                float rate=2f;

                r = (int)(r*rate);
                g = (int)(g*rate);
                b = (int)(b*rate);

                r=(r>255)?255:r;
                g=(g>255)?255:g;
                b=(b>255)?255:b;

                resultBitmap.setPixel(i, j, Color.argb(Color.alpha(p), r, g, b));
            }
        }

        return resultBitmap;
    }

}
