package com.cheatdatabase.helpers;

import com.cheatdatabase.R;

import org.androidannotations.annotations.sharedpreferences.DefaultBoolean;
import org.androidannotations.annotations.sharedpreferences.SharedPref;

@SharedPref(value = SharedPref.Scope.APPLICATION_DEFAULT)
public interface MyPrefs {

    @DefaultBoolean(value = false, keyRes = R.string.pref_enable_achievements_key)
    boolean isAchievementsEnabled();


    @DefaultBoolean(value = false)
    boolean isSeenAchievementsDialog();

}
