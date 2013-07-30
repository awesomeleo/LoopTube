package com.kskkbys.loop.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kskkbys.loop.BuildConfig;
import com.kskkbys.loop.LoopApplication;
import com.kskkbys.loop.R;
import com.kskkbys.loop.logger.FlurryLogger;
import com.kskkbys.loop.logger.KLog;
import com.kskkbys.loop.model.BlackList;
import com.kskkbys.loop.model.Playlist;
import com.kskkbys.loop.model.Video;
import com.kskkbys.loop.net.YouTubeSearchTask;
import com.kskkbys.loop.search.ArtistSuggestionsProvider;
import com.kskkbys.loop.service.PlayerCommand;
import com.kskkbys.loop.service.VideoPlayerService;
import com.kskkbys.loop.storage.ArtistStorage;
import com.kskkbys.loop.storage.ArtistStorage.Entry;
import com.kskkbys.loop.util.ConnectionState;
import com.kskkbys.rate.RateThisApp;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.view.MenuCompat;
import android.support.v4.view.MenuItemCompat;
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

	private static final int IMAGE_COUNT_PER_ROW = 5;
	
	// ListView
	private List<ArtistStorage.Entry> mRecentArtists;
	private ArtistAdapter mAdapter;
	private ListView mListView;

	// Menu
	private MenuItem mSearchItem;

	private ArtistStorage mStorage;

	// Contextual Action Bar
	private ActionMode mActionMode;
	private int mLongSelectedPosition;
	private View mLongSelectedItem;
	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
			// Disable selection
			if (mLongSelectedItem != null) {
				mLongSelectedItem.setSelected(false);
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
				clearHistory(mLongSelectedPosition);
				mAdapter.notifyDataSetChanged();
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

		// Set up listview and empty view
		mListView = (ListView)findViewById(R.id.main_search_history);
		View emptyView = findViewById(R.id.main_empty);
		emptyView.findViewById(R.id.main_search_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mSearchItem.expandActionView();
			}
		});
		mListView.setEmptyView(emptyView);
		mRecentArtists = new ArrayList<ArtistStorage.Entry>();
		mAdapter = new ArtistAdapter(this, mRecentArtists);
		mListView.setAdapter(mAdapter);
		mListView.setRecyclerListener(new RecyclerListener() {
			@Override
			public void onMovedToScrapHeap(View view) {
				//
				KLog.v(TAG, "onMoveToScrapHeap");
			}
		});
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				KLog.v(TAG, "onItemClick");
				// Check connection
				if (!ConnectionState.isConnected(MainActivity.this)) {
					KLog.w(TAG, "bad connection");
					// SimpleErrorDialog.show(MainActivity.this, R.string.loop_main_error_bad_connection);
					showAlert(R.string.loop_main_error_bad_connection, null);
					return;
				} else {
					ListView listView = (ListView) parent;
					ArtistStorage.Entry artist = (ArtistStorage.Entry) listView.getItemAtPosition(position);
					String currentArtist = Playlist.getInstance().getQuery();
					if (!TextUtils.isEmpty(currentArtist) && currentArtist.equals(artist.name)) {
						KLog.v(TAG, "Already playing. Go player without seraching.");
						goToNextActivity();
					} else {
						searchQuery(artist.name);
					}
				}
			}
		});
		mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View view,
					int position, long id) {
				// Show contextual action bar
				if (mActionMode != null) {
					return false;
				}
				mLongSelectedPosition = position;
				mLongSelectedItem = view;
				mActionMode = startSupportActionMode(mActionModeCallback);
				view.setSelected(true);
				return true;
			}
		});
		
		LoopApplication app = (LoopApplication)getApplication();
		mStorage = app.getArtistStorage();

		// Read recent artist saved in the device
		readHistory();

		// Update recent artists view
		updateHistoryUI();

		// Initialize action bar
		getSupportActionBar().setTitle(R.string.loop_main_title);
		mActionMode = null;
		mLongSelectedPosition = -1;
		mLongSelectedItem = null;

		// If a video is playing, show notification at bottom
		updatePlayingNotification();

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
	protected void onResume() {
		super.onResume();
		// update history
		updateHistoryUI();
		// update notification
		updatePlayingNotification();
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
					clearHistory();
					updateHistoryUI();
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
		ArtistStorage.Entry entry = findHistory(artist);
		if (entry != null) {
			mRecentArtists.remove(entry);
			entry.date = new Date();
			mRecentArtists.add(0, entry);
		} else {
			entry = new ArtistStorage.Entry();
			entry.name = artist;
			entry.imageUrls = new ArrayList<String>();	// Before search video list, image URL is null.
			entry.date = new Date();
			mRecentArtists.add(entry);
		}
		// mStorage.insertOrUpdate(entry);

		YouTubeSearchTask searchTask = new YouTubeSearchTask(MainActivity.this);
		searchTask.execute(artist);
	}

	private ArtistStorage.Entry findHistory(String artist) {
		for (ArtistStorage.Entry e: mRecentArtists) {
			if (e.name.equals(artist)) {
				return e;
			}
		}
		return null;
	}

	/**
	 * Read search history which is already restored before.
	 */
	private void readHistory() {
		List<ArtistStorage.Entry> entries = mStorage.getRestoredArtists();
		mRecentArtists.clear();
		mRecentArtists.addAll(entries);
		mAdapter.notifyDataSetChanged();
	}

	private void clearHistory() {
		// App's search history
		new Thread(new Runnable() {
			@Override
			public void run() {
				mStorage.clear();
			}
		}).start();
		mRecentArtists.clear();
		mAdapter.notifyDataSetChanged();
		// OS's search history
		SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
				ArtistSuggestionsProvider.AUTHORITY, ArtistSuggestionsProvider.MODE);
		suggestions.clearHistory();
	}

	private void clearHistory(int position) {
		ArtistStorage.Entry e = mRecentArtists.remove(position);
		new AsyncTask<ArtistStorage.Entry, Integer, Boolean>() {
			@Override
			protected Boolean doInBackground(Entry... params) {
				mStorage.delete(params[0]);
				return true;
			}
			@Override
			protected void onPostExecute(Boolean result) {
				mAdapter.notifyDataSetChanged();
			}
		}.execute(e);
	}

	public void updateHistory(String query, List<Video> videos) {
		// Update array list
		ArtistStorage.Entry updatedEntry = null;
		for (ArtistStorage.Entry entry: mRecentArtists) {
			if (entry.name.equals(query)) {
				entry.imageUrls = new ArrayList<String>();
				int count = 0;
				for (Video v: videos) {
					if (!TextUtils.isEmpty(v.getThumbnailUrl())) {
						entry.imageUrls.add(v.getThumbnailUrl());
						count++;
						if (count >= IMAGE_COUNT_PER_ROW) {
							break;
						}
					}
				}
				updatedEntry = entry;
				break;
			}
		}
		// Store to SQLite (async)
		saveArtist(updatedEntry);
	}
	
	private void saveArtist(final ArtistStorage.Entry updatedEntry) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (updatedEntry != null) {
					mStorage.insertOrUpdate(updatedEntry, true);
				}
			}
		}).start();
	}

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
						goToNextActivity();
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
	 * Update history view
	 */
	public void updateHistoryUI() {
		KLog.v(TAG, "updateHistoryUI");
		if (mRecentArtists != null && mRecentArtists.size() > 0) {
			mListView.setVisibility(View.VISIBLE);
		} else {
			mListView.setVisibility(View.INVISIBLE);
		}
		mAdapter.notifyDataSetChanged();
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
	 * Go next activity
	 */
	private void goToNextActivity() {
		Intent intent = new Intent(MainActivity.this, VideoPlayerActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(intent);
	}

	/**
	 * Adapter class of artist
	 * @author Keisuke Kobayashi
	 *
	 */
	private class ArtistAdapter extends ArrayAdapter<ArtistStorage.Entry> {
		
		private Activity mActivity;

		/**
		 * Constructor.
		 * @param activity
		 * @param objects
		 */
		public ArtistAdapter(Activity activity, List<ArtistStorage.Entry> objects) {
			super(activity, R.layout.search_history_list_item, R.id.search_history_artist, objects);
			mActivity = activity;
		}

		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent) {
			View view = convertView;
			String prevArtist = null;
			if (view == null) {
				LayoutInflater inflater = mActivity.getLayoutInflater();
				view = inflater.inflate(R.layout.search_history_list_item, parent, false);
			} else {
				TextView titleView = (TextView)view.findViewById(R.id.search_history_artist);
				prevArtist = titleView.getText().toString();
			}

			final ArtistStorage.Entry artist = getItem(position);

			// Set title
			TextView titleView = (TextView)view.findViewById(R.id.search_history_artist);
			titleView.setText(artist.name);

			// Set click / long click events
			setUpImageView((ListView)parent, view.findViewById(R.id.search_history_overlay), position);
			
			// Set background images
			LinearLayout container = (LinearLayout)view.findViewById(R.id.search_history_image_container);
			if (prevArtist == null || !prevArtist.equals(artist.name) || container.getChildCount() == 0) {
				// Reload images
				reloadImages(container, artist);
			}
			return view;
		}
		
		private void reloadImages(LinearLayout container, ArtistStorage.Entry artist) {
			container.removeAllViews();
			if (artist.imageUrls != null) {
				KLog.v(TAG, "Images are saved.");
				int size = Math.min(IMAGE_COUNT_PER_ROW, artist.imageUrls.size());
				for (int i=0; i<size; i++) {
					ImageView iv = new ImageView(getContext());
					LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
					iv.setLayoutParams(params);
					iv.setAdjustViewBounds(true);
					
					if (i % 2 == 0) {
						iv.setBackgroundColor(Color.WHITE);
					} else {
						iv.setBackgroundColor(Color.RED);
					}
					container.addView(iv);
					// Load image from URL
					ImageLoader imageLoader = ImageLoader.getInstance();
					imageLoader.displayImage(artist.imageUrls.get(i), iv);
				}
			}
		}

		/**
		 * Set click/long click events to invoke events of ListView.
		 * @param parent
		 * @param imageView
		 * @param position
		 */
		private void setUpImageView(final ListView parent, final View imageView, final int position) {
			imageView.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					KLog.v(TAG, "image onclikc");
					parent.setSelection(position);
					parent.performItemClick(v, position, v.getId());
				}
			});
			imageView.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					KLog.v(TAG, "image onlongclick");
					parent.setSelection(position);
					return false;
				}
			});
		}
	}
}
