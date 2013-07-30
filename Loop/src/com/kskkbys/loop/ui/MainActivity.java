package com.kskkbys.loop.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kskkbys.loop.BuildConfig;
import com.kskkbys.loop.LoopApplication;
import com.kskkbys.loop.R;
import com.kskkbys.loop.fragments.MainFavoriteFragment;
import com.kskkbys.loop.fragments.MainHistoryFragment;
import com.kskkbys.loop.logger.FlurryLogger;
import com.kskkbys.loop.logger.KLog;
import com.kskkbys.loop.model.BlackList;
import com.kskkbys.loop.model.Playlist;
import com.kskkbys.loop.model.Video;
import com.kskkbys.loop.net.YouTubeSearchTask;
import com.kskkbys.loop.search.ArtistSuggestionsProvider;
import com.kskkbys.loop.service.PlayerCommand;
import com.kskkbys.loop.service.VideoPlayerService;
import com.kskkbys.loop.storage.SQLiteStorage;
import com.kskkbys.loop.storage.SQLiteStorage.Artist;
import com.kskkbys.loop.util.ConnectionState;
import com.kskkbys.rate.RateThisApp;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.internal.widget.ActivityChooserModel.HistoricalRecord;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView.RecyclerListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Search screen.
 *
 */
public class MainActivity extends BaseActivity {

	private static final String TAG = MainActivity.class.getSimpleName();

	public static final String FROM_NOTIFICATION = "from_notification";

	// Fragments
	private MainHistoryFragment mHistoryFragment;
	private MainFavoriteFragment mFavoriteFragment;

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
			mHistoryFragment.deselect();
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
				mHistoryFragment.clearLongSelectedHistory();
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

		KLog.v(TAG, "onCreate");
		FlurryLogger.logEvent(FlurryLogger.SEE_SEARCH);

		// Start service here.
		// This api call is needed in order to keep the service alive 
		// even when all activities are close.
		startService(new Intent(MainActivity.this, VideoPlayerService.class));

		// Initialize fragments
		mHistoryFragment = (MainHistoryFragment)MainHistoryFragment.instantiate(this, MainHistoryFragment.class.getName());
		mFavoriteFragment = (MainFavoriteFragment)MainFavoriteFragment.instantiate(this, MainFavoriteFragment.class.getName());

		// Initialize action bar
		ActionBar actionBar = getSupportActionBar();
		actionBar.setTitle(R.string.loop_main_title);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		TabListener tabListener1 = new TabListener(mHistoryFragment);
		TabListener tabListener2 = new TabListener(mFavoriteFragment);
		actionBar.addTab(actionBar.newTab().setText("History").setTabListener(tabListener1));
		actionBar.addTab(actionBar.newTab().setText("Favorite").setTabListener(tabListener2));

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
					mHistoryFragment.clearAllHistory();
					mHistoryFragment.updateHistoryUI();
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
	public void searchOrGoToPlayer(SQLiteStorage.Artist artist) {
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
			.setPositiveButton(R.string.loop_ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {

				}
			})
			.setCancelable(false)
			.create().show();
			return;
		}
		// Add history
		mHistoryFragment.addArtist(artist);
		// Start to search
		YouTubeSearchTask searchTask = new YouTubeSearchTask(MainActivity.this);
		searchTask.execute(artist);
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
		// Save image URL in search history db
		mHistoryFragment.updateHistory(query, videos);
		// Update history list view
		mHistoryFragment.updateHistoryUI();
	}

	public static class TabListener implements ActionBar.TabListener {
		private final Fragment mFragment;

		/** Constructor used each time a new tab is created.
		 * @param activity  The host Activity, used to instantiate the fragment
		 * @param tag  The identifier tag for the fragment
		 * @param clz  The fragment's Class, used to instantiate the fragment
		 */
		public TabListener(Fragment fragment) {
			mFragment = fragment;
		}

		/* The following are each of the ActionBar.TabListener callbacks */

		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			ft.replace(android.R.id.content, mFragment);
			// Check if the fragment is already initialized
			/*
			if (mFragment == null) {
				// If not, instantiate and add it to the activity
				mFragment = Fragment.instantiate(mActivity, mClass.getName());
				ft.add(android.R.id.content, mFragment, mTag);
			} else {
				// If it exists, simply attach it in order to show it
				ft.attach(mFragment);
			}*/
		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			/*
			if (mFragment != null) {
				// Detach the fragment, because another one is being attached
				ft.detach(mFragment);
			}*/
		}

		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			// User selected the already selected tab. Usually do nothing.
		}
	}
}
