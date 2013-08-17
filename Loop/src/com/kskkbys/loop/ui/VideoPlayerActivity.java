package com.kskkbys.loop.ui;

import com.kskkbys.loop.R;
import com.kskkbys.loop.logger.FlurryLogger;
import com.kskkbys.loop.logger.KLog;
import com.kskkbys.loop.model.FavoriteList;
import com.kskkbys.loop.model.Playlist;
import com.kskkbys.loop.model.Video;
import com.kskkbys.loop.service.PlayerCommand;
import com.kskkbys.loop.service.VideoPlayerService.PlayerEvent;
import com.kskkbys.loop.ui.fragments.PlayerControlFragment;
import com.kskkbys.loop.ui.fragments.PlayerListFragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * Video player
 *
 */
public class VideoPlayerActivity extends BaseActivity {

	private static final String TAG = VideoPlayerActivity.class.getSimpleName();

	public static final String IS_RELOAD = "is_reload";

	// Fragments in this activity
	private PlayerListFragment mListFragment;
	private PlayerControlFragment mControlFragment;

	// Receiver instance to handle action intent from service.
	private BroadcastReceiver mPlayerReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(PlayerEvent.Error.getAction())) {
				KLog.e(TAG, "PlayerEvent.Error occurs!");
				handleError();
			} else if (action.equals(PlayerEvent.InvalidVideo.getAction())) {
				handleInvalidVideo();
			} else if (action.equals(PlayerEvent.Complete.getAction())) {
				handleCompletion();
			} else if (action.equals(PlayerEvent.EndToLoad.getAction())) {
				handleEndLoadVideo();
			} else if (action.equals(PlayerEvent.StartToLoad.getAction())) {
				handleStartLoadVideo();
			} else if (action.equals(PlayerEvent.SeekComplete.getAction())) {
				int msec = intent.getExtras().getInt("msec");
				handleSeekComplete(msec);
			} else if (action.equals(PlayerEvent.Prepared.getAction())) {
				handlePrepared();
			} else if (action.equals(PlayerEvent.PositionUpdate.getAction())) {
				int msec = intent.getExtras().getInt("msec");
				boolean isPlaying = intent.getExtras().getBoolean("is_playing");
				handleUpdate(msec, isPlaying);
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		KLog.v(TAG, "onCreate");
		FlurryLogger.logEvent(FlurryLogger.SEE_PLAYER);

		// action bar
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			getSupportActionBar().hide();
			this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			Video currentVideo = Playlist.getInstance().getCurrentVideo();
			if (currentVideo != null) {
				getSupportActionBar().setTitle(currentVideo.getTitle());
			}
		}

		// Content view and fragments
		setContentView(R.layout.activity_player);
		FragmentManager fm = getSupportFragmentManager();
		mControlFragment = (PlayerControlFragment)fm.findFragmentById(R.id.fragment_control);
		mListFragment = (PlayerListFragment)fm.findFragmentById(R.id.fragment_list);

		// Register broadcast
		IntentFilter filter = new IntentFilter();
		PlayerEvent[] eventTypes = PlayerEvent.values();
		for (PlayerEvent pe: eventTypes) {
			filter.addAction(pe.getAction());
		}
		registerReceiver(mPlayerReceiver, filter);

		// If activity is created by intent, it will reload player
		if (savedInstanceState == null) {
			Intent intent = getIntent();
			if (intent != null) {
				PlayerCommand.play(this, intent.getBooleanExtra(IS_RELOAD, false));
				intent.putExtra(IS_RELOAD, false);
			}
		} else {
			// Re-initialized activity (caused by rotation)
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		KLog.v(TAG, "onNewIntent");
		// Send PLAY command to service
		if (intent != null) {
			PlayerCommand.play(this, intent.getBooleanExtra(IS_RELOAD, false));
			intent.putExtra(IS_RELOAD, false);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(IS_RELOAD, false);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		KLog.v(TAG, "onDestroy");
		// unregister
		unregisterReceiver(mPlayerReceiver);
	}

	@Override
	protected void onResume() {
		super.onResume();
		KLog.v(TAG, "onResume");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_player, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent intent = new Intent(this, MainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			finish();
			return true;
		case R.id.menu_share:
			startShareIntent();
			return true;
		case R.id.menu_favorite:
			addFavorite();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Show a dialog to publish facebook
	 */
	private void startShareIntent() {
		FlurryLogger.logEvent(FlurryLogger.SHARE_VIDEO);
		String videoUrl = Playlist.getInstance().getCurrentVideo().getVideoUrl();
		if (!TextUtils.isEmpty(videoUrl)) {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TEXT, videoUrl);
			startActivity(intent);
		}
	}

	/**
	 * Add current video to favorite list
	 */
	private void addFavorite() {
		new AsyncTask<String, Integer, Boolean>() {
			@Override
			protected Boolean doInBackground(String... params) {
				Video video = Playlist.getInstance().getCurrentVideo();
				String artist = Playlist.getInstance().getQuery();
				if (TextUtils.isEmpty(artist)) {
					// Playing fav list
					return false;
				} else {
					return FavoriteList.getInstance(VideoPlayerActivity.this).addFavorite(video, artist);
				}
			}
			@Override
			protected void onPostExecute(Boolean result) {
				if (result) {
					Toast.makeText(VideoPlayerActivity.this, R.string.loop_video_player_favorite, Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(VideoPlayerActivity.this, R.string.loop_video_player_favorite_error, Toast.LENGTH_SHORT).show();
				}
			}
		}.execute();
	}

	private void handlePrepared() {
		KLog.v(TAG, "handlePrepared");
		updateVideoInfo();
		dismissProgress();
	}

	private void handleCompletion() {
		KLog.v(TAG, "handleCompletion");
		// End of playlist => close the player
		if (Playlist.getInstance().getCurrentVideo() == null) {
			finish();
		}
	}

	private void handleSeekComplete(int positionMsec) {
		KLog.v(TAG, "handleSeekComplete");
		mControlFragment.handleSeekComplete(positionMsec);
		dismissProgress();
	}

	/**
	 * Show error when the video does not have valid url
	 */
	private void handleInvalidVideo() {
		// Update UI (video title)
		updateVideoInfo();
		// Show toast
		Toast.makeText(this, R.string.loop_video_player_invalid_video, Toast.LENGTH_SHORT).show();
		// Go next video
		PlayerCommand.next(VideoPlayerActivity.this);
	}

	/**
	 * Show toast and go to next video.
	 */
	private void handleError() {
		// Show toast
		Toast.makeText(this, R.string.loop_video_player_unknown_error, Toast.LENGTH_SHORT).show();
	}

	private void handleStartLoadVideo() {
		KLog.v(TAG, "handleStartLoadVideo");
		showProgress(R.string.loop_video_player_dialog_loading);
	}

	private void handleEndLoadVideo() {
		KLog.v(TAG, "handleEndLoadVideo");
		dismissProgress();
	}

	private void handleUpdate(int msec, boolean isPlaying) {
		mControlFragment.handleUpdate(msec, isPlaying);
	}


	private void updateVideoInfo() {
		// Update action bar (video title)
		Video video = Playlist.getInstance().getCurrentVideo();
		if (video != null) {
			getSupportActionBar().setTitle(video.getTitle());
		} else {
			getSupportActionBar().setTitle(R.string.loop_app_name);
		}

		// For fragments
		mControlFragment.updateVideoInfo();
		if (mListFragment != null) {
			mListFragment.updateVideoInfo();
		}
	}
}
