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

import com.kskkbys.loop.playlist.Playlist;

import android.app.AlertDialog;
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
import android.view.SurfaceView;

/**
 * Video player service
 */
public class VideoPlayerService extends Service {

	private static final String TAG = VideoPlayerService.class.getSimpleName();

	private NotificationManager mNM;

	// MediaPlayer
	private MediaPlayer mMediaPlayer;

	// VideoPlayerActivity
	private MediaPlayerCallback mListener;

	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	private int NOTIFICATION_ID = R.string.video_player_service_started;

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
				// Service starts to play next video
				Playlist.getInstance().next();
				startVideo();
				if (mListener != null) {
					mListener.onCompletion();
				}
			}
		});
		mMediaPlayer.setOnPreparedListener(new OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				// When prepared, start to play
				showNotification(Playlist.getInstance().getCurrentVideo().getTitle());
				play();
				if (mListener != null) {
					mListener.onPrepared();
				}
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
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, VideoPlayerActivity.class), 0);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.ic_launcher, text,
				System.currentTimeMillis());

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.app_name), text, contentIntent);

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
		Playlist.getInstance().prev();
		startVideo();
	}

	public void next() {
		Playlist.getInstance().next();
		startVideo();
	}

	public void play() {
		this.mMediaPlayer.start();
	}
	public void pause() {
		this.mMediaPlayer.pause();
	}

	public boolean isPlaying() {
		return this.mMediaPlayer.isPlaying();
	}

	public void seekTo(int msec) {
		this.mMediaPlayer.seekTo(msec);
	}

	/**
	 * Get the current position in the playing video
	 * @return
	 */
	public int getCurrentPosition() {
		return this.mMediaPlayer.getCurrentPosition();
	}

	/**
	 * Start to play video
	 */
	public void startVideo() {
		Video video = Playlist.getInstance().getCurrentVideo();
		YouTubePlayTask task = new YouTubePlayTask(video.getId());
		task.execute();
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
	public void setSurfaceView(SurfaceView surfaceView) {
		Log.v(TAG, "setSurfaceView");
		if (surfaceView == null) {
			this.mMediaPlayer.setDisplay(null);
		} else {
			SurfaceHolder holder = surfaceView.getHolder();
			if (holder.isCreating()) {
				Log.w(TAG, "surafce view is creating");
			} else {
				int width = surfaceView.getWidth();
				int height = surfaceView.getWidth() * 9 / 16;	// 16:9
				holder.setFixedSize(width, height);
				this.mMediaPlayer.setDisplay(holder);
			}
		}
	}

	/**
	 * Set video URL. After player is prepared, please call play method.
	 * @param url
	 */
	private void setVideoUrl(String url) {
		if (mMediaPlayer != null) {
			try {
				// If already playing, reset the MediaPlayer
				mMediaPlayer.reset();

				// Set URL and prepare
				mMediaPlayer.setDataSource(url);
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
			//mDialog.setMessage("Downloading...");
			//mDialog.show();
		}

		@Override
		protected Boolean doInBackground(String... arg0) {
			int begin, end;
			String tmpstr = null;
			try {
				DefaultHttpClient client = new DefaultHttpClient();
				HttpGet request = new HttpGet("http://www.youtube.com/watch?v=" + this.mVideoId);
				request.setHeader("User-Agent", "Mozilla/5.0 (iPad; CPU OS 5_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko ) Version/5.1 Mobile/9B176 Safari/7534.48.3");
				HttpResponse response = client.execute(request);

				InputStream stream = response.getEntity().getContent();
				InputStreamReader reader=new InputStreamReader(stream);
				StringBuffer buffer=new StringBuffer();
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
			//mDialog.dismiss();
			Log.v(TAG, "PlayTask onPostExecute");
			if (!success) {
				if (mListener != null) {

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
	}
}
