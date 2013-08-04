package com.kskkbys.loop.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.kskkbys.loop.BuildConfig;
import com.kskkbys.loop.R;
import com.kskkbys.loop.fragments.MainFavoriteFragment;
import com.kskkbys.loop.fragments.MainHistoryFragment;
import com.kskkbys.loop.logger.FlurryLogger;
import com.kskkbys.loop.logger.KLog;
import com.kskkbys.loop.model.Artist;
import com.kskkbys.loop.model.BlackList;
import com.kskkbys.loop.model.Playlist;
import com.kskkbys.loop.model.Video;
import com.kskkbys.loop.net.YouTubeSearchTask;
import com.kskkbys.loop.search.ArtistSuggestionsProvider;
import com.kskkbys.loop.service.PlayerCommand;
import com.kskkbys.loop.service.VideoPlayerService;
import com.kskkbys.loop.util.ConnectionState;
import com.kskkbys.rate.RateThisApp;

import android.net.Uri;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.Intent;
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
			mActionMode = null;
			// Disable selection
			int pos = mViewPager.getCurrentItem();
			if (pos == 0) {
				MainHistoryFragment fragment = (MainHistoryFragment)mSectionsPagerAdapter.getItem(pos);
				fragment.deselect();
			}
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.activity_main_cab, menu);
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case R.id.menu_delete:
				int pos = mViewPager.getCurrentItem();
				if (pos == 0) {
					MainHistoryFragment fragment = (MainHistoryFragment)mSectionsPagerAdapter.getItem(pos);
					fragment.clearLongSelectedHistory();
				}
				mode.finish(); // Action picked, so close the CAB
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
		actionBar.addTab(actionBar.newTab().setText("History").setTabListener(this));
		actionBar.addTab(actionBar.newTab().setText("Favorite").setTabListener(this));

		// CAB
		mActionMode = null;

		// If this activity is launched from notification, go to PlayerActivity
		boolean isFromNotification = getIntent().getBooleanExtra(FROM_NOTIFICATION, false);
		if (isFromNotification) {
			KLog.v(TAG, "Launched from notification. Go next activity.");
			goToNextActivity();
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
				goToNextActivity();
				return;
			}
		}
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
		AlertDialog.Builder builder;
		switch (item.getItemId()) {
		case R.id.menu_clear_history:
			builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.loop_main_confirm_clear_history)
			.setPositiveButton(R.string.loop_ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					int position = mViewPager.getCurrentItem();
					if (position == 0) {
						MainHistoryFragment fragment = (MainHistoryFragment)mSectionsPagerAdapter.getItem(position);
						fragment.clearAllHistory();
						fragment.updateHistoryUI();
					}
				}
			})
			.setNegativeButton(R.string.loop_cancel, null);
			builder.create().show();
			return true;
		case R.id.menu_clear_blacklist:
			builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.loop_main_confirm_clear_blacklist)
			.setPositiveButton(R.string.loop_ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					BlackList.getInstance().clear();
				}
			})
			.setNegativeButton(R.string.loop_cancel, null);
			builder.create().show();
			return true;
		case R.id.menu_version:
			showVersion();
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
				goToNextActivity();
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
				MainHistoryFragment fragment = (MainHistoryFragment)mSectionsPagerAdapter.getItem(position);
				fragment.addArtist(artist);
			}
			// Start to search
			YouTubeSearchTask searchTask = new YouTubeSearchTask(MainActivity.this);
			searchTask.execute(artist);
		}
	}



	private void openGooglePlay() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("market://details?id=com.kskkbys.loop"));
		startActivity(intent);
	}

	private void showVersion() {
		PackageManager pm = getPackageManager();
		PackageInfo info;
		try {
			info = pm.getPackageInfo(this.getPackageName(), PackageManager.GET_ACTIVITIES);
			String message = "Version " + info.versionName;
			if (BuildConfig.DEBUG) {
				message += " (Debug)";
			}

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.loop_app_name);
			builder.setMessage(message);
			builder.setPositiveButton(R.string.loop_ok, null);
			builder.setNegativeButton(R.string.loop_main_open_google_play, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					openGooglePlay();
				}
			});
			builder.create().show();
		} catch (NameNotFoundException e) {
			KLog.e(TAG, "package not found", e);
		}
	}

	/**
	 * Start video player
	 * @param result
	 */
	public void startVideoPlayer(String query, List<Video> result) {
		Playlist.getInstance().setVideoList(query, result);
		PlayerCommand.play(this, true);
		// Go next activity
		goToNextActivity();
	}

	/**
	 * This method is called when button in empty view is clicked.
	 */
	public void expandSearchView() {
		mSearchItem.expandActionView();
	}

	/**
	 * Go next activity
	 */
	public void goToNextActivity() {
		Intent intent = new Intent(MainActivity.this, VideoPlayerActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(intent);
	}

	public boolean startActionModeByLongClick() {
		// Show contextual action bar
		if (mActionMode != null) {
			return false;
		}
		mActionMode = startSupportActionMode(mActionModeCallback);
		return true;
	}

	public void updateHistory(String query, List<Video> videos) {
		int position = mViewPager.getCurrentItem();
		if (position == 0) {
			MainHistoryFragment fragment = (MainHistoryFragment)mSectionsPagerAdapter.getItem(position);
			// Save image URL in search history db
			fragment.updateHistory(query, videos);
			// Update history list view
			fragment.updateHistoryUI();
		}
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
			Fragment fragment = null;
			switch (position) {
			case 0:
				fragment = new MainHistoryFragment();
				break;
			case 1:
				fragment = new MainFavoriteFragment();
				break;
			}
			return fragment;
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
