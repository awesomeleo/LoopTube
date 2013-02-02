package com.kskkbys.loop;

import java.util.Timer;
import java.util.TimerTask;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.kskkbys.loop.dialog.ProgressDialogFragment;
import com.kskkbys.loop.playlist.Playlist;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
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
import android.widget.Button;
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
	private SeekBar mSeekBar;
	private Handler mHandler = new Handler();
	private boolean mIsSeeking = false;
	private Timer mTimer;

	private Button mPauseButton;
	private SurfaceView mSurfaceView;
	private ListView mPlayListView;

	private ProgressDialogFragment mProgressDialogFragment;

	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.v(TAG, "service connected");
			mService = ((VideoPlayerService.VideoPlayerServiceBinder)service).getService();
			// Toast.makeText(VideoPlayerActivity.this, "Service connected", Toast.LENGTH_SHORT).show();

			// Set this activity to the service
			mService.setListener(VideoPlayerActivity.this);

			//
			updateVideoInfo();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
			Log.v(TAG, "Service disconnected");
			//Toast.makeText(VideoPlayerActivity.this, "Service disconnected", Toast.LENGTH_SHORT).show();
		}
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_player);

		Log.v(TAG, "onCreate");

		// Controller
		findViewById(R.id.prevButton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mService.prev();
			}
		});
		findViewById(R.id.nextButton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mService.next();
			}
		});
		mPauseButton = (Button)findViewById(R.id.pauseButton);
		mPauseButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mService.isPlaying()) {
					mService.pause();
					mPauseButton.setBackgroundResource(android.R.drawable.ic_media_play);
				} else {
					mService.play();
					mPauseButton.setBackgroundResource(android.R.drawable.ic_media_pause);
				}
			}
		});
		mSeekBar = (SeekBar)findViewById(R.id.playerSeekBar);
		mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				Log.v(TAG, "onStopTrackingTouch");
				showProgress(R.string.loop_video_player_dialog_seeking);
				mService.seekTo(mSeekBar.getProgress());
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				Log.v(TAG, "onStartTrackingTouch");
				mIsSeeking = true;
			}
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// Log.v(TAG, "onProgressChanged");
			}
		});

		// Update seek position with handler
		mTimer = new Timer();
		mTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						//Log.v(TAG, "SeekBar update");
						if (Playlist.getInstance().getCurrentVideo() == null) {
							// finished to play
							mTimer.cancel();
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

		// SurfaceView
		mSurfaceView = (SurfaceView)findViewById(R.id.surfaceView1);
		SurfaceHolder holder = mSurfaceView.getHolder();
		holder.addCallback(new SurfaceHolder.Callback() {
			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				Log.v(TAG, "surface destroyed");
				VideoPlayerService.setSurfaceHolder(null);
			}

			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				Log.v(TAG, "surface created");
				// After surface view is created, attach it to MediaPlayer
				int width = mSurfaceView.getWidth();
				int height = width * 9 / 16;
				holder.setFixedSize(width, height);
				VideoPlayerService.setSurfaceHolder(holder);
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width,
					int height) {
				Log.v(TAG, "surafce changed");
			}
		});

		// PlayListView
		mPlayListView = (ListView)findViewById(R.id.playListView);
		mPlayListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Log.v(TAG, "onItemClick " + position);
				// Toast.makeText(VideoPlayerActivity.this, "OnItemClick: " + position, Toast.LENGTH_SHORT).show();
				Playlist.getInstance().setPlayingIndex(position);
				mService.startVideo();
			}
		});

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

		Log.v(TAG, "onDestroy");

		if (mTimer != null) {
			mTimer.cancel();
		}

		doUnbindService();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.v(TAG, "onResume");
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
		case R.id.menu_repeat:
			if (Playlist.getInstance().getCurrentVideo() != null) {
				if (mService != null) {
					if (mService.isLooping()) {
						// stop looping
						mService.setLooping(false);
						item.setTitle(R.string.loop_menu_item_start_repeat);
					} else {
						// start looping
						mService.setLooping(true);
						item.setTitle(R.string.loop_menu_item_stop_repeat);
					}
				}
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		Log.v(TAG, "onTouchEvent");
		if (event.getAction() == MotionEvent.ACTION_UP) {
			Log.v(TAG, "up");

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

			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}

	@Override
	public void onError() {
		Log.v(TAG, "OnError");
		//Toast.makeText(this, "OnError", Toast.LENGTH_SHORT).show();
	}



	@Override
	public void onPrepared() {
		Log.v(TAG, "onPrepared");
		updateVideoInfo();
		//Toast.makeText(this, "OnPrepared", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onCompletion() {
		Log.v(TAG, "OnCompletion");
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

		Log.v(TAG, "onSeekComplete");
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
			Log.e(TAG, "Not connected with Service!");
			return;
		}

		// For playlist
		VideoAdapter adapter = new VideoAdapter();
		mPlayListView.setAdapter(adapter);

		// For current video
		Video video = Playlist.getInstance().getCurrentVideo();
		TextView durationView = (TextView)findViewById(R.id.durationText);
		if (video != null) {
			getSupportActionBar().setTitle(video.getTitle());
			int minutes = (video.getDuration() / 1000) / 60;
			int seconds = (video.getDuration() / 1000) % 60;
			durationView.setText(String.format("0:00 / %d:%02d", minutes, seconds));
			mSeekBar.setMax(video.getDuration());
		} else {
			getSupportActionBar().setTitle(R.string.loop_app_name);
			durationView.setText("0:00 / 0:00");
			mSeekBar.setMax(100);
		}

		if (mService.isPlaying()) {
			mPauseButton.setBackgroundResource(android.R.drawable.ic_media_pause);
		} else {
			mPauseButton.setBackgroundResource(android.R.drawable.ic_media_play);
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
					imageView.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
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

	private void showProgress(int resId) {
		// Remove prev fragment
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);
		// Show dialog fragment
		mProgressDialogFragment = ProgressDialogFragment.newInstance(resId);
		mProgressDialogFragment.show(ft, "dialog");
	}

	private void dismissProgress() {
		if (mProgressDialogFragment != null) {
			mProgressDialogFragment.dismiss();
			mProgressDialogFragment = null;
		}
	}
}
