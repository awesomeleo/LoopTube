package com.kskkbys.loop.fragments;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.actionbarsherlock.app.SherlockFragment;
import com.kskkbys.loop.R;
import com.kskkbys.loop.audio.MuteManager;
import com.kskkbys.loop.logger.KLog;
import com.kskkbys.loop.model.LoopManager;
import com.kskkbys.loop.model.Playlist;
import com.kskkbys.loop.model.Video;
import com.kskkbys.loop.service.PlayerCommand;
import com.kskkbys.loop.service.VideoPlayerService;
import com.kskkbys.loop.ui.VideoPlayerActivity;

/**
 * This class is a fragment of video control.
 * @author keisuke.kobayashi
 *
 */
public class PlayerControlFragment extends SherlockFragment implements OnTouchListener, SurfaceHolder.Callback {

	private static final String TAG = PlayerControlFragment.class.getSimpleName();

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

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container == null) {
			return null;
		}
		View view = inflater.inflate(R.layout.fragment_player_control, container, false);
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mIsSeeking = false;
		mIsShowingControl = true;
		
		// Controller
		View view = getView();
		mPrevButton = view.findViewById(R.id.prevButton);
		mPrevButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				PlayerCommand.prev(getActivity());
			}
		});
		mNextButton = view.findViewById(R.id.nextButton);
		mNextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				PlayerCommand.next(getActivity());
			}
		});
		mPauseButton = view.findViewById(R.id.pauseButton);
		mPauseButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Judge play/pause from view
				if (true) {
					PlayerCommand.pause(getActivity());
					mPauseButton.setBackgroundResource(R.drawable.play);
				} else {
					PlayerCommand.play(getActivity(), false);
					mPauseButton.setBackgroundResource(R.drawable.pause);
				}
			}
		});
		mLoopButton = view.findViewById(R.id.loopButton);
		mLoopButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				KLog.v(TAG, "loop clicked");
				toggleLoop();
			}
		});
		mVolumeButton = view.findViewById(R.id.volumeButton);
		mVolumeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				KLog.v(TAG, "volume clicked");
				showVolumeSettingDialog();
			}
		});
		mFullScreenButton = view.findViewById(R.id.fullScreenButton);
		mFullScreenButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				KLog.v(TAG, "fullScreen");
			}
		});
		mDurationView = (TextView)view.findViewById(R.id.durationText);
		mSeekBar = (SeekBar)view.findViewById(R.id.playerSeekBar);
		mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				KLog.v(TAG, "onStopTrackingTouch");
				// TODO showProgress(R.string.loop_video_player_dialog_seeking);
				PlayerCommand.seek(getActivity(), mSeekBar.getProgress());
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
		/*
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
								TextView durationView = (TextView)view.findViewById(R.id.durationText);
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
		*/

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
		mSurfaceView = (SurfaceView)view.findViewById(R.id.surfaceView1);
		SurfaceHolder holder = mSurfaceView.getHolder();
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		holder.addCallback(this);
	}

	@Override
	public boolean onTouch(View view, MotionEvent event) {
		KLog.v(TAG, "onTouch");
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
		return true;
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		// When this fragment is destroyed, it must release timer tasks.
		releaseTimer();
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

	private void releaseTimer() {
		if (mSeekBarTimer != null) {
			mSeekBarTimer.cancel();
		}
		if (mTouchEventTimer != null) {
			mTouchEventTimer.cancel();
		}
	}
	
	/**
	 * Switch looping
	 */
	private void toggleLoop() {
		if (Playlist.getInstance().getCurrentVideo() != null) {
			if (LoopManager.getInstance().isLooping()) {
				// stop looping
				KLog.v(TAG, "Stop looping");
				PlayerCommand.setLooping(getActivity(), false);
				mLoopButton.setBackgroundResource(R.drawable.synchronize_off);
			} else {
				// start looping
				KLog.v(TAG, "Start looping");
				PlayerCommand.setLooping(getActivity(), true);
				mLoopButton.setBackgroundResource(R.drawable.synchronize_on);
			}
		}
	}

	/**
	 * Switch mute on/off
	 */
	private void toggleMute() {
		if (Playlist.getInstance().getCurrentVideo() != null) {
			if (MuteManager.getInstance().isMute()) {
				KLog.v(TAG, "Mute Off");
				MuteManager.getInstance().setMute(getActivity(), false);
				mVolumeButton.setBackgroundResource(R.drawable.volume_plus2);
				mVolumeButtonInDialog.setBackgroundResource(R.drawable.volume_plus2);
			} else {
				KLog.v(TAG, "Mute On");
				MuteManager.getInstance().setMute(getActivity(), true);
				mVolumeButton.setBackgroundResource(R.drawable.volume_off);
				mVolumeButtonInDialog.setBackgroundResource(R.drawable.volume_off);
			}
		}
	}

	private void showVolumeSettingDialog() {
		final AudioManager am = (AudioManager)getActivity().getSystemService(Context.AUDIO_SERVICE);
		final int maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		KLog.v(TAG, "maxVolume = " + maxVolume);
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View layout = inflater.inflate(R.layout.dialog_volume, null);

		mVolumeButtonInDialog = (ImageView)layout.findViewById(R.id.volumeImageView);
		if (MuteManager.getInstance().isMute()) {
			mVolumeButtonInDialog.setBackgroundResource(R.drawable.volume_off);
		}
		mVolumeButtonInDialog.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleMute();
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
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(layout)
		.setPositiveButton(R.string.loop_ok, null)
		.setTitle(R.string.loop_video_player_volume_setting)
		.create().show();
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

	public void updateVideoInfo() {
		// For current video
		Video video = Playlist.getInstance().getCurrentVideo();
		if (video != null) {
			int minutes = (video.getDuration() / 1000) / 60;
			int seconds = (video.getDuration() / 1000) % 60;
			mDurationView.setText(String.format("0:00 / %d:%02d", minutes, seconds));
			mSeekBar.setMax(video.getDuration());
		} else {
			mDurationView.setText("0:00 / 0:00");
			mSeekBar.setMax(100);
		}

		/*
		if (mService.isPlaying()) {
			mPauseButton.setBackgroundResource(R.drawable.pause);
		} else {
			mPauseButton.setBackgroundResource(R.drawable.play);
		}*/

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
	
	public void handleSeekComplete(int positionMsec) {
		mSeekBar.setProgress(positionMsec / 1000);
		mIsSeeking = false;
	}
}
