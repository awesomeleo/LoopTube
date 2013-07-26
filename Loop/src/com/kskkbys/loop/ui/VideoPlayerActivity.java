package com.kskkbys.loop.ui;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.kskkbys.loop.R;
import com.kskkbys.loop.Video;
import com.kskkbys.loop.VideoPlayerService;
import com.kskkbys.loop.VideoPlayerService.PlayerEvent;
import com.kskkbys.loop.audio.MuteManager;
import com.kskkbys.loop.logger.FlurryLogger;
import com.kskkbys.loop.logger.KLog;
import com.kskkbys.loop.playlist.BlackList;
import com.kskkbys.loop.playlist.LoopManager;
import com.kskkbys.loop.playlist.Playlist;
import com.kskkbys.loop.service.PlayerCommand;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Video player
 *
 */
public class VideoPlayerActivity extends BaseActivity 
implements SurfaceHolder.Callback {

	private static final String TAG = VideoPlayerActivity.class.getSimpleName();

	// Services
	private VideoPlayerService mService;
	private boolean mIsBound = false;

	// UI
	private TextView mDurationView;
	private SeekBar mSeekBar;
	private Handler mHandler = new Handler();
	private boolean mIsSeeking = false;
	private Timer mSeekBarTimer;

	private Date mLastTouchDate;
	private Timer mTouchEventTimer;

	private View mPauseButton;
	private View mPrevButton;
	private View mNextButton;
	private View mLoopButton;
	private View mVolumeButton;
	private View mVolumeButtonInDialog;
	private View mFullScreenButton;
	private boolean mIsShowingControl;
	private SurfaceView mSurfaceView;

	private ListView mPlayListView;
	
	private BroadcastReceiver mPlayerReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(PlayerEvent.Error.getAction())) {
				
			} else if (action.equals(PlayerEvent.InvalidVideoError.getAction())) {
				handleInvalidVideoError();
			} else if (action.equals(PlayerEvent.Complete.getAction())) {
				handleCompletion();
			} else if (action.equals(PlayerEvent.EndToLoad.getAction())) {
				handleEndLoadVideo();
			} else if (action.equals(PlayerEvent.StartToLoad.getAction())) {
				handleStartLoadVideo();
			} else if (action.equals(PlayerEvent.SeekComplete.getAction())) {
				handleSeekComplete(intent.getExtras().getInt("msec"));
			} else if (action.equals(PlayerEvent.Prepared.getAction())) {
				handlePrepared();
			}
		}
	};

	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			KLog.v(TAG, "service connected");
			mService = ((VideoPlayerService.VideoPlayerServiceBinder)service).getService();
			// Toast.makeText(VideoPlayerActivity.this, "Service connected", Toast.LENGTH_SHORT).show();

			// If the MediaPlayer is INIT_STATE(= loading video), show progress
			if (mService.getState() == VideoPlayerService.STATE_INIT) {
				showProgress(R.string.loop_video_player_dialog_loading);
			}

			//
			updateVideoInfo();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
			KLog.v(TAG, "Service disconnected");
			//Toast.makeText(VideoPlayerActivity.this, "Service disconnected", Toast.LENGTH_SHORT).show();
		}
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// action bar
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		setContentView(R.layout.activity_video_player);

		KLog.v(TAG, "onCreate");
		FlurryLogger.logEvent(FlurryLogger.SEE_PLAYER);

		// Controller
		mPrevButton = findViewById(R.id.prevButton);
		mPrevButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				PlayerCommand.prev(VideoPlayerActivity.this);
			}
		});
		mNextButton = findViewById(R.id.nextButton);
		mNextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				PlayerCommand.next(VideoPlayerActivity.this);
			}
		});
		mPauseButton = findViewById(R.id.pauseButton);
		mPauseButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mService.isPlaying()) {
					PlayerCommand.pause(VideoPlayerActivity.this);
					mPauseButton.setBackgroundResource(R.drawable.play);
				} else {
					PlayerCommand.play(VideoPlayerActivity.this, false);
					mPauseButton.setBackgroundResource(R.drawable.pause);
				}
			}
		});
		mLoopButton = findViewById(R.id.loopButton);
		mLoopButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				KLog.v(TAG, "loop clicked");
				switchLoop();
			}
		});
		mVolumeButton = findViewById(R.id.volumeButton);
		mVolumeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				KLog.v(TAG, "volume clicked");
				showVolumeSettingDialog();
			}
		});
		mFullScreenButton = findViewById(R.id.fullScreenButton);
		mFullScreenButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				KLog.v(TAG, "fullScreen");
			}
		});
		mDurationView = (TextView)findViewById(R.id.durationText);
		mSeekBar = (SeekBar)findViewById(R.id.playerSeekBar);
		mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				KLog.v(TAG, "onStopTrackingTouch");
				showProgress(R.string.loop_video_player_dialog_seeking);
				PlayerCommand.seek(VideoPlayerActivity.this, mSeekBar.getProgress());
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				KLog.v(TAG, "onStartTrackingTouch");
				mIsSeeking = true;
			}
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// KLog.v(TAG, "onProgressChanged");
			}
		});

		// Update seek position with handler
		mSeekBarTimer = new Timer();
		mSeekBarTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						//KLog.v(TAG, "SeekBar update");
						if (Playlist.getInstance().getCurrentVideo() == null) {
							// finished to play
							mSeekBarTimer.cancel();
						} else {
							if (mService != null && !mIsSeeking) {
								TextView durationView = (TextView)findViewById(R.id.durationText);
								int currentMinitues = (mService.getCurrentPosition() / 1000) / 60;
								int currentSeconds = (mService.getCurrentPosition() / 1000) % 60;
								int durationMinitues = (Playlist.getInstance().getCurrentVideo().getDuration() / 1000) / 60;
								int durationSeconds = (Playlist.getInstance().getCurrentVideo().getDuration() / 1000) % 60;
								durationView.setText(String.format("%d:%02d / %d:%02d", 
										currentMinitues, currentSeconds, durationMinitues, durationSeconds));

								mSeekBar.setProgress(mService.getCurrentPosition());
							}
						}
					}
				});
			}
		}, 0, 500);

		mLastTouchDate = new Date();
		mTouchEventTimer = new Timer();
		mTouchEventTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						if (mIsShowingControl) {
							Date date = new Date();
							if (date.getTime() - mLastTouchDate.getTime() > 10 * 1000) {
								mIsShowingControl = false;
								updateControlVisibility();
							}
						}
					}
				});
			}
		}, 0, 1000);

		// SurfaceView
		// getWindow().setFormat(PixelFormat.TRANSPARENT);		// needed???
		mSurfaceView = (SurfaceView)findViewById(R.id.surfaceView1);
		SurfaceHolder holder = mSurfaceView.getHolder();
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		holder.addCallback(this);

		// PlayListView
		mPlayListView = (ListView)findViewById(R.id.playListView);
		mPlayListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				KLog.v(TAG, "onItemClick " + position);
				Video touchedVideo = Playlist.getInstance().getVideoAtIndex(position);
				BlackList bl = BlackList.getInstance();
				if (bl.containsByUser(touchedVideo.getId()) || bl.containsByApp(touchedVideo.getId())) {
					// In blacklist: can not play it.
					KLog.v(TAG, "This video can not be played.");
					Toast.makeText(VideoPlayerActivity.this, R.string.loop_video_player_ignored_already, Toast.LENGTH_SHORT).show();
				} else {
					Playlist.getInstance().setPlayingIndex(position);
					PlayerCommand.play(VideoPlayerActivity.this, true);
				}
			}
		});

		mIsSeeking = false;
		mIsShowingControl = true;

		// Connect surfaceview to mediaplayer
		if (!mIsBound) {
			doBindService();
		}

		// action bar
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			getSupportActionBar().hide();
		}
		
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
		if (mSeekBarTimer != null) {
			mSeekBarTimer.cancel();
		}
		if (mTouchEventTimer != null) {
			mTouchEventTimer.cancel();
		}
		doUnbindService();
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
		case R.id.menu_ignore:
			ignoreCurrentVideo();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
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

	/**
	 * Switch looping
	 */
	private void switchLoop() {
		if (Playlist.getInstance().getCurrentVideo() != null) {
			if (mService != null) {
				if (LoopManager.getInstance().isLooping()) {
					// stop looping
					KLog.v(TAG, "Stop looping");
					PlayerCommand.setLooping(VideoPlayerActivity.this, false);
					mLoopButton.setBackgroundResource(R.drawable.synchronize_off);
				} else {
					// start looping
					KLog.v(TAG, "Start looping");
					PlayerCommand.setLooping(VideoPlayerActivity.this, true);
					mLoopButton.setBackgroundResource(R.drawable.synchronize_on);
				}
			}
		}
	}

	/**
	 * Switch mute on/off
	 */
	private void switchMute() {
		if (Playlist.getInstance().getCurrentVideo() != null) {
			if (MuteManager.getInstance().isMute()) {
				KLog.v(TAG, "Mute Off");
				MuteManager.getInstance().setMute(this, false);
				mVolumeButton.setBackgroundResource(R.drawable.volume_plus2);
				mVolumeButtonInDialog.setBackgroundResource(R.drawable.volume_plus2);
			} else {
				KLog.v(TAG, "Mute On");
				MuteManager.getInstance().setMute(this, true);
				mVolumeButton.setBackgroundResource(R.drawable.volume_off);
				mVolumeButtonInDialog.setBackgroundResource(R.drawable.volume_off);
			}
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

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		KLog.v(TAG, "onTouchEvent");
		if (event.getAction() == MotionEvent.ACTION_UP) {
			KLog.v(TAG, "onTouchEvent(up)");
			mLastTouchDate = new Date();
			if (mIsShowingControl) {
				// mIsShowingControl = false;
				// updateControlVisibility();
			} else {
				mIsShowingControl = true;
				updateControlVisibility();
			}
		}
		return super.onTouchEvent(event);
	}

	private void doBindService() {
		// Establish a connection with the service.  We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		bindService(new Intent(VideoPlayerActivity.this, VideoPlayerService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	/**
	 * Unbind player service with this activity
	 */
	private void doUnbindService() {
		if (mIsBound) {
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	private void handlePrepared() {
		KLog.v(TAG, "onPrepared");
		updateVideoInfo();
		dismissProgress();

		// For Android 2.3: When start to play next video, attach SurfaceView again.
		// attachSurfaceViewToPlayer();
	}

	private void handleCompletion() {
		KLog.v(TAG, "OnCompletion");
		//Toast.makeText(this, "OnCompletion", Toast.LENGTH_SHORT).show();

		// End of playlist => close the player
		if (Playlist.getInstance().getCurrentVideo() == null) {
			// SimpleErrorDialog.show(this, R.string.video_player_dialog_end);
			finish();
		}
	}

	private void handleSeekComplete(int positionMsec) {
		KLog.v(TAG, "onSeekComplete");
		mSeekBar.setProgress(positionMsec / 1000);
		dismissProgress();
		mIsSeeking = false;
	}

	private void handleInvalidVideoError() {
		this.showInvalidVideoError();
	}

	/**
	 * Show error when the video does not have valid url
	 */
	private void showInvalidVideoError() {
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

	private void showVolumeSettingDialog() {
		final AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		final int maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		KLog.v(TAG, "maxVolume = " + maxVolume);
		LayoutInflater inflater = LayoutInflater.from(this);
		View layout = inflater.inflate(R.layout.dialog_volume, (ViewGroup)findViewById(R.id.layoutVoluemeDialog));

		mVolumeButtonInDialog = (ImageView)layout.findViewById(R.id.volumeImageView);
		if (MuteManager.getInstance().isMute()) {
			mVolumeButtonInDialog.setBackgroundResource(R.drawable.volume_off);
		}
		mVolumeButtonInDialog.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				switchMute();
			}
		});

		SeekBar volumeBar = (SeekBar)layout.findViewById(R.id.volumeSeekBar);
		volumeBar.setMax(maxVolume);
		volumeBar.setProgress(am.getStreamVolume(AudioManager.STREAM_MUSIC));
		volumeBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
			}
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				KLog.v(TAG, "volume changed");
				am.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
			}
		});
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(layout)
		.setPositiveButton(R.string.loop_ok, null)
		.setTitle(R.string.loop_video_player_volume_setting)
		.create().show();
	}

	private void updateVideoInfo() {
		if (mService == null) {
			KLog.e(TAG, "Not connected with Service!");
			return;
		}

		// For playlist
		VideoAdapter adapter = new VideoAdapter();
		mPlayListView.setAdapter(adapter);

		// For current video
		Video video = Playlist.getInstance().getCurrentVideo();
		if (video != null) {
			getSupportActionBar().setTitle(video.getTitle());
			int minutes = (video.getDuration() / 1000) / 60;
			int seconds = (video.getDuration() / 1000) % 60;
			mDurationView.setText(String.format("0:00 / %d:%02d", minutes, seconds));
			mSeekBar.setMax(video.getDuration());
		} else {
			getSupportActionBar().setTitle(R.string.loop_app_name);
			mDurationView.setText("0:00 / 0:00");
			mSeekBar.setMax(100);
		}

		if (mService.isPlaying()) {
			mPauseButton.setBackgroundResource(R.drawable.pause);
		} else {
			mPauseButton.setBackgroundResource(R.drawable.play);
		}

		if (LoopManager.getInstance().isLooping()) {
			mLoopButton.setBackgroundResource(R.drawable.synchronize_on);
		} else {
			mLoopButton.setBackgroundResource(R.drawable.synchronize_off);
		}

		if (MuteManager.getInstance().isMute()) {
			mVolumeButton.setBackgroundResource(R.drawable.volume_off);
		} else {
			mVolumeButton.setBackgroundResource(R.drawable.volume_plus2);
		}
	}

	private void updateControlVisibility() {
		// Dismiss UI controls when touch event has not been invoked
		if (!mIsShowingControl) {
			mVolumeButton.setVisibility(View.INVISIBLE);
			mLoopButton.setVisibility(View.INVISIBLE);
			mSeekBar.setVisibility(View.INVISIBLE);
			mDurationView.setVisibility(View.INVISIBLE);
			mPauseButton.setVisibility(View.INVISIBLE);
			mPrevButton.setVisibility(View.INVISIBLE);
			mNextButton.setVisibility(View.INVISIBLE);
			mFullScreenButton.setVisibility(View.INVISIBLE);
		} else {
			mVolumeButton.setVisibility(View.VISIBLE);
			mLoopButton.setVisibility(View.VISIBLE);
			mSeekBar.setVisibility(View.VISIBLE);
			mDurationView.setVisibility(View.VISIBLE);
			mPauseButton.setVisibility(View.VISIBLE);
			mPrevButton.setVisibility(View.VISIBLE);
			mNextButton.setVisibility(View.VISIBLE);
			mFullScreenButton.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Adapter for playlist view
	 *
	 */
	private class VideoAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return Playlist.getInstance().getCount();
		}

		@Override
		public Object getItem(int position) {
			return Playlist.getInstance().getVideoAtIndex(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(R.layout.row_video_player_playlist, null);
			}
			Video video = (Video)getItem(position);
			if (video != null) {
				// video title
				TextView textView = (TextView)v.findViewById(R.id.videoTitleViewInList);
				textView.setText(video.getTitle());
				// now playing or blacklist or nothing
				ImageView imageView = (ImageView)v.findViewById(R.id.nowPlayingImageInList);
				if (Playlist.getInstance().getPlayingIndex() == position) {
					imageView.setImageResource(R.drawable.volume_plus2);
				} else if (BlackList.getInstance().containsByUser(video.getId())) {
					imageView.setImageResource(R.drawable.prohibited);
				} else if (BlackList.getInstance().containsByApp(video.getId())) {
					imageView.setImageResource(R.drawable.circle_exclamation);
				} else {
					imageView.setImageBitmap(null);
				}
			}
			return v;
		}

	}

	private void handleStartLoadVideo() {
		showProgress(R.string.loop_video_player_dialog_loading);
	}

	private void handleEndLoadVideo() {
		dismissProgress();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		KLog.v(TAG, "surface destroyed");
		VideoPlayerService.setSurfaceHolder(null);
	}
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		KLog.v(TAG, "surface created");
		// After surface view is created, attach it to MediaPlayer
		attachSurfaceViewToPlayer();
	}
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		KLog.v(TAG, "surafce changed");
		KLog.v(TAG, "w = " + width);
		KLog.v(TAG, "h = " + height);
	}

	/**
	 * Set SurfaceView to MediaPlayer
	 */
	private void attachSurfaceViewToPlayer() {
		SurfaceHolder holder = mSurfaceView.getHolder();
		// set size
		KLog.v(TAG, "before width = " + mSurfaceView.getWidth());
		KLog.v(TAG, "before height = " + mSurfaceView.getHeight());
		int width = mSurfaceView.getWidth();
		int height = width * 9 / 16;
		KLog.v(TAG, "set width = " + width);
		KLog.v(TAG, "set height = " + height);
		holder.setFixedSize(width, height);
		setSurfaceViewSize(mSurfaceView, width, height);	// for Android 2.3
		VideoPlayerService.setSurfaceHolder(holder);
	}

	/**
	 * For Android 2.3
	 * @param view
	 * @param width
	 * @param height
	 */
	private void setSurfaceViewSize(SurfaceView view, int width, int height) {
		android.view.ViewGroup.LayoutParams layout = view.getLayoutParams();
		layout.width = width;
		layout.height = height;
		view.setLayoutParams(layout);
	}
}
