package com.kskkbys.loop.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.kskkbys.loop.R;
import com.kskkbys.loop.fragments.PlayerControlFragment;
import com.kskkbys.loop.fragments.PlayerListFragment;
import com.kskkbys.loop.logger.FlurryLogger;
import com.kskkbys.loop.logger.KLog;
import com.kskkbys.loop.model.BlackList;
import com.kskkbys.loop.model.Playlist;
import com.kskkbys.loop.model.Video;
import com.kskkbys.loop.service.PlayerCommand;
import com.kskkbys.loop.service.VideoPlayerService.PlayerEvent;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.widget.Toast;

/**
 * Video player
 *
 */
public class VideoPlayerActivity extends BaseActivity {

	private static final String TAG = VideoPlayerActivity.class.getSimpleName();

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
			} else if (action.equals(PlayerEvent.Update.getAction())) {
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
		getSupportMenuInflater().inflate(R.menu.activity_player, menu);
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
		case R.id.menu_ignore_current:
			ignoreCurrentVideo();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	public void startContextualActionBar(int position) {
		final int longClickedPos = position;
		startActionMode(new ActionMode.Callback() {
			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				MenuInflater inflater = getSupportMenuInflater();
				inflater.inflate(R.menu.activity_player_cab, menu);
				return true;
			}
			@Override
			public boolean onPrepareActionMode(ActionMode mode,
					Menu menu) {
				return false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode,
					MenuItem item) {
				switch (item.getItemId()) {
				case R.id.menu_ignore:
					ignoreVideo(longClickedPos);
					mode.finish(); // Action picked, so close the CAB
					return true;
				default:
					return false;
				}
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
			}
		});
	}

	/**
	 * Add the current video to black list
	 */
	private void ignoreCurrentVideo() {
		KLog.v(TAG, "ignoreCurrentVideo");
		Video video = Playlist.getInstance().getCurrentVideo();
		if (video != null) {
			String videoId = video.getId();
			BlackList.getInstance().addUserBlackList(videoId);
			//
			Toast.makeText(this, R.string.loop_video_player_ignored, Toast.LENGTH_SHORT).show();
			// Go next video
			PlayerCommand.next(VideoPlayerActivity.this);
		}
	}
	
	private void ignoreVideo(int position) {
		KLog.v(TAG, "ignoreVideo");
		Video video = Playlist.getInstance().getVideoAtIndex(position);
		if (video != null) {
			String videoId = video.getId();
			BlackList.getInstance().addUserBlackList(videoId);
			//
			Toast.makeText(this, R.string.loop_video_player_ignored, Toast.LENGTH_SHORT).show();
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

	private void handlePrepared() {
		KLog.v(TAG, "onPrepared");
		updateVideoInfo();
		dismissProgress();
	}

	private void handleCompletion() {
		KLog.v(TAG, "OnCompletion");
		// End of playlist => close the player
		if (Playlist.getInstance().getCurrentVideo() == null) {
			finish();
		}
	}
	
	private void handleSeekComplete(int positionMsec) {
		KLog.v(TAG, "onSeekComplete");
		mControlFragment.handleSeekComplete(positionMsec);
		dismissProgress();
	}

	/**
	 * Show error when the video does not have valid url
	 */
	private void handleInvalidVideo() {
		// Update UI (video title)
		updateVideoInfo();
		// Show error dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.loop_video_player_invalid_video);
		builder.setPositiveButton(R.string.loop_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				PlayerCommand.next(VideoPlayerActivity.this);
			}
		});
		builder.create().show();
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
		mListFragment.updateVideoInfo();
	}

	private void handleStartLoadVideo() {
		showProgress(R.string.loop_video_player_dialog_loading);
	}

	private void handleEndLoadVideo() {
		dismissProgress();
	}
	
	private void handleUpdate(int msec, boolean isPlaying) {
		mControlFragment.handleUpdate(msec, isPlaying);
	}
}
