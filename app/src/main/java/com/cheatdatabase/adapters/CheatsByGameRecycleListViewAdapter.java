package com.cheatdatabase.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.cheatdatabase.R;
import com.cheatdatabase.businessobjects.Cheat;
import com.cheatdatabase.events.CheatListRecyclerViewClickEvent;
import com.cheatdatabase.helpers.Konstanten;
import com.cheatdatabase.helpers.Reachability;
import com.cheatdatabase.helpers.Tools;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

@EBean
public class CheatsByGameRecycleListViewAdapter extends RecyclerView.Adapter<CheatsByGameRecycleListViewAdapter.ViewHolder> implements FastScrollRecyclerView.SectionedAdapter,
        FastScrollRecyclerView.MeasurableAdapter {

    private static final String TAG = CheatsByGameRecycleListViewAdapter.class.getSimpleName();

    private ArrayList<Cheat> mCheats;
    private Typeface latoFontBold;
    private Typeface latoFontLight;
    private Cheat cheatObj;

    @RootContext
    Context mContext;

    public void init(ArrayList<Cheat> cheatList) {
        mCheats = cheatList;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView mCheatTitle;
        RatingBar mRatingBar;
        IMyViewHolderClicks mListener;
        ImageView mFlagNewAddition;
        ImageView mFlagScreenshot;
        ImageView mFlagGerman;

        public ViewHolder(View v, IMyViewHolderClicks listener) {
            super(v);
            mListener = listener;

            mCheatTitle = v.findViewById(R.id.cheat_title);
            mCheatTitle.setTypeface(Tools.getFont(v.getContext().getAssets(), Konstanten.FONT_REGULAR));

            // Average rating (not member rating)
            mRatingBar = v.findViewById(R.id.small_ratingbar);
            mRatingBar.setNumStars(5);

            mFlagNewAddition = v.findViewById(R.id.newaddition);
            mFlagNewAddition.setImageResource(R.drawable.flag_new);
            mFlagScreenshot = v.findViewById(R.id.screenshots);
            mFlagScreenshot.setImageResource(R.drawable.flag_img);
            mFlagGerman = v.findViewById(R.id.flag);
            mFlagGerman.setImageResource(R.drawable.flag_german);

            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mListener.onCheatClick(this);
        }

    }

    public interface IMyViewHolderClicks {
        void onCheatClick(CheatsByGameRecycleListViewAdapter.ViewHolder caller);
    }

    // Create new views (invoked by the layout manager)
    @Override
    public CheatsByGameRecycleListViewAdapter.ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {

        latoFontBold = Tools.getFont(parent.getContext().getAssets(), Konstanten.FONT_BOLD);
        latoFontLight = Tools.getFont(parent.getContext().getAssets(), Konstanten.FONT_LIGHT);

        // create a new view
        final View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.listrow_cheat_item, parent, false);
        v.setDrawingCacheEnabled(true);

        return new ViewHolder(v, new IMyViewHolderClicks() {
            @Override
            public void onCheatClick(ViewHolder caller) {
                if (Reachability.reachability.isReachable) {

                    Log.d(TAG, "caller.getAdapterPosition(): " + caller.getAdapterPosition());
                    Log.d(TAG, "Cheat Title: " + mCheats.get(caller.getAdapterPosition()).getCheatTitle());

                    EventBus.getDefault().post(new CheatListRecyclerViewClickEvent(mCheats.get(caller.getAdapterPosition()), caller.getAdapterPosition()));
                } else {
                    EventBus.getDefault().post(new CheatListRecyclerViewClickEvent(new Exception()));
                }
            }
        });
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        cheatObj = mCheats.get(position);

        holder.mCheatTitle.setText(cheatObj.getCheatTitle());
        holder.mCheatTitle.setTypeface(latoFontBold);

        holder.mRatingBar.setRating(cheatObj.getRatingAverage() / 2);

        if (cheatObj.getDayAge() < Konstanten.CHEAT_DAY_AGE_SHOW_NEWADDITION_ICON) {
            holder.mFlagNewAddition.setVisibility(View.VISIBLE);
        } else {
            holder.mFlagNewAddition.setVisibility(View.GONE);
        }

        if (cheatObj.isScreenshots()) {
            holder.mFlagScreenshot.setVisibility(View.VISIBLE);
        } else {
            holder.mFlagScreenshot.setVisibility(View.GONE);
        }

        if (cheatObj.getLanguageId() == 2) { // 2 = German
            holder.mFlagGerman.setVisibility(View.VISIBLE);
        } else {
            holder.mFlagGerman.setVisibility(View.GONE);
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mCheats.size();
    }

    // Display the first letter of the cheat during fast scrolling
    @NonNull
    @Override
    public String getSectionName(int position) {
        return mCheats.get(position).getCheatTitle().substring(0, 1).toUpperCase();
    }

    // Height of the scroll-bar at the right screen side
    @Override
    public int getViewTypeHeight(RecyclerView recyclerView, @Nullable RecyclerView.ViewHolder viewHolder, int viewType) {
        return 100;
    }

}
