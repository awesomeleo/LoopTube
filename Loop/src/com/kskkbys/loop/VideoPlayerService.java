package com.kskkbys.loop;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import com.kskkbys.loop.playlist.Playlist;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * Video player service
 */
public class VideoPlayerService extends Service {

	private static final String TAG = VideoPlayerService.class.getSimpleName();
	public static final int STATE_INIT = 0;
	public static final int STATE_PEPARED = 1;
	public static final int STATE_PLAYING = 2;
	public static final int STATE_COMPLETE = 3;

	private NotificationManager mNM;

	// MediaPlayer
	private static MediaPlayer mMediaPlayer;
	private int mState;
	private boolean mIsLooping;

	// VideoPlayerActivity
	private MediaPlayerCallback mListener;

	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	private int NOTIFICATION_ID = R.string.loop_video_player_service_started;

	/**
	 * Class to access to this service
	 */
	public class VideoPlayerServiceBinder extends Binder {
		VideoPlayerService getService() {
			return VideoPlayerService.this;
		}
	}

	private final IBinder mBinder = new VideoPlayerServiceBinder();

	@Override
	public void onCreate() {
		Log.v(TAG, "onCreate");
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

		// state of MediaPlayer
		mState = STATE_INIT;
		mIsLooping = false;

		// Initialize MediaPlayer
		mMediaPlayer = new MediaPlayer();
		mMediaPlayer.setOnErrorListener(new OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				if (mListener != null) {
					mListener.onError();
				}
				//return false;	// go to OnComptionListener
				return true;	// dont go to OnCompletion
			}
		});
		mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				//
				mState = STATE_COMPLETE;
				// Service starts to play next video
				Playlist.getInstance().next();
				if (Playlist.getInstance().getCurrentVideo() != null) {
					startVideo();
				} else {
					// End of playlist
					mNM.cancel(NOTIFICATION_ID);
				}
				// Notify listeners
				if (mListener != null) {
					mListener.onCompletion();
				}
			}
		});
		mMediaPlayer.setOnPreparedListener(new OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				// When prepared, start to play video
				mState = STATE_PEPARED;
				mp.setLooping(mIsLooping);	//set loop setting
				play();
				// Notify
				if (mListener != null) {
					mListener.onPrepared();
				}
				// show notification
				showNotification(Playlist.getInstance().getCurrentVideo().getTitle());
			}
		});
		mMediaPlayer.setOnSeekCompleteListener(new OnSeekCompleteListener() {
			@Override
			public void onSeekComplete(MediaPlayer mp) {
				if (mListener != null) {
					mListener.onSeekComplete(mp.getCurrentPosition());
				}
			}
		});
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("LocalService", "Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "onDestroy");
		// Cancel the persistent notification.
		mNM.cancel(NOTIFICATION_ID);

		// Stop mediaplayer
		if (mMediaPlayer != null) {
			mMediaPlayer.release();
		}

		// Tell the user we stopped.
		Log.v(TAG, "VideoPlayerService stopped.");
		// Toast.makeText(this, "VideoPlayerService Stopped", Toast.LENGTH_SHORT).show();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return this.mBinder;
	}

	/**
	 * Show a notification in status bar
	 * @param videoTitle
	 */
	private void showNotification(String videoTitle) {
		// In this sample, we'll use the same text for the ticker and the expanded notification
		CharSequence text = "Now Playing: " + videoTitle;

		// The PendingIntent to launch our activity if the user selects this notification
		Intent intent = new Intent(this, MainActivity.class);
		intent.putExtra("from_notification", true);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				intent, 0);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.ic_launcher, text,
				System.currentTimeMillis());

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.loop_app_name), text, contentIntent);

		/*
		Notification notification = new Notification.Builder(this).setContentIntent(contentIntent)
				.setSmallIcon(R.drawable.ic_launcher)
				.setTicker("tickerText").setContentTitle("contentTitle")
				.setContentText("contentText").setWhen(System.currentTimeMillis())
				.build();
		 */

		// Send the notification.
		mNM.notify(NOTIFICATION_ID, notification);
	}

	public void prev() {
		Log.v(TAG, "prev");
		Playlist.getInstance().prev();
		startVideo();
	}

	public void next() {
		Log.v(TAG,"next");
		Playlist.getInstance().next();
		startVideo();
	}

	/**
	 * After prepared, this method starts to play and changes its state to STATE_PLAYING.
	 */
	public void play() {
		Log.v(TAG, "play");
		if (mState == STATE_PEPARED || mState == STATE_PLAYING) {
			mState = STATE_PLAYING;
			mMediaPlayer.start();
		} else {
			Log.w(TAG, "Invalid state: " + mState);
		}
	}
	public void pause() {
		Log.v(TAG, "pause");
		if (mState == STATE_PLAYING) {
			mMediaPlayer.pause();
		} else {
			Log.w(TAG, "Invalid state: " + mState);
		}
	}

	public boolean isPlaying() {
		Log.v(TAG, "isPlaying");
		return mMediaPlayer.isPlaying();
	}

	public void seekTo(int msec) {
		Log.v(TAG, "seekTo");
		if (mState == STATE_PLAYING) {
			mMediaPlayer.seekTo(msec);
		}
	}

	public void setLooping(boolean isRepeat) {
		Log.v(TAG, "setRepeat");
		mIsLooping = isRepeat;
		mMediaPlayer.setLooping(isRepeat);
	}

	public boolean isLooping() {
		return mMediaPlayer.isLooping();
	}

	/**
	 * Get the current position in the playing video
	 * @return
	 */
	public int getCurrentPosition() {
		//Log.v(TAG, "getCurrentPosition");	// called in 1sec
		if (mState == STATE_PLAYING) {
			return mMediaPlayer.getCurrentPosition();
		} else {
			return 0;
		}
	}

	/**
	 * Start to play video
	 */
	public void startVideo() {
		Log.v(TAG, "startVideo");
		// Reset the current player
		mMediaPlayer.reset();
		mState = STATE_INIT;
		// Search and start to play
		Video video = Playlist.getInstance().getCurrentVideo();
		if (video != null) {
			YouTubePlayTask task = new YouTubePlayTask(video.getId());
			task.execute();
		}
	}
	
	/**
	 * Get the media player state
	 * @return
	 */
	public int getState() {
		return this.mState;
	}

	/**
	 * Set the lister for media player events of this service
	 * @param listener
	 */
	public void setListener(MediaPlayerCallback listener) {
		this.mListener = listener;
	}

	/**
	 * Attach SurfaceView instance to MediaPlayer
	 * @param surfaceView
	 */
	public static void setSurfaceHolder(SurfaceHolder holder) {
		Log.v(TAG, "setSurfaceView");
		mMediaPlayer.setDisplay(holder);
	}

	/**
	 * Set video URL. After player is prepared, please call play method.
	 * @param url
	 */
	private void setVideoUrl(String url) {
		Log.v(TAG, "setVideoUrl");
		if (mMediaPlayer != null) {
			try {
				// If already playing, reset the MediaPlayer
				mMediaPlayer.reset();

				// Set URL and prepare
				mMediaPlayer.setDataSource(url);
				//mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mMediaPlayer.prepare();

			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			Log.e(TAG, "MediaPlayer is null!");
		}
	}

	/**
	 * Inner class to play YouTube video with MediaPlayer
	 */
	private class YouTubePlayTask extends AsyncTask<String, Void, Boolean> {

		//private ProgressDialog mDialog = new ProgressDialog(VideoPlayerService.this);

		private String mVideoId;

		/**
		 * Constructor
		 */
		public YouTubePlayTask(String videoId) {
			this.mVideoId = videoId;
		}

		@Override
		protected void onPreExecute() {
			Log.v(TAG, "onPreExecute");
			if (mListener != null) {
				mListener.onStartLoadVideo();
			}
		}

		@Override
		protected Boolean doInBackground(String... arg0) {
			int begin, end;
			String tmpstr = null;
			try {
				DefaultHttpClient client = new DefaultHttpClient();
				HttpParams params = client.getParams();
				HttpConnectionParams.setConnectionTimeout(params, 5000);
				HttpConnectionParams.setSoTimeout(params, 5000);
				HttpGet request = new HttpGet("http://www.youtube.com/watch?v=" + this.mVideoId);
				request.setHeader("User-Agent", "Mozilla/5.0 (iPad; CPU OS 5_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko ) Version/5.1 Mobile/9B176 Safari/7534.48.3");
				HttpResponse response = client.execute(request);

				InputStream stream = response.getEntity().getContent();
				InputStreamReader reader = new InputStreamReader(stream);
				StringBuffer buffer = new StringBuffer();
				char[] buf=new char[262144];
				int chars_read;
				while ((chars_read = reader.read(buf, 0, 262144)) != -1) {
					buffer.append(buf, 0, chars_read);
				}
				tmpstr=buffer.toString();

				begin  = tmpstr.indexOf("url_encoded_fmt_stream_map=");
				end = tmpstr.indexOf("&", begin + 27);
				if (end == -1) {
					end = tmpstr.indexOf("\"", begin + 27);
				}
				tmpstr = URLDecoder.decode(tmpstr.substring(begin + 27, end), "utf-8");

				reader.close();
				client.getConnectionManager().shutdown();

			} catch (MalformedURLException e) {
				throw new RuntimeException();
			} catch (IOException e) {
				throw new RuntimeException();
			}

			Vector<String> url_encoded_fmt_stream_map = new Vector<String>();
			begin = 0;
			end   = tmpstr.indexOf(",");

			while (end != -1) {
				url_encoded_fmt_stream_map.add(tmpstr.substring(begin, end));
				begin = end + 1;
				end   = tmpstr.indexOf(",", begin);
			}

			url_encoded_fmt_stream_map.add(tmpstr.substring(begin, tmpstr.length()));
			String result = "";
			Enumeration<String> url_encoded_fmt_stream_map_enum = url_encoded_fmt_stream_map.elements();
			while (url_encoded_fmt_stream_map_enum.hasMoreElements()) {
				tmpstr = (String)url_encoded_fmt_stream_map_enum.nextElement();
				begin = tmpstr.indexOf("itag=");
				if (begin != -1) {
					end = tmpstr.indexOf("&", begin + 5);

					if (end == -1) {
						end = tmpstr.length();
					}

					try {
						int fmt = Integer.parseInt(tmpstr.substring(begin + 5, end));
						Log.v(TAG, "fmt = " + fmt);
						Log.v(TAG, "tmpstr = " + tmpstr);

						if (fmt == 18 /*35*/) {
							begin = tmpstr.indexOf("url=");
							if (begin != -1) {
								end = tmpstr.indexOf("&", begin + 4);
								if (end == -1) {
									end = tmpstr.length();
								}
								try {
									result = URLDecoder.decode(tmpstr.substring(begin + 4, end), "utf-8");
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
								}
								break;
							}
						}
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}

			// PLAY VIDEO
			Log.v(TAG, "video url = " + result);
			if (result == null || result.length() == 0) {
				Log.v(TAG, "Invalid video.");
				return false;
			}
			setVideoUrl(result);

			return true;
		}

		@Override
		protected void onPostExecute(Boolean success) {
			Log.v(TAG, "onPostExecute");
			Log.v(TAG, "PlayTask onPostExecute");
			if (mListener != null) {
				mListener.onEndLoadVideo();
				if (!success) {
					mListener.onInvalidVideoError();
				}
			}
		}

	}

	/**
	 * Listener for the MediaPlayer events
	 * Video player which needs media player events must implement this interface
	 */
	public interface MediaPlayerCallback {
		/**
		 * On error
		 */
		public void onError();
		/**
		 * On completion 
		 */
		public void onCompletion();
		/**
		 * On prepared
		 */
		public void onPrepared();
		/**
		 * On seek complete
		 * @param new position in msec
		 */
		public void onSeekComplete(int positionMsec);

		/**
		 * Invoked when the YouTube video can not be played (does not have valid URL)
		 */
		public void onInvalidVideoError();

		/**
		 * Invoked when start to load video
		 */
		public void onStartLoadVideo();

		/**
		 * Invoked when end to load video
		 */
		public void onEndLoadVideo();
	}
}
