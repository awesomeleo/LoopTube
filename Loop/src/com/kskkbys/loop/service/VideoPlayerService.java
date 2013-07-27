package com.kskkbys.loop.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import com.kskkbys.loop.logger.FlurryLogger;
import com.kskkbys.loop.logger.KLog;
import com.kskkbys.loop.model.BlackList;
import com.kskkbys.loop.model.Playlist;
import com.kskkbys.loop.model.Video;
import com.kskkbys.loop.notification.NotificationManager;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
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

	// Command
	public static final String COMMAND = "command";
	public static final int COMMAND_UNKNOWN = 0;
	public static final int COMMAND_PLAY = 1;
	public static final int COMMAND_PAUSE = 2;
	public static final int COMMAND_NEXT = 3;
	public static final int COMMAND_PREV = 4;
	public static final int COMMAND_LOOPING = 5;
	public static final int COMMAND_SEEK = 6;

	public static final String PLAY_RELOAD = "play_reload";
	public static final String LOOPING = "looping";
	public static final String SEEK_MSEC = "seek_msec";

	// MediaPlayer
	private static MediaPlayer mMediaPlayer;
	private int mState;
	private boolean mIsLooping;

	// for Flurry
	private boolean mIsPlaying;

	/**
	 * Class to access to this service
	 */
	public class VideoPlayerServiceBinder extends Binder {
		public VideoPlayerService getService() {
			return VideoPlayerService.this;
		}
	}

	private final IBinder mBinder = new VideoPlayerServiceBinder();

	@Override
	public void onCreate() {
		KLog.v(TAG, "onCreate");

		FlurryLogger.onStartSession(VideoPlayerService.this);

		// state of MediaPlayer
		mState = STATE_INIT;
		mIsLooping = false;

		// for flurry
		mIsPlaying = false;

		// Initialize MediaPlayer
		mMediaPlayer = new MediaPlayer();
		mMediaPlayer.setOnErrorListener(new OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				KLog.e(TAG, "onError");
				KLog.e(TAG, "what = " + what);
				KLog.e(TAG, "extra = " + extra);
				Intent intent = new Intent();
				intent.setAction(PlayerEvent.Error.getAction());
				intent.setPackage(getPackageName());
				sendBroadcast(intent);
				return true;
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
					NotificationManager.cancel(VideoPlayerService.this);
				}
				// Notify listeners
				Intent intent = new Intent(PlayerEvent.Complete.getAction());
				intent.setPackage(getPackageName());
				sendBroadcast(intent);
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
				Intent intent = new Intent(PlayerEvent.Prepared.getAction());
				intent.setPackage(getPackageName());
				sendBroadcast(intent);
			}
		});
		mMediaPlayer.setOnSeekCompleteListener(new OnSeekCompleteListener() {
			@Override
			public void onSeekComplete(MediaPlayer mp) {
				Intent intent = new Intent(PlayerEvent.SeekComplete.getAction());
				intent.putExtra("msec", mp.getCurrentPosition());
				intent.setPackage(getPackageName());
				sendBroadcast(intent);
			}
		});
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		KLog.i(TAG, "Received start id " + startId + ": " + intent);

		if (intent != null) {
			int command = intent.getIntExtra(COMMAND, COMMAND_UNKNOWN);
			boolean isReload = intent.getBooleanExtra(PLAY_RELOAD, false);
			boolean isLooping = intent.getBooleanExtra(LOOPING, false);
			int msec = intent.getIntExtra(SEEK_MSEC, 0);
			KLog.v(TAG, "command = " + command);
			switch (command) {
			case COMMAND_PLAY:
				play(isReload);
				break;
			case COMMAND_PAUSE:
				pause();
				break;
			case COMMAND_PREV:
				prev();
				break;
			case COMMAND_NEXT:
				next();
				break;
			case COMMAND_LOOPING:
				setLooping(isLooping);
				break;
			case COMMAND_SEEK:
				seekTo(msec);
				break;
			default:
				KLog.e(TAG, "Unknown command!");
				break;
			}
		}
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		KLog.v(TAG, "onDestroy");

		FlurryLogger.onEndSession(VideoPlayerService.this);

		// Cancel the persistent notification.
		com.kskkbys.loop.notification.NotificationManager.cancel(this);

		// Stop mediaplayer
		if (mMediaPlayer != null) {
			mMediaPlayer.release();
		}

		// If playing, send playing time to flurry
		if (mIsPlaying) {
			mIsPlaying = false;
			FlurryLogger.endTimedEvent(FlurryLogger.PLAY_VIDEO);
		}

		// Tell the user we stopped.
		KLog.v(TAG, "VideoPlayerService stopped.");
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
		NotificationManager.show(this, videoTitle);
	}

	private void prev() {
		KLog.v(TAG, "prev");
		Playlist.getInstance().prev();
		startVideo();
	}

	private void next() {
		KLog.v(TAG,"next");
		Playlist.getInstance().next();
		startVideo();
	}

	/**
	 * Play command
	 * @param reset		If true, reload video. Otherwise, only start.
	 */
	private void play(boolean reset) {
		if (reset) {
			startVideo();
		} else {
			play();
		}
	}

	/**
	 * This method starts MediaPlayer only.
	 * After prepared, this method starts to play and changes its state to STATE_PLAYING.
	 */
	private void play() {
		KLog.v(TAG, "play");
		if (mState == STATE_PEPARED || mState == STATE_PLAYING) {
			mState = STATE_PLAYING;
			mMediaPlayer.start();
			// show notification
			showNotification(Playlist.getInstance().getCurrentVideo().getTitle());
			// logger
			if (!mIsPlaying) {
				mIsPlaying = true;
				Map<String, String> param = new HashMap<String, String>();
				param.put("query", Playlist.getInstance().getQuery());
				FlurryLogger.logEvent(FlurryLogger.PLAY_VIDEO, param, true);
			}
		} else {
			KLog.w(TAG, "Invalid state: " + mState);
		}
	}
	private void pause() {
		KLog.v(TAG, "pause");
		if (mState == STATE_PLAYING) {
			mMediaPlayer.pause();
			NotificationManager.cancel(this);
			if (mIsPlaying) {
				mIsPlaying = false;
				FlurryLogger.endTimedEvent(FlurryLogger.PLAY_VIDEO);
			}
		} else {
			KLog.w(TAG, "Invalid state: " + mState);
		}
	}

	private boolean isPlaying() {
		return mMediaPlayer.isPlaying();
	}

	private void seekTo(int msec) {
		KLog.v(TAG, "seekTo");
		if (mState == STATE_PLAYING) {
			mMediaPlayer.seekTo(msec);
		}
	}

	private void setLooping(boolean isRepeat) {
		KLog.v(TAG, "setRepeat");
		mIsLooping = isRepeat;
		mMediaPlayer.setLooping(isRepeat);
	}

	/**
	 * Get the current position in the playing video
	 * @return
	 */
	private int getCurrentPosition() {
		if (mState == STATE_PLAYING) {
			return mMediaPlayer.getCurrentPosition();
		} else {
			return 0;
		}
	}

	/**
	 * Start to play video
	 */
	private void startVideo() {
		KLog.v(TAG, "startVideo");
		// Reset the current player
		mMediaPlayer.reset();
		mState = STATE_INIT;
		// Search and start to play
		Video video = Playlist.getInstance().getCurrentVideo();
		if (video != null) {
			// check blacklist
			BlackList bl = BlackList.getInstance();
			if (bl.containsByUser(video.getId()) || bl.containsByApp(video.getId())) {
				KLog.v(TAG, "This video is registered in black list. Skip this video.");
				next();
			} else {
				YouTubePlayTask task = new YouTubePlayTask(video.getId());
				task.execute();
			}
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
	 * Attach SurfaceView instance to MediaPlayer
	 * @param surfaceView
	 */
	public static void setSurfaceHolder(SurfaceHolder holder) {
		KLog.v(TAG, "setSurfaceView");
		mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mMediaPlayer.setDisplay(holder);
	}

	/**
	 * Set video URL. After player is prepared, please call play method.
	 * @param url
	 */
	private void setVideoUrl(String url) {
		KLog.v(TAG, "setVideoUrl");
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
			KLog.e(TAG, "MediaPlayer is null!");
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
			KLog.v(TAG, "onPreExecute");
			Intent intent = new Intent(PlayerEvent.StartToLoad.getAction());
			sendBroadcast(intent);
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
				KLog.e(TAG, "YouTubePlayTask error", e);
				return false;
			} catch (IOException e) {
				KLog.e(TAG, "YouTubePlayTask error", e);
				return false;
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
						KLog.v(TAG, "fmt = " + fmt);
						KLog.v(TAG, "tmpstr = " + tmpstr);

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
			KLog.v(TAG, "video url = " + result);
			if (result == null || result.length() == 0) {
				KLog.v(TAG, "Invalid video.");
				// Add this video to black list 
				BlackList.getInstance().addAppBlackList(mVideoId);
				return false;
			}
			setVideoUrl(result);

			return true;
		}

		@Override
		protected void onPostExecute(Boolean success) {
			KLog.v(TAG, "onPostExecute");
			KLog.v(TAG, "PlayTask onPostExecute");

			Intent endIntent = new Intent(PlayerEvent.EndToLoad.getAction());
			sendBroadcast(endIntent);

			if (!success) {
				Intent invalidIntent = new Intent(PlayerEvent.InvalidVideo.getAction());
				sendBroadcast(invalidIntent);
			}
		}

	}

	/**
	 * Player events.
	 * @author Keisuke Kobayashi
	 *
	 */
	public enum PlayerEvent {
		/**
		 * Unhandled error
		 */
		Error,
		/**
		 * Found a video which can not be played on Android
		 */
		InvalidVideo,
		/**
		 * Completed to play
		 */
		Complete,
		/**
		 * Prepared video
		 */
		Prepared,
		/**
		 * Completed to seek
		 */
		SeekComplete,
		/**
		 * Start to load video
		 */
		StartToLoad,
		/**
		 * End to load video
		 */
		EndToLoad,
		/**
		 * General update of player state
		 */
		Update;

		/**
		 * Get action for intent.
		 * @param context
		 * @return
		 */
		public String getAction() {
			return "com.kskkbys.loop." + name();
		}
	}
}
