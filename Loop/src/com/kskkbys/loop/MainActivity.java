package com.kskkbys.loop;

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

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.kskkbys.loop.logger.FlurryLogger;
import com.kskkbys.loop.logger.KLog;
import com.kskkbys.loop.net.ConnectionState;
import com.kskkbys.loop.playlist.BlackList;
import com.kskkbys.loop.playlist.Playlist;

import android.os.Bundle;
import android.os.IBinder;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

/**
 * Search screen.
 *
 */
public class MainActivity extends BaseActivity {

	private static final String TAG = MainActivity.class.getSimpleName();

	private static final String FILENAME_SEARCH_HISTORY = "search_history.txt";

	private List<String> mRecentArtists = new ArrayList<String>();

	// Services
	private VideoPlayerService mService;
	private boolean mIsBound = false;

	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = ((VideoPlayerService.VideoPlayerServiceBinder)service).getService();
			//Toast.makeText(MainActivity.this, "Service connected", Toast.LENGTH_SHORT).show();
			KLog.v(TAG, "service connected");
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
			//Toast.makeText(MainActivity.this, "Service disconnected", Toast.LENGTH_SHORT).show();
			KLog.v(TAG, "service disconnected");
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

		// Bind player service
		if (!mIsBound) {
			doBindService();
		}

		// Read recent artist saved in the device
		readHistory();

		// Update recent artists view
		updateHistoryUI();

		Button button = (Button)findViewById(R.id.searchButton);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Check connection
				if (!ConnectionState.isConnected(MainActivity.this)) {
					KLog.w(TAG, "bad connection");
					// SimpleErrorDialog.show(MainActivity.this, R.string.loop_main_error_bad_connection);
					showAlert(R.string.loop_main_error_bad_connection, null);
					return;
				} else {
					KLog.v(TAG, "connection ok");
					// Search query
					EditText searchEditText = (EditText)findViewById(R.id.searchText);
					String query = searchEditText.getEditableText().toString();

					Map<String, String> param = new HashMap<String, String>();
					param.put("query", query);
					FlurryLogger.logEvent(FlurryLogger.SEARCH_ARTIST, param);

					searchQuery(query);
				}
			}
		});

		// If this activity is launched from notification, go to PlayerActivity
		boolean isFromNotification = getIntent().getBooleanExtra("from_notification", false);
		if (isFromNotification) {
			KLog.v(TAG, "Launched from notification. Go next activity.");
			goNextActivity();
			return;
		}

		//
		getSupportActionBar().setTitle(R.string.loop_main_title);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		KLog.v(TAG, "onNewIntent");
		// If this activity is launched from notification, go to PlayerActivity
		boolean isFromNotification = intent.getBooleanExtra("from_notification", false);
		if (isFromNotification) {
			KLog.v(TAG, "Launched from notification. Go next activity.");
			goNextActivity();
			return;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		// update history
		updateHistoryUI();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		doUnbindService();
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
		mRecentArtists = new ArrayList<String>();
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

	/**
	 * Update history view
	 */
	private void updateHistoryUI() {
		if (mRecentArtists != null && mRecentArtists.size() > 0) {
			// Has history
			findViewById(R.id.listView1).setVisibility(View.VISIBLE);
			findViewById(R.id.noHistoryLabel).setVisibility(View.INVISIBLE);

			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
			for (int i = mRecentArtists.size() - 1; i >= 0; i--) {
				adapter.add(mRecentArtists.get(i));
			}

			ListView listView = (ListView)findViewById(R.id.listView1);
			listView.setAdapter(adapter);

			listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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
							goNextActivity();
						} else {
							searchQuery(artistName);
						}
					}
				}
			});
		} else {
			// no histroy
			findViewById(R.id.noHistoryLabel).setVisibility(View.VISIBLE);
			findViewById(R.id.listView1).setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		AlertDialog.Builder builder;
		switch (item.getItemId()) {
		case R.id.menu_show_player:
			if (Playlist.getInstance().getCurrentVideo() != null) {
				startActivity(new Intent(MainActivity.this, VideoPlayerActivity.class));
			} else {
				//SimpleErrorDialog.show(this, R.string.loop_main_dialog_not_playing);
				showAlert(R.string.loop_main_dialog_not_playing, null);
			}
			return true;
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
		this.mService.startVideo();
		// Go next activity
		goNextActivity();
	}

	/**
	 * Go next activity
	 */
	private void goNextActivity() {
		Intent intent = new Intent(MainActivity.this, VideoPlayerActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(intent);
	}

	private void doBindService() {
		// Establish a connection with the service.  We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		bindService(new Intent(MainActivity.this, VideoPlayerService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	private void doUnbindService() {
		if (mIsBound) {
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}
}
