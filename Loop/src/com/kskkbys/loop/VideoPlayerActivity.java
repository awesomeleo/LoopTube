package com.kskkbys.loop;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.actionbarsherlock.view.Menu;
import com.kskkbys.loop.logger.KLog;
import com.kskkbys.loop.playlist.Playlist;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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
import android.widget.TextView;;

/**
 * Video player
 *
 */
public class VideoPlayerActivity extends BaseActivity implements VideoPlayerService.MediaPlayerCallback {

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
	private boolean mIsShowingControl;
	private SurfaceView mSurfaceView;

	private ListView mPlayListView;

	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			KLog.v(TAG, "service connected");
			mService = ((VideoPlayerService.VideoPlayerServiceBinder)service).getService();
			// Toast.makeText(VideoPlayerActivity.this, "Service connected", Toast.LENGTH_SHORT).show();

			// Set this activity to the service
			mService.setListener(VideoPlayerActivity.this);

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
		setContentView(R.layout.activity_video_player);

		KLog.v(TAG, "onCreate");

		// Controller
		mPrevButton = findViewById(R.id.prevButton);
		mPrevButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mService.prev();
			}
		});
		mNextButton = findViewById(R.id.nextButton);
		mNextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mService.next();
			}
		});
		mPauseButton = findViewById(R.id.pauseButton);
		mPauseButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mService.isPlaying()) {
					mService.pause();
					mPauseButton.setBackgroundResource(R.drawable.play);
				} else {
					mService.play();
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
				switchMute();
			}
		});
		mDurationView = (TextView)findViewById(R.id.durationText);
		mSeekBar = (SeekBar)findViewById(R.id.playerSeekBar);
		mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				KLog.v(TAG, "onStopTrackingTouch");
				showProgress(R.string.loop_video_player_dialog_seeking);
				mService.seekTo(mSeekBar.getProgress());
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
				mSurfaceView = (SurfaceView)findViewById(R.id.surfaceView1);
				SurfaceHolder holder = mSurfaceView.getHolder();
				holder.addCallback(new SurfaceHolder.Callback() {
					@Override
					public void surfaceDestroyed(SurfaceHolder holder) {
						KLog.v(TAG, "surface destroyed");
						VideoPlayerService.setSurfaceHolder(null);
					}

					@Override
					public void surfaceCreated(SurfaceHolder holder) {
						KLog.v(TAG, "surface created");
						// After surface view is created, attach it to MediaPlayer
						int width = mSurfaceView.getWidth();
						int height = width * 9 / 16;
						holder.setFixedSize(width, height);
						VideoPlayerService.setSurfaceHolder(holder);
					}

					@Override
					public void surfaceChanged(SurfaceHolder holder, int format, int width,
							int height) {
						KLog.v(TAG, "surafce changed");
					}
				});

				// PlayListView
				mPlayListView = (ListView)findViewById(R.id.playListView);
				mPlayListView.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						KLog.v(TAG, "onItemClick " + position);
						// Toast.makeText(VideoPlayerActivity.this, "OnItemClick: " + position, Toast.LENGTH_SHORT).show();
						Playlist.getInstance().setPlayingIndex(position);
						mService.startVideo();
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
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		KLog.v(TAG, "onDestroy");

		if (mSeekBarTimer != null) {
			mSeekBarTimer.cancel();
		}

		doUnbindService();
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

	/*
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_repeat:
			switchLoop();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}*/

	/**
	 * Switch looping
	 */
	private void switchLoop() {
		if (Playlist.getInstance().getCurrentVideo() != null) {
			if (mService != null) {
				if (mService.isLooping()) {
					// stop looping
					KLog.v(TAG, "Stop looping");
					mService.setLooping(false);
					mLoopButton.setBackgroundResource(R.drawable.synchronize_off);
				} else {
					// start looping
					KLog.v(TAG, "Start looping");
					mService.setLooping(true);
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
			if (mService != null) {
				if (mService.isMute()) {
					KLog.v(TAG, "Mute Off");
					mService.setMute(false);
					mVolumeButton.setBackgroundResource(R.drawable.volume_plus2);
				} else {
					KLog.v(TAG, "Mute On");
					mService.setMute(true);
					mVolumeButton.setBackgroundResource(R.drawable.volume_off);
				}
			}
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
			// Detach surfaceview
			// mService.setSurfaceView(null);

			// Remove listener of service
			mService.setListener(null);
			mService = null;

			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	@Override
	public void onError() {
		KLog.v(TAG, "OnError");
		//Toast.makeText(this, "OnError", Toast.LENGTH_SHORT).show();
	}



	@Override
	public void onPrepared() {
		KLog.v(TAG, "onPrepared");
		updateVideoInfo();
		dismissProgress();
		//Toast.makeText(this, "OnPrepared", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onCompletion() {
		KLog.v(TAG, "OnCompletion");
		//Toast.makeText(this, "OnCompletion", Toast.LENGTH_SHORT).show();

		// End of playlist => close the player
		if (Playlist.getInstance().getCurrentVideo() == null) {
			// SimpleErrorDialog.show(this, R.string.video_player_dialog_end);
			finish();
		}
	}

	@Override
	public void onSeekComplete(int positionMsec) {
		int msec = mService.getCurrentPosition();
		mSeekBar.setProgress(msec / 1000);

		KLog.v(TAG, "onSeekComplete");
		//Toast.makeText(this, "OnSeekComplete", Toast.LENGTH_SHORT).show();

		dismissProgress();
		mIsSeeking = false;
	}

	@Override
	public void onInvalidVideoError() {
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
				mService.next();
			}
		});
		builder.create().show();
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

		if (mService.isLooping()) {
			mLoopButton.setBackgroundResource(R.drawable.synchronize_on);
		} else {
			mLoopButton.setBackgroundResource(R.drawable.synchronize_off);
		}

		if (mService.isMute()) {
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
		} else {
			mVolumeButton.setVisibility(View.VISIBLE);
			mLoopButton.setVisibility(View.VISIBLE);
			mSeekBar.setVisibility(View.VISIBLE);
			mDurationView.setVisibility(View.VISIBLE);
			mPauseButton.setVisibility(View.VISIBLE);
			mPrevButton.setVisibility(View.VISIBLE);
			mNextButton.setVisibility(View.VISIBLE);
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
				TextView textView = (TextView)v.findViewById(R.id.videoTitleViewInList);
				textView.setText(video.getTitle());

				ImageView imageView = (ImageView)v.findViewById(R.id.nowPlayingImageInList);
				if (Playlist.getInstance().getPlayingIndex() == position) {
					imageView.setImageResource(R.drawable.volume_plus2);
				} else {
					imageView.setImageBitmap(null);
				}
			}
			return v;
		}

	}

	@Override
	public void onStartLoadVideo() {
		showProgress(R.string.loop_video_player_dialog_loading);
	}

	@Override
	public void onEndLoadVideo() {
		dismissProgress();
	}
}
