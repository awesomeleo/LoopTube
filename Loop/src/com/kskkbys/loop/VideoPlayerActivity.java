package com.kskkbys.loop;

import java.util.Timer;
import java.util.TimerTask;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class VideoPlayerActivity extends BaseActivity {

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
	
	private ListView mPlayListView;
	
	private ProgressDialog mProgressDialog;
	
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = ((VideoPlayerService.VideoPlayerServiceBinder)service).getService();
			Toast.makeText(VideoPlayerActivity.this, "Service connected", Toast.LENGTH_SHORT).show();

			// Set this activity to the service
			mService.setPlayerActivity(VideoPlayerActivity.this);
			
			// Set this SurfaceView to the service
			SurfaceView surfaceView = (SurfaceView)findViewById(R.id.surfaceView1);
			mService.setSurfaceView(surfaceView);
			
			//
			updateVideoInfo();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
			Toast.makeText(VideoPlayerActivity.this, "Service disconnected", Toast.LENGTH_SHORT).show();
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
				//mService.seekTo(progress);
				mProgressDialog = new ProgressDialog(VideoPlayerActivity.this);
				mProgressDialog.setTitle("Seeking...");
				mProgressDialog.show();
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
						if (mService != null && !mIsSeeking) {
							TextView durationView = (TextView)findViewById(R.id.durationText);
							int currentMinitues = (mService.getCurrentPosition() / 1000) / 60;
							int currentSeconds = (mService.getCurrentPosition() / 1000) % 60;
							int durationMinitues = (mService.getDuration() / 1000) / 60;
							int durationSeconds = (mService.getDuration() / 1000) % 60;
							durationView.setText(String.format("%d:%02d / %d:%02d", 
									currentMinitues, currentSeconds, durationMinitues, durationSeconds));
							
							mSeekBar.setProgress(mService.getCurrentPosition());
						}
					}
				});
			}
		}, 0, 500);

		// PlayListView
		mPlayListView = (ListView)findViewById(R.id.playListView);
		mPlayListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Log.v(TAG, "onItemClick " + position);
				// Toast.makeText(VideoPlayerActivity.this, "OnItemClick: " + position, Toast.LENGTH_SHORT).show();
				mService.startVideo(position);
			}
		});
		
		// Connect surfaceview to mediaplayer
		if (!mIsBound) {
			doBindService();
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		SurfaceView surfaceView = (SurfaceView)findViewById(R.id.surfaceView1);
		
		// When the orientation is set to landscape, it maximizes SurfaceView
		switch (newConfig.orientation) {
		case Configuration.ORIENTATION_LANDSCAPE:
			int w = surfaceView.getWidth();
			int h = w * 9 / 16;
			surfaceView.getHolder().setFixedSize(w, h);
			break;
		case Configuration.ORIENTATION_PORTRAIT:
			break;
		default:
			break;
		}
	
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (mTimer != null) {
			mTimer.cancel();
		}
		
		doUnbindService();
	}
	
	private void doBindService() {
		// Establish a connection with the service.  We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		bindService(new Intent(VideoPlayerActivity.this, VideoPlayerService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	private void doUnbindService() {
		if (mIsBound) {
			// Detach surfaceview
			mService.setSurfaceView(null);
			
			// Detach our existing connection.
			unbindService(mConnection);
			mIsBound = false;
		}
	}
	
	public void onError() {
		Toast.makeText(this, "OnError", Toast.LENGTH_SHORT).show();
	}
	
	public void onPrepared() {
		Log.v(TAG, "onPrepared");
		updateVideoInfo();
		Toast.makeText(this, "OnPrepared", Toast.LENGTH_SHORT).show();
	}
	
	public void onCompletion() {
		Toast.makeText(this, "OnCompletion", Toast.LENGTH_SHORT).show();
	}
	
	public void onSeekComplete() {
		int msec = mService.getCurrentPosition();
		mSeekBar.setProgress(msec / 1000);
		Toast.makeText(this, "OnSeekComplete", Toast.LENGTH_SHORT).show();
		
		mProgressDialog.dismiss();
		mIsSeeking = false;
	}
	
	private void updateVideoInfo() {
		if (mService == null) {
			Log.e(TAG, "Not connected with Service!");
			return;
		}
		
		// For playlist
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		for (int i=0; i<mService.getVideoList().size(); i++) {
			Video v = mService.getVideoList().get(i);
			adapter.add(v.getTitle() + " (" + Util.getDurationText(v.getDuration()) + ")");
		}
		mPlayListView.setAdapter(adapter);
		
		// For current video
		Video video = mService.getCurrentVideo();
		TextView titleView = (TextView)findViewById(R.id.videoTitleLabel);
		TextView durationView = (TextView)findViewById(R.id.durationText);
		if (video != null) {
			titleView.setText(video.getTitle());
			int minutes = (video.getDuration() / 1000) / 60;
			int seconds = (video.getDuration() / 1000) % 60;
			durationView.setText(String.format("0:00 / %d:%02d", minutes, seconds));
			mSeekBar.setMax(video.getDuration());
		} else {
			titleView.setText("");
			durationView.setText("0:00 / 0:00");
			mSeekBar.setMax(100);
		}
		
		if (mService.isPlaying()) {
			mPauseButton.setBackgroundResource(android.R.drawable.ic_media_pause);
		} else {
			mPauseButton.setBackgroundResource(android.R.drawable.ic_media_play);
		}
	}
}
