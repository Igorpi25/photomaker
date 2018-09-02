package com.ivanov.tech.photomaker.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ivanov.tech.photomaker.R;
import com.ivanov.tech.photomaker.effect.Effect;

import java.util.ArrayList;

public class EffectsListAdapter extends RecyclerView.Adapter<EffectsListAdapter.EffectItemViewHolder> implements View.OnClickListener{

    ArrayList<Effect> listOfEffects; //Objects that store algorithm and basic info, used in adapter
    View.OnClickListener mOnClickListener; //Here we hold an Activity's listener

    public final static int TAG_INDEX= R.layout.recyclerview_item;// We use it in OnClicked callback function to get index of clicked item

    private int mSelectedItemPosition=-1;//Current selected item. It used to inflate special layout for selected item, other items use default layout

    //These constants we used to determine type of items: default, selected, etc
    //P.S: Here is no real reason to use separate constants for that issue. But that's good practice, and will be useful in future
    final static int TYPE_DEFAULT_ITEM=0;
    final static int TYPE_SELECTED_ITEM=1;

    @Override
    public void onClick(View view) {

        mSelectedItemPosition=(Integer)view.getTag(TAG_INDEX);
        this.notifyDataSetChanged();// We update all items, cause there is not large number of items

        mOnClickListener.onClick(view);
    }

    public static class EffectItemViewHolder extends RecyclerView.ViewHolder {

        public TextView mTextView;
        public View layoutView;//we have to hold root view. In other JVM can remove it like a garbage

        public EffectItemViewHolder(View v) {
            super(v);
            layoutView = v;
            mTextView=(TextView)layoutView.findViewById(R.id.textView);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public EffectsListAdapter(ArrayList<Effect> data, View.OnClickListener listener) {
        mSelectedItemPosition=-1;//Initially no item is selected
        listOfEffects = data;
        mOnClickListener = listener;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public EffectItemViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {

        int layoutId = R.layout.recyclerview_item;//Use this layout by default
        if(viewType==TYPE_SELECTED_ITEM)layoutId=R.layout.recyclerview_item_selected; //use another layout for selected item

        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);

        EffectItemViewHolder vh = new EffectItemViewHolder(v);
        vh.layoutView.setOnClickListener(this);//all callbacks from items will invoke Activity, at the end

        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(EffectItemViewHolder holder, int position) {

        holder.mTextView.setText(listOfEffects.get(position).getName());

        //We put the index layouts tags to use it in OnClickListener
        holder.layoutView.setTag(TAG_INDEX,(Integer)position);

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return listOfEffects.size();
    }

    // Return the type of item: selected, default
    @Override
    public int getItemViewType(int position){

        return (position==mSelectedItemPosition) ? TYPE_SELECTED_ITEM : TYPE_DEFAULT_ITEM ;
    }

    //Return the current selected item index
    public int getSelectedItemPosition(){
        return mSelectedItemPosition;
    }

}
