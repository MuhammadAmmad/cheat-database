package com.cheatdatabase.members.cheatview;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.cheatdatabase.CheatForumActivity_;
import com.cheatdatabase.LoginActivity;
import com.cheatdatabase.R;
import com.cheatdatabase.SubmitCheatActivity_;
import com.cheatdatabase.businessobjects.Cheat;
import com.cheatdatabase.businessobjects.Game;
import com.cheatdatabase.businessobjects.Member;
import com.cheatdatabase.dialogs.CheatMetaDialog;
import com.cheatdatabase.dialogs.RateCheatDialog;
import com.cheatdatabase.dialogs.ReportCheatDialog;
import com.cheatdatabase.events.CheatRatingFinishedEvent;
import com.cheatdatabase.events.CheatReportingFinishedEvent;
import com.cheatdatabase.helpers.Helper;
import com.cheatdatabase.helpers.Konstanten;
import com.cheatdatabase.helpers.Reachability;
import com.cheatdatabase.helpers.Tools;
import com.google.gson.Gson;
import com.mopub.mobileads.MoPubView;


import net.lucode.hackware.magicindicator.MagicIndicator;
import net.lucode.hackware.magicindicator.ViewPagerHelper;
import net.lucode.hackware.magicindicator.buildins.UIUtil;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerIndicator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerTitleView;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.indicators.LinePagerIndicator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.ColorTransitionPagerTitleView;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.SimplePagerTitleView;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Horizontal sliding through cheats submitted by member.
 *
 * @author Dominik Erbsland
 * @version 1.0
 */
@EBean
public class MemberCheatViewPageIndicator extends AppCompatActivity {

    private final String TAG = MemberCheatViewPageIndicator.class.getName();

    @Bean
    Tools tools;

    private Intent intent;

    private View viewLayout;
    private int pageSelected;

    private ArrayList<Cheat> cheatArrayList;
    private Cheat visibleCheat;
    private Game gameObj;

    private MoPubView mAdView;

    private SharedPreferences settings;
    private Editor editor;

    private Member member;

    public AlertDialog.Builder builder;

    private MemberCheatViewFragmentAdapter mAdapter;
    private ViewPager mPager;

    private int activePage;

    private ShareActionProvider mShare;
    private Toolbar mToolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intent = getIntent();

        LayoutInflater inflater = LayoutInflater.from(this);
        viewLayout = inflater.inflate(R.layout.activity_cheatview_pager, null);
        setContentView(viewLayout);

        init();

        try {
            Cheat[] cheatObjX = new Gson().fromJson(settings.getString(Konstanten.PREFERENCES_TEMP_CHEAT_ARRAY_OBJECT_VIEW, null), Cheat[].class);

            cheatArrayList = new ArrayList<>(Arrays.asList(cheatObjX));

            if (cheatArrayList == null) {
                cheatArrayList = (ArrayList<Cheat>) intent.getSerializableExtra("cheatArrayList");
            }
            pageSelected = intent.getIntExtra("selectedPage", 0);
            activePage = pageSelected;

            visibleCheat = cheatArrayList.get(pageSelected);
            gameObj = new Game();
            gameObj.setCheat(visibleCheat);
            gameObj.setGameId(visibleCheat.getGameId());
            gameObj.setGameName(visibleCheat.getGameName());
            gameObj.setSystemId(visibleCheat.getSystemId());
            gameObj.setSystemName(visibleCheat.getSystemName());

            getSupportActionBar().setTitle(visibleCheat.getGameName());
            getSupportActionBar().setSubtitle(visibleCheat.getSystemName());

            initialisePaging();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void init() {


        settings = getSharedPreferences(Konstanten.PREFERENCES_FILE, 0);
        editor = settings.edit();

        mAdView = Tools.initMoPubAdView(this, mAdView);
        mToolbar = Tools.initToolbarBase(this, mToolbar);

        member = new Gson().fromJson(settings.getString(Konstanten.MEMBER_OBJECT, null), Member.class);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!Reachability.isRegistered()) {
            Reachability.registerReachability(this);
        }
        member = new Gson().fromJson(settings.getString(Konstanten.MEMBER_OBJECT, null), Member.class);
        invalidateOptionsMenu();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        Reachability.unregister(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
    }

    private void initialisePaging() {
        final String[] cheatTitles = new String[cheatArrayList.size()];
        for (int i = 0; i < cheatArrayList.size(); i++) {
            cheatTitles[i] = cheatArrayList.get(i).getCheatTitle();
        }

        try {
            mAdapter = new MemberCheatViewFragmentAdapter(getSupportFragmentManager(), cheatArrayList, cheatTitles);

            mPager = (ViewPager) viewLayout.findViewById(R.id.pager);
            mPager.setAdapter(mAdapter);
            mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                }

                @Override
                public void onPageSelected(int position) {
                    // Save selected page
                    editor.putInt(Konstanten.PREFERENCES_PAGE_SELECTED, position);
                    editor.commit();

                    activePage = position;

                    try {
                        visibleCheat = cheatArrayList.get(position);
                        invalidateOptionsMenu();

                        gameObj = new Game();
                        gameObj.setCheat(visibleCheat);
                        gameObj.setGameId(visibleCheat.getGameId());
                        gameObj.setGameName(visibleCheat.getGameName());
                        gameObj.setSystemId(visibleCheat.getSystemId());
                        gameObj.setSystemName(visibleCheat.getSystemName());

                        getSupportActionBar().setTitle(visibleCheat.getGameName());
                        getSupportActionBar().setSubtitle(visibleCheat.getSystemName());
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                        Toast.makeText(MemberCheatViewPageIndicator.this, R.string.err_somethings_wrong, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            });

            MagicIndicator magicIndicator = (MagicIndicator) findViewById(R.id.magic_indicator);
            CommonNavigator commonNavigator = new CommonNavigator(this);
            commonNavigator.setSkimOver(true);
            commonNavigator.setAdapter(new CommonNavigatorAdapter() {
                @Override
                public int getCount() {
                    return cheatArrayList == null ? 0 : cheatArrayList.size();
                }

                @Override
                public IPagerTitleView getTitleView(Context context, final int index) {
                    SimplePagerTitleView clipPagerTitleView = new ColorTransitionPagerTitleView(context);
                    clipPagerTitleView.setText(cheatTitles[index]);
                    clipPagerTitleView.setNormalColor(Color.parseColor("#88ffffff")); // White transparent
                    clipPagerTitleView.setSelectedColor(Color.WHITE);
                    clipPagerTitleView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mPager.setCurrentItem(index);
                        }
                    });
                    return clipPagerTitleView;
                }

                @Override
                public IPagerIndicator getIndicator(Context context) {
                    LinePagerIndicator indicator = new LinePagerIndicator(context);
                    indicator.setMode(LinePagerIndicator.MODE_EXACTLY);
                    indicator.setYOffset(UIUtil.dip2px(context, 3));
                    indicator.setColors(Color.WHITE);
                    return indicator;
                }
            });
            magicIndicator.setNavigator(commonNavigator);
            ViewPagerHelper.bind(magicIndicator, mPager);
            mPager.setCurrentItem(pageSelected);

            FloatingActionButton fa = (FloatingActionButton) viewLayout.findViewById(R.id.add_new_cheat_button);
            fa.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SubmitCheatActivity_.intent(MemberCheatViewPageIndicator.this).gameObj(gameObj).start();
                }
            });
        } catch (Exception e2) {
            Log.e(TAG, "ERROR: " + getPackageName() + "/" + getTitle() + "... " + e2.getMessage());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            // Return result code. Login success, Register success etc.
            int intentReturnCode = data.getIntExtra("result", Konstanten.LOGIN_REGISTER_FAIL_RETURN_CODE);

            if (requestCode == Konstanten.LOGIN_REGISTER_OK_RETURN_CODE) {
                member = new Gson().fromJson(settings.getString(Konstanten.MEMBER_OBJECT, null), Member.class);
                invalidateOptionsMenu();
                if ((member != null) && intentReturnCode == Konstanten.REGISTER_SUCCESS_RETURN_CODE) {
                    Toast.makeText(MemberCheatViewPageIndicator.this, R.string.register_thanks, Toast.LENGTH_LONG).show();
                } else if ((member != null) && intentReturnCode == Konstanten.REGISTER_SUCCESS_RETURN_CODE) {
                    Toast.makeText(MemberCheatViewPageIndicator.this, R.string.login_ok, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if ((visibleCheat != null) && (visibleCheat.getMemberRating() > 0)) {
            getMenuInflater().inflate(R.menu.handset_cheatview_rating_on_menu, menu);
        } else {
            getMenuInflater().inflate(R.menu.handset_cheatview_rating_off_menu, menu);
        }

        if (member != null) {
            getMenuInflater().inflate(R.menu.signout_menu, menu);
        } else {
            getMenuInflater().inflate(R.menu.signin_menu, menu);
        }

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.action_share);

        // Sharing
        mShare = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

        // Search
        getMenuInflater().inflate(R.menu.search_menu, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        return super.onCreateOptionsMenu(menu);
    }

    // Call to update the share intent
    private void setShareIntent(Intent shareIntent) {
        if (mShare != null) {
            mShare.setShareIntent(shareIntent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_submit_cheat:
                Intent explicitIntent = new Intent(MemberCheatViewPageIndicator.this, SubmitCheatActivity_.class);
                explicitIntent.putExtra("gameObj", gameObj);
                startActivity(explicitIntent);
                return true;
            case R.id.action_rate:
                showRatingDialog();
                return true;
            case R.id.action_forum:
                CheatForumActivity_.intent(this).gameObj(gameObj).cheatObj(visibleCheat).start();
                return true;
            case R.id.action_add_to_favorites:
                Toast.makeText(MemberCheatViewPageIndicator.this, R.string.favorite_adding, Toast.LENGTH_SHORT).show();
                Helper.addFavorite(MemberCheatViewPageIndicator.this, visibleCheat);
                return true;
            case R.id.action_report:
                showReportDialog();
                return true;
            case R.id.action_metainfo:
                CheatMetaDialog cmDialog = new CheatMetaDialog(MemberCheatViewPageIndicator.this, visibleCheat);
                cmDialog.show();
                return true;
            case R.id.action_login:
                Intent loginIntent = new Intent(MemberCheatViewPageIndicator.this, LoginActivity.class);
                startActivityForResult(loginIntent, Konstanten.LOGIN_REGISTER_OK_RETURN_CODE);
                return true;
            case R.id.action_logout:
                member = null;
                tools.logout(MemberCheatViewPageIndicator.this, editor);
                invalidateOptionsMenu();
                return true;
            case R.id.action_share:
                setShareIntent(tools.setShareText(MemberCheatViewPageIndicator.this, visibleCheat));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public void showReportDialog() {
        if ((member == null) || (member.getMid() == 0)) {
            Toast.makeText(MemberCheatViewPageIndicator.this, R.string.error_login_required, Toast.LENGTH_SHORT).show();
        } else {
            Bundle args = new Bundle();
            args.putSerializable("cheatArrayList", visibleCheat);

            FragmentManager fm = getSupportFragmentManager();
            ReportCheatDialog reportCheatDialog = new ReportCheatDialog();
            reportCheatDialog.setArguments(args);
            reportCheatDialog.show(fm, "fragment_report_cheat");
        }
    }

    @Subscribe
    public void onEvent(CheatReportingFinishedEvent result) {
        if (result.isSucceeded()) {
            Toast.makeText(this, R.string.thanks_for_reporting, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.no_internet, Toast.LENGTH_SHORT).show();
        }
    }

    public void showRatingDialog() {
        if ((member == null) || (member.getMid() == 0)) {
            Toast.makeText(this, R.string.error_login_required, Toast.LENGTH_LONG).show();
        } else {
            Bundle args = new Bundle();
            args.putSerializable("cheatArrayList", visibleCheat);

            FragmentManager fm = getSupportFragmentManager();
            RateCheatDialog ratingCheatDialog = new RateCheatDialog();
            ratingCheatDialog.setArguments(args);
            ratingCheatDialog.show(fm, "fragment_rating_cheat");
        }
    }

    @Subscribe
    public void onEvent(CheatRatingFinishedEvent result) {
        visibleCheat.setMemberRating(result.getRating());
        cheatArrayList.get(activePage).setMemberRating(result.getRating());
        invalidateOptionsMenu();
        Toast.makeText(this, R.string.rating_inserted, Toast.LENGTH_SHORT).show();
    }

    public void setRating(int position, float rating) {
        cheatArrayList.get(position).setMemberRating(rating);
        invalidateOptionsMenu();
    }
}