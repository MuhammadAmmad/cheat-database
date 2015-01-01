package com.cheatdatabase;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazon.device.ads.Ad;
import com.amazon.device.ads.AdError;
import com.amazon.device.ads.AdListener;
import com.amazon.device.ads.AdProperties;
import com.amazon.device.ads.AdRegistration;
import com.appbrain.AppBrain;
import com.cheatdatabase.fragments.ContactFormFragment;
import com.cheatdatabase.fragments.FavoriteGamesListFragment;
import com.cheatdatabase.fragments.NewsFragment;
import com.cheatdatabase.fragments.SubmitCheatFragment;
import com.cheatdatabase.fragments.SystemListFragment;
import com.cheatdatabase.fragments.TopMembersFragment;
import com.cheatdatabase.helpers.Group;
import com.cheatdatabase.helpers.Konstanten;
import com.cheatdatabase.helpers.Reachability;
import com.cheatdatabase.helpers.Tools;
import com.cheatdatabase.navigationdrawer.CustomDrawerAdapter;
import com.cheatdatabase.navigationdrawer.DrawerItem;
import com.cheatdatabase.pojo.Member;
import com.cheatdatabase.search.SearchSuggestionProvider;
import com.google.analytics.tracking.android.Tracker;
import com.google.gson.Gson;
import com.mopub.mobileads.MoPubView;

// FragmentActivity replaces by ActionBarActivity (http://antonioleiva.com/material-design-everywhere/)
public class MainActivity extends ActionBarActivity implements ActionBar.OnNavigationListener, AdListener {

	private static Typeface latoFontBold;

	private Tracker tracker;

	// Navigation Drawer
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private CharSequence mTitle;
	private CharSequence mDrawerTitle;
	// private String[] mNavigationDrawerListEntries;
	List<DrawerItem> dataList;
	CustomDrawerAdapter mAdapter;

	// more efficient than HashMap for mapping integers to objects
	SparseArray<Group> groups = new SparseArray<Group>();

	private Member member;

	// private String[] allSystems;
	// private String[] allSystemsPlusEmpty;

	private SharedPreferences settings;
	private Editor editor;

	private ViewGroup adViewContainer;
	private com.amazon.device.ads.AdLayout amazonAdView;
	private boolean amazonAdEnabled;
	private MoPubView mAdView;

	private static final String SCREEN_LABEL = "Main Activity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		setTitle(R.string.app_name);
		//Tools.styleActionbar(this);
		Reachability.registerReachability(this.getApplicationContext());

		settings = getSharedPreferences(Konstanten.PREFERENCES_FILE, 0);
		editor = settings.edit();

		member = new Gson().fromJson(settings.getString(Konstanten.MEMBER_OBJECT, null), Member.class);

		latoFontBold = Tools.getFont(getAssets(), "Lato-Bold.ttf");

		// Set up the action bar to show a dropdown list.
		// ActionBar actionBar = getActionBar();
		// actionBar.setDisplayShowTitleEnabled(false);
		// actionBar.setDisplayHomeAsUpEnabled(true); // Used for Drawer
		// actionBar.setHomeButtonEnabled(true);

		// Set up the dropdown list navigation in the action bar.
		// allSystems = Tools.getSystemNames(this);
		// allSystemsPlusEmpty = new String[allSystems.length + 1];
		// allSystemsPlusEmpty[0] = getString(R.string.list_games);
		// for (int i = 1; i <= allSystems.length; i++) {
		// allSystemsPlusEmpty[i] = allSystems[i - 1];
		// }


		init();

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar != null) {
			setSupportActionBar(toolbar);			
		}

//		DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
//		drawerLayout.setStatusBarBackgroundColor(getResources().getColor(R.color.page_indicator_background));
		
		// Create Drawer
//		createNavigationDrawer(savedInstanceState);
	}

	/**
	 * Custom adapter dropdown menu with all the game systems.
	 * 
	 * @author Dominik
	 * 
	 */
	public class GameSystemsAdapter extends ArrayAdapter<String> {
		private final Context context;
		private final String[] values;

		public GameSystemsAdapter(Context context, String[] values) {
			super(context, R.layout.gamesystem_dropdown_menu_item, values);
			this.context = context;
			this.values = values;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

			View rowView = inflater.inflate(R.layout.gamesystem_dropdown_menu_item, parent, false);

			TextView textView = (TextView) rowView.findViewById(R.id.system_name);
			textView.setTypeface(latoFontBold);
			textView.setText(values[position]);

			return rowView;
		}
	}

	private void init() {
		AppBrain.init(this);
		Tools.initGA(MainActivity.this, tracker, SCREEN_LABEL, "Main Activity", "Cheat-Database Main Activity");

		// Initialize ad views
		AdRegistration.setAppKey(Konstanten.AMAZON_API_KEY);
		amazonAdView = new com.amazon.device.ads.AdLayout(this, com.amazon.device.ads.AdSize.SIZE_320x50);
		amazonAdView.setListener(this);
		// Initialize view container
		adViewContainer = (ViewGroup) findViewById(R.id.adview);
		amazonAdEnabled = true;
		adViewContainer.addView(amazonAdView);
		amazonAdView.loadAd(new com.amazon.device.ads.AdTargetingOptions());

		mAdView = Tools.createMoPubAdView(this);
		
		// TODO FIXME - find out where this part was before and re-add it.
		FragmentManager frgManager = getFragmentManager();
		frgManager.beginTransaction().replace(R.id.content_frame, new SystemListFragment()).commit();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// selectItem(0); // FIXME here maybe preserving fragment ID
		member = new Gson().fromJson(settings.getString(Konstanten.MEMBER_OBJECT, null), Member.class);
	}

	// http://developer.android.com/training/implementing-navigation/nav-drawer.html#Init
	private void createNavigationDrawer(Bundle savedInstanceState) {
		mTitle = mDrawerTitle = getTitle();
		// mNavigationDrawerListEntries =
		// getResources().getStringArray(R.array.main_navigation_drawer);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);

		// set custom shadow that overlays main content when drawer opens
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

		// Add Drawer Item to dataList
		dataList = new ArrayList<DrawerItem>();
		dataList.add(new DrawerItem(getString(R.string.goto_main_screen), R.drawable.ic_home));
		dataList.add(new DrawerItem(getString(R.string.goto_news), R.drawable.ic_info));
		dataList.add(new DrawerItem(getString(R.string.favorites), R.drawable.ic_favorite));
		dataList.add(new DrawerItem(getString(R.string.top_members_title), R.drawable.ic_topmembers));
		dataList.add(new DrawerItem(getString(R.string.submit_cheat_title), R.drawable.ic_submit));
		dataList.add(new DrawerItem(getString(R.string.menu_more_apps), R.drawable.ic_otherapps));
		dataList.add(new DrawerItem(getString(R.string.contactform_title), R.drawable.ic_contact));
		dataList.add(new DrawerItem(getString(R.string.rate_us), R.drawable.ic_rate));

//		mAdapter = new CustomDrawerAdapter(this, R.layout.custom_drawer_item, dataList);
//		mDrawerList.setAdapter(mAdapter);
//
//		// Set the list's click listener
//		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

//		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
//
//			/** Called when a drawer has settled in a completely closed state. */
//			@Override
//			public void onDrawerClosed(View view) {
//				getActionBar().setTitle(mTitle);
//				invalidateOptionsMenu();
//			}
//
//			/** Called when a drawer has settled in a completely open state. */
//			@Override
//			public void onDrawerOpened(View drawerView) {
//				getActionBar().setTitle(mDrawerTitle);
//
//				// creates call to onPrepareOptionsMenu()
//				invalidateOptionsMenu();
//			}
//		};

		// Set the drawer toggle as the DrawerListener
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		if (savedInstanceState == null) {
			int position = settings.getInt(Konstanten.PREFERENCES_SELECTED_DRAWER_FRAGMENT_ID, 99);
			selectItem(position);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// menu.clear();
		if (member != null) {
			getMenuInflater().inflate(R.menu.signout_menu, menu);
		} else {
			getMenuInflater().inflate(R.menu.signin_menu, menu);
		}

		getMenuInflater().inflate(R.menu.clear_search_history_menu, menu);

		// Search
		// Associate searchable configuration with the SearchView
		getMenuInflater().inflate(R.menu.search_menu, menu);
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
//		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

		return super.onCreateOptionsMenu(menu);
	}

	/* Called whenever we call invalidateOptionsMenu() */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		if (member != null) {
			getMenuInflater().inflate(R.menu.signout_menu, menu);
		} else {
			getMenuInflater().inflate(R.menu.signin_menu, menu);
		}
		getMenuInflater().inflate(R.menu.clear_search_history_menu, menu);

		// Search
		// Associate searchable configuration with the SearchView
		getMenuInflater().inflate(R.menu.search_menu, menu);
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
//		searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

		// If the nav drawer is open, hide action items related to the content
		// view
//		boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
//		menu.findItem(R.id.search).setVisible(!drawerOpen);

		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * Backward-compatible version of {@link android.app.ActionBar#getThemedContext()} that
	 * simply returns the {@link android.app.Activity} if
	 * <code>getThemedContext</code> is unavailable.
	 */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private Context getActionBarThemedContextCompat() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return getActionBar().getThemedContext();
		} else {
			return this;
		}
	}

	@Override
	public boolean onNavigationItemSelected(int position, long id) {

		// SELECTION OF SYSTEM IN DROPDOWN MENU
		// if (position > 0) {
		// // List all Games from selected System
		// Intent explicitIntent = new Intent(this,
		// GamesBySystemActivity.class);
		// explicitIntent.putExtra("systemObj",
		// Tools.getSystemObjectByName(this, allSystems[position - 1]));
		// startActivity(explicitIntent);
		// } else {
		Fragment fragment = new SystemListFragment();
		getActionBar().setTitle(R.string.app_name);

		FragmentManager frgManager = getFragmentManager();
		frgManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

		mDrawerList.setItemChecked(position, true);
		setTitle(dataList.get(position).getItemName());
		mDrawerLayout.closeDrawer(mDrawerList);
		// }

		return true;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			// Return result code. Login success, Register success etc.
			int intentReturnCode = data.getIntExtra("result", Konstanten.LOGIN_REGISTER_FAIL_RETURN_CODE);

			if (intentReturnCode == Konstanten.LOGIN_SUCCESS_RETURN_CODE) {
				member = new Gson().fromJson(settings.getString(Konstanten.MEMBER_OBJECT, null), Member.class);
				Toast.makeText(MainActivity.this, R.string.login_ok, Toast.LENGTH_LONG).show();
			} else if (intentReturnCode == Konstanten.REGISTER_SUCCESS_RETURN_CODE) {
				member = new Gson().fromJson(settings.getString(Konstanten.MEMBER_OBJECT, null), Member.class);
				Toast.makeText(MainActivity.this, R.string.register_thanks, Toast.LENGTH_LONG).show();
			}
			invalidateOptionsMenu();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// The action bar home/up action should open or close the drawer.
		// ActionBarDrawerToggle will take care of this.
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		// Handle action buttons
		switch (item.getItemId()) {
		case R.id.action_clear_search_history:
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, SearchSuggestionProvider.AUTHORITY, SearchSuggestionProvider.MODE);
			suggestions.clearHistory();
			Toast.makeText(MainActivity.this, R.string.search_history_cleared, Toast.LENGTH_LONG).show();
			return true;
		case R.id.action_login:
			Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
			startActivityForResult(loginIntent, Konstanten.LOGIN_REGISTER_OK_RETURN_CODE);
			return true;
		case R.id.action_logout:
			member = null;
			Tools.logout(MainActivity.this, settings.edit());
			invalidateOptionsMenu();
			return true;
		default:
			return false;
		}
	}

	// See strings.xml for menu list. Don't change the order!
	public static final int DRAWER_MAIN = 0;
	public static final int DRAWER_NEWS = 1;
	public static final int DRAWER_FAVORITES = 2;
	public static final int DRAWER_TOP_MEMBERS = 3;
	public static final int DRAWER_SUBMIT_CHEAT = 4;
	public static final int DRAWER_MORE_APPS = 5;
	public static final int DRAWER_CONTACT = 6;
	public static final int DRAWER_RATE_APP = 7;

	// update the main content by replacing fragments
	private void selectItem(int position) {
		Fragment fragment = null;
		Bundle args = new Bundle();
		boolean isFragment = false;

		ActionBar actionBar = getActionBar();

		switch (position) {
		case DRAWER_MAIN:
			fragment = new SystemListFragment();
			args.putString(SystemListFragment.ITEM_NAME, dataList.get(position).getItemName());
			args.putInt(SystemListFragment.IMAGE_RESOURCE_ID, dataList.get(position).getImgResID());
			// actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			// actionBar.setListNavigationCallbacks(new
			// GameSystemsAdapter(getActionBarThemedContextCompat(),
			// allSystemsPlusEmpty), this);
			actionBar.setTitle(R.string.app_name);
			isFragment = true;
			break;
		case DRAWER_NEWS:
			fragment = new NewsFragment();
			// Remove System-Select Drop-Down
			actionBar.setListNavigationCallbacks(null, null);
			actionBar.setNavigationMode(0);
			actionBar.setTitle(R.string.news_title);
			isFragment = true;
			break;
		case DRAWER_FAVORITES:
			fragment = new FavoriteGamesListFragment();
			args.putString(FavoriteGamesListFragment.ITEM_NAME, dataList.get(position).getItemName());
			args.putInt(FavoriteGamesListFragment.IMAGE_RESOURCE_ID, dataList.get(position).getImgResID());
			// Remove System-Select Drop-Down
			actionBar.setListNavigationCallbacks(null, null);
			actionBar.setNavigationMode(0);
			actionBar.setTitle(R.string.favorites);
			isFragment = true;
			break;
		case DRAWER_TOP_MEMBERS:
			fragment = new TopMembersFragment();
			args.putString(TopMembersFragment.ITEM_NAME, dataList.get(position).getItemName());
			args.putInt(TopMembersFragment.IMAGE_RESOURCE_ID, dataList.get(position).getImgResID());
			actionBar.setNavigationMode(0);
			// Remove System-Select Drop-Down
			actionBar.setListNavigationCallbacks(null, null);
			actionBar.setTitle(R.string.top_members_top_helping);
			isFragment = true;
			break;
		case DRAWER_SUBMIT_CHEAT:
			fragment = new SubmitCheatFragment();
			args.putString(SubmitCheatFragment.ITEM_NAME, dataList.get(position).getItemName());
			args.putInt(SubmitCheatFragment.IMAGE_RESOURCE_ID, dataList.get(position).getImgResID());
			actionBar.setNavigationMode(0);
			// Remove System-Select Drop-Down
			actionBar.setListNavigationCallbacks(null, null);
			actionBar.setTitle(R.string.submit_cheat_short);
			isFragment = true;
			break;
		case DRAWER_CONTACT:
			fragment = new ContactFormFragment();
			args.putString(ContactFormFragment.ITEM_NAME, dataList.get(position).getItemName());
			args.putInt(ContactFormFragment.IMAGE_RESOURCE_ID, dataList.get(position).getImgResID());
			actionBar.setNavigationMode(0);
			// Remove System-Select Drop-Down
			actionBar.setListNavigationCallbacks(null, null);
			actionBar.setTitle(R.string.contactform_title);
			isFragment = true;
			break;
		case DRAWER_MORE_APPS:
			Uri uri = Uri.parse(Konstanten.URL_MORE_APPS);
			Intent intentMoreApps = new Intent(Intent.ACTION_VIEW, uri);
			if (intentMoreApps.resolveActivity(getPackageManager()) != null) {
				startActivity(intentMoreApps);
			} else {
				Toast.makeText(MainActivity.this, R.string.err_other_problem, Toast.LENGTH_LONG).show();
			}
			break;
		case DRAWER_RATE_APP:
			Uri appUri = Uri.parse(Konstanten.GOOGLE_PLAY_URL);
			Intent intentRateApp = new Intent(Intent.ACTION_VIEW, appUri);
			if (intentRateApp.resolveActivity(getPackageManager()) != null) {
				startActivity(intentRateApp);
			} else {
				Toast.makeText(MainActivity.this, R.string.err_other_problem, Toast.LENGTH_LONG).show();
			}
			break;
		default:
			break;
		}

		if (isFragment) {
			fragment.setArguments(args);
			FragmentManager frgManager = getFragmentManager();
			frgManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

			mDrawerList.setItemChecked(position, true);
			setTitle(dataList.get(position).getItemName());
			mDrawerLayout.closeDrawer(mDrawerList);
		}

	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		//getActionBar().setTitle(mTitle);
//		getSupportActionBar().setTitle(mTitle);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
//		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggls
		mDrawerToggle.onConfigurationChanged(newConfig); 
	}

	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Log.i("DRAWER", position + "");
			editor.putInt(Konstanten.PREFERENCES_SELECTED_DRAWER_FRAGMENT_ID, position);
			editor.commit();
			selectItem(position);
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		editor.putInt(Konstanten.PREFERENCES_SELECTED_DRAWER_FRAGMENT_ID, 0);
		editor.commit();
		AppBrain.getAds().maybeShowInterstitial(this);
		finish();
	}

	@Override
	public void onAdCollapsed(Ad arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onAdDismissed(Ad arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onAdExpanded(Ad arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onAdFailedToLoad(Ad arg0, AdError arg1) {
		Log.d("AMAZON ADS", "onAdFailedToLoad");
		// Call AdMob SDK for backfill
		if (amazonAdEnabled) {
			amazonAdEnabled = false;
			adViewContainer.removeView(amazonAdView);
			adViewContainer.addView(mAdView);
		}

		mAdView.loadAd();
	}

	@Override
	public void onAdLoaded(Ad arg0, AdProperties arg1) {
		Log.d("AMAZON ADS", "onAdLoaded");
		if (!amazonAdEnabled) {
			amazonAdEnabled = true;
			adViewContainer.removeView(mAdView);
			adViewContainer.addView(amazonAdView);
		}
	}

	public void refreshAd() {
		amazonAdView.loadAd(new com.amazon.device.ads.AdTargetingOptions());
	}
}