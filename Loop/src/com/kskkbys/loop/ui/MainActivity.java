package com.kskkbys.loop.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.kskkbys.loop.R;
import com.kskkbys.loop.logger.FlurryLogger;
import com.kskkbys.loop.logger.KLog;
import com.kskkbys.loop.model.Artist;
import com.kskkbys.loop.model.Playlist;
import com.kskkbys.loop.model.SearchHistory;
import com.kskkbys.loop.model.Video;
import com.kskkbys.loop.net.YouTubeSearchTask;
import com.kskkbys.loop.search.ArtistSuggestionsProvider;
import com.kskkbys.loop.service.VideoPlayerService;
import com.kskkbys.loop.ui.fragments.MainFavoriteFragment;
import com.kskkbys.loop.ui.fragments.MainHistoryFragment;
import com.kskkbys.loop.util.ConnectionState;
import com.kskkbys.rate.RateThisApp;

import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.TabListener;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Search screen.
 *
 */
public class MainActivity extends BaseActivity implements TabListener {

	private static final String TAG = MainActivity.class.getSimpleName();

	public static final String FROM_NOTIFICATION = "from_notification";

	// Pager
	SectionsPagerAdapter mSectionsPagerAdapter;
	ViewPager mViewPager;

	// Fragments
	MainHistoryFragment mHistoryFragment = new MainHistoryFragment();
	MainFavoriteFragment mFavoriteFragment = new MainFavoriteFragment();

	// Menu
	private MenuItem mSearchItem;

	// Contextual Action Bar
	private ActionMode mActionMode;
	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			KLog.v(TAG, "onDestroyActionMode");
			mActionMode = null;
			// Disable selection
			int pos = mViewPager.getCurrentItem();
			if (pos == 0) {
				MainHistoryFragment fragment = (MainHistoryFragment)mSectionsPagerAdapter.getItem(pos);
				fragment.deselect();
			} else {
				// MainFavoriteFragment fragment = (MainFavoriteFragment)mSectionsPagerAdapter.getItem(pos);
				// fragment.deselect();
			}
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			KLog.v(TAG, "onCreateActionMode");
			MenuInflater inflater = getMenuInflater();
			if (mViewPager.getCurrentItem() == 0) {
				inflater.inflate(R.menu.activity_main_history_cab, menu);
			} else {
				inflater.inflate(R.menu.activity_main_favorite_cab, menu);
			}
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			KLog.v(TAG, "onActionItemClicked");
			switch (item.getItemId()) {
			case R.id.menu_delete_history:
				MainHistoryFragment fragment0 = (MainHistoryFragment)mSectionsPagerAdapter.getItem(0);
				fragment0.clearLongSelectedHistory();
				mode.finish(); // Action picked, so close the CAB
				return true;
			case R.id.menu_delete_favorite:
				MainFavoriteFragment fragment1 = (MainFavoriteFragment)mSectionsPagerAdapter.getItem(1);
				fragment1.clearLongSelectedVideo();
				mode.finish();
				return true;
			default:
				return false;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		KLog.v(TAG, "onCreate");
		FlurryLogger.logEvent(FlurryLogger.SEE_SEARCH);

		// Start service here.
		// This api call is needed in order to keep the service alive 
		// even when all activities are close.
		startService(new Intent(MainActivity.this, VideoPlayerService.class));

		// Initialize action bar
		final ActionBar actionBar = getSupportActionBar();
		actionBar.setTitle(R.string.loop_main_title);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Pager
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager
		.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				actionBar.setSelectedNavigationItem(position);
			}
		});

		// Tabs
		actionBar.addTab(actionBar.newTab().setText(R.string.loop_main_tab_history).setTabListener(this));
		actionBar.addTab(actionBar.newTab().setText(R.string.loop_main_tab_favorite).setTabListener(this));

		// CAB
		mActionMode = null;

		// If this activity is launched from notification, go to PlayerActivity
		boolean isFromNotification = getIntent().getBooleanExtra(FROM_NOTIFICATION, false);
		if (isFromNotification) {
			KLog.v(TAG, "Launched from notification. Go next activity.");
			goToNextActivity(false);
			return;
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		KLog.v(TAG, "onNewIntent");
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			// When receiving SEARCH intent
			KLog.v(TAG, "Receiving SEARCH intent");
			String query = intent.getStringExtra(SearchManager.QUERY);
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
					ArtistSuggestionsProvider.AUTHORITY, ArtistSuggestionsProvider.MODE);
			suggestions.saveRecentQuery(query, null);

			// Check connection
			if (!ConnectionState.isConnected(MainActivity.this)) {
				KLog.w(TAG, "bad connection");
				showAlert(R.string.loop_main_error_bad_connection, null);
			} else {
				KLog.v(TAG, "connection ok");
				// Search query
				Map<String, String> param = new HashMap<String, String>();
				param.put("query", query);
				FlurryLogger.logEvent(FlurryLogger.SEARCH_ARTIST, param);
				searchQuery(query);
			}
		} else {
			// If this activity is launched from notification, go to PlayerActivity
			boolean isFromNotification = intent.getBooleanExtra(FROM_NOTIFICATION, false);
			if (isFromNotification) {
				KLog.v(TAG, "Launched from notification. Go next activity.");
				goToNextActivity(false);
				return;
			}
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		KLog.v(TAG, "onConfigurationChanged");
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onStart() {
		super.onStart();
		RateThisApp.onStart(this);
		RateThisApp.showRateDialogIfNeeded(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		updatePlayingNotification();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);

		// Set action view
		mSearchItem = menu.findItem(R.id.menu_search);
		final SearchView sv = (SearchView) MenuItemCompat.getActionView(mSearchItem);
		// Get the SearchView and set the searchable configuration
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		// Assumes current activity is the searchable activity
		sv.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			goToSettings();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Search the artist with YouTube API.
	 * If already playing the artist, it goes to video player directly.
	 * @param artist
	 */
	public void searchOrGoToPlayer(Artist artist) {
		if (!ConnectionState.isConnected(this)) {
			KLog.w(TAG, "bad connection");
			// SimpleErrorDialog.show(MainActivity.this, R.string.loop_main_error_bad_connection);
			showAlert(R.string.loop_main_error_bad_connection, null);
			return;
		} else {
			String currentArtist = Playlist.getInstance().getQuery();
			if (!TextUtils.isEmpty(currentArtist) && currentArtist.equals(artist.name)) {
				KLog.v(TAG, "Already playing. Go player without seraching.");
				goToNextActivity(false);
			} else {
				searchQuery(artist.name);
			}
		}
	}

	private void searchQuery(String artist) {
		KLog.v(TAG, "searchQuery");
		// validation
		if (TextUtils.isEmpty(artist)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.loop_main_invalid_query)
			.setPositiveButton(R.string.loop_ok, null)
			.setCancelable(false)
			.create().show();
		} else {
			// Add history
			int position = mViewPager.getCurrentItem();
			if (position == 0) {
				SearchHistory.getInstance(this).addArtist(artist);
			}
			// Start to search
			YouTubeSearchTask searchTask = new YouTubeSearchTask(MainActivity.this);
			searchTask.execute(artist);
		}
	}
	
	private void goToSettings() {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}

	// TODO Playing notification will be another fragment.
	private void updatePlayingNotification() {
		if (Playlist.getInstance().getCurrentVideo() != null) {
			RelativeLayout base = (RelativeLayout)findViewById(R.id.main_base);
			View notification = base.findViewById(R.id.notification_base);
			if (notification == null) {
				// Add
				notification = getLayoutInflater().inflate(R.layout.main_playing_notification, null);
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				params.addRule(RelativeLayout.CENTER_HORIZONTAL);
				notification.setLayoutParams(params);
				notification.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						goToNextActivity(false);
					}
				});
				base.addView(notification);
			}
			// Update
			TextView title = (TextView)notification.findViewById(R.id.notification_title);
			title.setText(Playlist.getInstance().getCurrentVideo().getTitle());
		}
	}

	/**
	 * Start video player
	 * @param query		Search query. When playing favorites, it will be null.
	 * @param result	List of videos.
	 * @param position	Index to start to play.
	 */
	public void startVideoPlayer(String query, List<Video> result, int position) {
		Playlist.getInstance().setVideoList(query, result, position);
		// Go next activity
		goToNextActivity(true);
	}

	/**
	 * This method is called when button in empty view is clicked.
	 */
	public void expandSearchView() {
		mSearchItem.expandActionView();
	}

	/**
	 * Go next activity
	 * @param isReload	A flag whether player will reload videos.
	 */
	public void goToNextActivity(boolean isReload) {
		Intent intent = new Intent(MainActivity.this, VideoPlayerActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		intent.putExtra(VideoPlayerActivity.IS_RELOAD, isReload);
		startActivity(intent);
	}

	public boolean startActionModeByLongClick() {
		KLog.v(TAG, "startActionModeByLongClick");
		// Show contextual action bar
		if (mActionMode != null) {
			return false;
		}
		mActionMode = startSupportActionMode(mActionModeCallback);
		return true;
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		/**
		 * Constructor.
		 * @param fm
		 */
		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case 0:
				return mHistoryFragment;
			case 1:
				return mFavoriteFragment;
			}
			return null;
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.loop_main_tab_history).toUpperCase(l);
			case 1:
				return getString(R.string.loop_main_tab_favorite).toUpperCase(l);
			}
			return null;
		}
	}

	@Override
	public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
	}
}
