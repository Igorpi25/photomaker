package com.ivanov.tech.photomaker.effect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.ivanov.tech.photomaker.R;

/*  Return gray-scaled picture where used limited number of colors
    Every used color is like a branch in color scale. There are same distances between branches in color scale
    Example 1: If we have two branches then there are two colors: white and black
    Example 2: If we have three branches: white, black and gray(exactly in the gray-scale's center between black and white)
*/
public class EffectLimitedGray implements Effect {

    Context mContext;
    int mBranches=4; //Number of colors (white, black and shades of gray) that will be used

    public EffectLimitedGray(Context context){
        mContext=context;
    }

    @Override
    public String getName() {
        return mContext.getString(R.string.effect_whiteblack_name);
    }

    @Override
    public Bitmap getEffectedBitmap(Bitmap sourceBitmap) {
        return  getEffectedBitmap(sourceBitmap,null);
    }

    @Override
    public Bitmap getEffectedBitmap(Bitmap sourceBitmap, OnProgressListener progressListener) {
        Bitmap resultBitmap = Bitmap.createBitmap(sourceBitmap.getWidth(),sourceBitmap.getHeight(), sourceBitmap.getConfig());

        for(int i=0; i<sourceBitmap.getWidth(); i++){

            if(progressListener!=null){
                progressListener.onProgressChanged(i*100/sourceBitmap.getWidth());
            }

            for(int j=0; j<sourceBitmap.getHeight(); j++){
                int p = sourceBitmap.getPixel(i, j);
                int r = Color.red(p);
                int g = Color.green(p);
                int b = Color.blue(p);

                float m=(r+g+b)/3.0f;

                int part=Math.round(m/255*(mBranches-1));

                int v=part* (255/(mBranches-1));

                resultBitmap.setPixel(i, j, Color.argb(Color.alpha(p), v, v, v));
            }

        }

        return resultBitmap;
    }

}
