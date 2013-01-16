package com.kskkbys.loop;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
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
import android.widget.Toast;

public class VideoPlayerService extends Service {

	private static final String TAG = VideoPlayerService.class.getSimpleName();

	private NotificationManager mNM;

	// Playlist
	private List<Video> mVideoList = new ArrayList<Video>();
	private int mPlayingIndex = 0;

	// MediaPlayer
	private MediaPlayer mMediaPlayer;
	private boolean mIsValidPlayer;

	// VideoPlayerActivity
	private VideoPlayerActivity mPlayerActivity = null;

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
		mIsValidPlayer = false;
		mMediaPlayer = new MediaPlayer();
		mMediaPlayer.setOnErrorListener(new OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				AlertDialog.Builder builder = new AlertDialog.Builder(VideoPlayerService.this);
				builder.setTitle("Error").setMessage("mediaplayer error occurs!").setPositiveButton("OK", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// do nothing
					}
				});

				if (mPlayerActivity != null) {
					mPlayerActivity.onError();
				}

				//return false;	// go to OnComptionListener
				return true;	// dont go to OnCompletion
			}
		});
		mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				mPlayingIndex++;
				startVideo(mPlayingIndex);

				if (mPlayerActivity != null) {
					mPlayerActivity.onCompletion();
				}
			}
		});
		mMediaPlayer.setOnPreparedListener(new OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				if (mPlayerActivity != null) {
					mPlayerActivity.onPrepared();
				}
			}
		});
		mMediaPlayer.setOnSeekCompleteListener(new OnSeekCompleteListener() {
			@Override
			public void onSeekComplete(MediaPlayer mp) {
				if (mPlayerActivity != null) {
					mPlayerActivity.onSeekComplete();
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
		Toast.makeText(this, "VideoPlayerService Stopped", Toast.LENGTH_SHORT).show();
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
		this.mPlayingIndex--;
		startVideo(this.mPlayingIndex);
	}

	public void next() {
		this.mPlayingIndex++;
		startVideo(this.mPlayingIndex);
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

	public int getCurrentPosition() {
		return this.mMediaPlayer.getCurrentPosition();
	}

	/**
	 * Get duration of the playing video
	 * @return
	 */
	public int getDuration() {
		Video video = getCurrentVideo();
		if (video != null) {
			return video.getDuration();
		}
		return 0;
	}

	/**
	 * Get current video instance.
	 * If the index is invalid, it returns null.
	 * @return
	 */
	public Video getCurrentVideo() {
		if (0 <= mPlayingIndex && mPlayingIndex < mVideoList.size()) {
			return mVideoList.get(mPlayingIndex);
		} else {
			return null;
		}
	}

	public List<Video> getVideoList() {
		return mVideoList;
	}

	public int getPlayingIndex() {
		return mPlayingIndex;
	}

	/**
	 * Start to play video with index in playlist
	 * @param index
	 */
	public void startVideo(int index) {
		this.mPlayingIndex = index;
		Video video = getCurrentVideo();
		YouTubePlayTask task = new YouTubePlayTask(video.getId());
		task.execute();
	}

	public void setPlayerActivity(VideoPlayerActivity activity) {
		this.mPlayerActivity = activity;
	}

	/**
	 * Attach SurfaceView instance to MediaPlayer
	 * @param surfaceView
	 */
	public void setSurfaceView(SurfaceView surfaceView) {
		if (surfaceView == null) {
			this.mMediaPlayer.setDisplay(null);
		} else {
			SurfaceHolder holder = surfaceView.getHolder();
			int width = surfaceView.getWidth();
			int height = surfaceView.getWidth() * 9 / 16;	// 16:9
			holder.setFixedSize(width, height);
			this.mMediaPlayer.setDisplay(holder);
		}
	}

	public void setSearchResult(List<Video> result) {
		this.mVideoList = result;
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
			if (mMediaPlayer != null) {
				try {
					// If already playing, reset the MediaPlayer
					mIsValidPlayer = false;
					mMediaPlayer.reset();

					// Start to Play
					mMediaPlayer.setDataSource(result);
					mMediaPlayer.prepare();
					mMediaPlayer.start();
					mIsValidPlayer = true;

					// Show notification
					showNotification(getCurrentVideo().getTitle());

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

			return true;
		}

		@Override
		protected void onPostExecute(Boolean success) {
			//mDialog.dismiss();
			Log.v(TAG, "PlayTask onPostExecute");
			if (!success) {
				if (mPlayerActivity != null) {
					AlertDialog.Builder builder = new AlertDialog.Builder(mPlayerActivity);
					builder.setTitle(R.string.video_player_invalid_video);
					builder.setMessage(R.string.video_player_invalid_video);
					builder.setPositiveButton(R.string.ok, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub

						}
					});
					builder.create().show();
				}
			}
		}

	}
}
