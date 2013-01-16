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
import java.util.List;

import android.os.Bundle;
import android.os.IBinder;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

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
			Toast.makeText(MainActivity.this, "Service connected", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
			Toast.makeText(MainActivity.this, "Service disconnected", Toast.LENGTH_SHORT).show();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Log.v(TAG, "onCreate");
		
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
		updateHistory();

		Button button = (Button)findViewById(R.id.searchButton);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				EditText searchEditText = (EditText)findViewById(R.id.searchText);
				String query = searchEditText.getEditableText().toString();
				searchQuery(query);
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		// update history
		updateHistory();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		doUnbindService();
	}

	private void searchQuery(String artist) {
		if (artist == null || artist.length() == 0) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Error")
			.setMessage("Please input artist name.")
			.setPositiveButton("OK", new OnClickListener() {
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
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Update history view
	 */
	private void updateHistory() {
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
					ListView listView = (ListView) parent;
					String item = (String) listView.getItemAtPosition(position);
					searchQuery(item);
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
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			startActivity(new Intent(MainActivity.this, VideoPlayerActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
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
	public void startVideoPlayer(List<Video> result) {
		this.mService.setSearchResult(result);
		this.mService.startVideo(0);

		Intent intent = new Intent(MainActivity.this, VideoPlayerActivity.class);
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
