package com.kskkbys.loop.ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;
import com.kskkbys.loop.BuildConfig;
import com.kskkbys.loop.R;
import com.kskkbys.loop.Video;
import com.kskkbys.loop.VideoPlayerService;
import com.kskkbys.loop.YouTubeSearchTask;
import com.kskkbys.loop.R.id;
import com.kskkbys.loop.R.layout;
import com.kskkbys.loop.R.menu;
import com.kskkbys.loop.R.string;
import com.kskkbys.loop.logger.FlurryLogger;
import com.kskkbys.loop.logger.KLog;
import com.kskkbys.loop.net.ConnectionState;
import com.kskkbys.loop.playlist.BlackList;
import com.kskkbys.loop.playlist.Playlist;
import com.kskkbys.loop.service.PlayerCommand;
import com.kskkbys.rate.RateThisApp;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.Intent;
import android.support.v4.view.MenuItemCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Search screen.
 *
 */
public class MainActivity extends BaseActivity {

	private static final String TAG = MainActivity.class.getSimpleName();

	private static final String FILENAME_SEARCH_HISTORY = "search_history.txt";

	public static final String FROM_NOTIFICATION = "from_notification";

	private List<String> mRecentArtists;
	private ArtistAdapter mAdapter;
	private ListView mListView;

	private MenuItem mSearchItem;

	// Contextual Action Bar
	private ActionMode mActionMode;
	private int mLongSelectedPosition;
	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = getSupportMenuInflater();
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

		// Set up listview
		mRecentArtists = new ArrayList<String>();
		mAdapter = new ArtistAdapter(this, mRecentArtists);
		mListView = (ListView)findViewById(R.id.main_search_history);
		mListView.setAdapter(mAdapter);

		// Read recent artist saved in the device
		readHistory();

		// Update recent artists view
		updateHistoryUI();

		// Initialize action bar
		getSupportActionBar().setTitle(R.string.loop_main_title);
		mActionMode = null;
		mLongSelectedPosition = -1;

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
		// If this activity is launched from notification, go to PlayerActivity
		boolean isFromNotification = intent.getBooleanExtra(FROM_NOTIFICATION, false);
		if (isFromNotification) {
			KLog.v(TAG, "Launched from notification. Go next activity.");
			goToNextActivity();
			return;
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
	protected void onStart() {
		super.onStart();
		RateThisApp.onStart(this);
		RateThisApp.showRateDialogIfNeeded(this);
	}

	private void searchQuery(String artist) {
		if (artist == null || artist.length() == 0) {
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
		if (mRecentArtists.contains(artist)) {
			// already exist => Go to last position
			mRecentArtists.remove(artist);
			mRecentArtists.add(artist);
		} else {
			// new add
			mRecentArtists.add(artist);
		}

		saveSearchHistory();

		YouTubeSearchTask searchTask = new YouTubeSearchTask(MainActivity.this);
		searchTask.execute(artist);
	}

	/**
	 * Read search history from saved file
	 */
	private void readHistory() {
		mRecentArtists.clear();
		FileInputStream fis;
		try {
			fis = openFileInput(FILENAME_SEARCH_HISTORY);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			String line;
			while ((line = br.readLine()) != null) {
				mRecentArtists.add(line);
			}
			br.close();
		} catch (FileNotFoundException e) {
			// Fisrt launching
			//e.printStackTrace();
			KLog.w(TAG,"FileNotFound of search history. May be first launch.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void clearHistory() {
		mRecentArtists = new ArrayList<String>();
		saveSearchHistory();
	}

	private void clearHistory(int position) {
		mRecentArtists.remove(position);
		saveSearchHistory();
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
	private void updateHistoryUI() {
		if (mRecentArtists != null && mRecentArtists.size() > 0) {
			// Has history
			mListView.setVisibility(View.VISIBLE);
			//TODO findViewById(R.id.noHistoryLabel).setVisibility(View.INVISIBLE);

			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
			for (int i = mRecentArtists.size() - 1; i >= 0; i--) {
				adapter.add(mRecentArtists.get(i));
			}

			mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					// Check connection
					if (!ConnectionState.isConnected(MainActivity.this)) {
						KLog.w(TAG, "bad connection");
						// SimpleErrorDialog.show(MainActivity.this, R.string.loop_main_error_bad_connection);
						showAlert(R.string.loop_main_error_bad_connection, null);
						return;
					} else {
						ListView listView = (ListView) parent;
						String artistName = (String) listView.getItemAtPosition(position);
						String currentArtist = Playlist.getInstance().getQuery();
						if (!TextUtils.isEmpty(currentArtist) && currentArtist.equals(artistName)) {
							KLog.v(TAG, "Already playing. Go player without seraching.");
							goToNextActivity();
						} else {
							searchQuery(artistName);
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
					mActionMode = startActionMode(mActionModeCallback);
					view.setSelected(true);
					return true;
				}
			});
		} else {
			// no histroy
			// TODO findViewById(R.id.noHistoryLabel).setVisibility(View.VISIBLE);
			findViewById(R.id.main_search_history).setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.activity_main, menu);

		// Set action view
		mSearchItem = menu.findItem(R.id.menu_search);
		final SearchView sv = (SearchView) mSearchItem.getActionView();
		sv.setQueryHint(getText(R.string.loop_main_search_hint));
		sv.setOnQueryTextListener(new OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
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
				return true;
			}
			@Override
			public boolean onQueryTextChange(String newText) {
				// TODO Auto-generated method stub
				return false;
			}
		});

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

	private void saveSearchHistory() {
		FileOutputStream fos;
		try {
			fos = openFileOutput(FILENAME_SEARCH_HISTORY, Context.MODE_PRIVATE);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
			for (String artist : mRecentArtists) {
				bw.write(artist);
				bw.newLine();
			}
			bw.flush();
			bw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
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
	private static class ArtistAdapter extends ArrayAdapter<String> {

		private Activity mActivity;

		/**
		 * Constructor.
		 * @param activity
		 * @param objects
		 */
		public ArtistAdapter(Activity activity, List<String> objects) {
			super(activity, R.layout.search_history_list_item, R.id.search_history_artist, objects);
		}
		/*
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				this.
				LayoutInflater inflater = mActivity.getLayoutInflater();
				view = inflater.inflate(R.layout.search_history_list_item, parent, false);
			}
			return view;
		}
		 */
	}
}
