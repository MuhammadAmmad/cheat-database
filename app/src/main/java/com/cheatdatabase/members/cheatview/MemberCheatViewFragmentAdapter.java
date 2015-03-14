package com.cheatdatabase.members.cheatview;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.cheatdatabase.businessobjects.Cheat;

public class MemberCheatViewFragmentAdapter extends FragmentPagerAdapter {

    protected static String[] CONTENT = new String[]{"T1", "T2", "T3"};
    private int mCount = CONTENT.length;

    // Icons not used....
    protected static final int[] ICONS = new int[]{android.R.drawable.btn_plus, android.R.drawable.btn_plus, android.R.drawable.btn_plus, android.R.drawable.btn_plus};

    private Cheat[] cheats;

    public MemberCheatViewFragmentAdapter(FragmentManager fm, Cheat[] cheats, String[] cheatTitleNames) {
        super(fm);
        this.cheats = cheats;
        CONTENT = cheatTitleNames;
        mCount = CONTENT.length;
    }

    @Override
    public Fragment getItem(int position) {
        if (CONTENT == null) {
            CONTENT = new String[cheats.length];
        }
        return MemberCheatViewFragment.newInstance(CONTENT[position % CONTENT.length], cheats, position);
    }

    @Override
    public int getCount() {
        return mCount;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return MemberCheatViewFragmentAdapter.CONTENT[position % CONTENT.length];
    }

    public int getIconResId(int index) {
        return ICONS[index % ICONS.length];
    }

    public void setCount(int count) {
        if (count > 0 && count <= 10) {
            mCount = count;
            notifyDataSetChanged();
        }
    }

}
