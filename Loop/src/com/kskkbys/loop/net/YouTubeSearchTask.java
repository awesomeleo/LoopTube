package com.kskkbys.loop.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.kskkbys.loop.R;
import com.kskkbys.loop.logger.KLog;
import com.kskkbys.loop.model.BlackList;
import com.kskkbys.loop.model.Video;
import com.kskkbys.loop.ui.MainActivity;

import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;

/**
 * Asynchronous task to search youtube videos
 * @author Keisuke Kobayashi
 */
public class YouTubeSearchTask extends AsyncTask<String, Integer, String> {

	private static final String TAG = YouTubeSearchTask.class.getSimpleName();

	private static final int CONNECT_TIMEOUT = 30 * 1000;
	private static final int READ_TIMEOUT = 30 * 1000;

	private MainActivity mParent;
	private YouTubeSearchResult mResult;
	private String mQuery;

	public YouTubeSearchTask(MainActivity parent) {
		this.mParent = parent;
	}

	@Override
	protected String doInBackground(String... query) {
		KLog.v(TAG, "doInBackground");
		mQuery = query[0];
		
		String uri = null;
		try {
			uri = "http://gdata.youtube.com/feeds/api/videos?q="
					+ URLEncoder.encode(query[0], "UTF-8") + "&alt=json&max-results=50";
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			return null;
		}
		disableConnectionReuseIfNecessary();
		HttpURLConnection connection = null;
		try {
			URL url = new URL(uri);
			connection = (HttpURLConnection)url.openConnection();
			connection.setConnectTimeout(CONNECT_TIMEOUT);
			connection.setReadTimeout(READ_TIMEOUT);
			connection.setDefaultUseCaches(false);

			InputStreamReader isr = new InputStreamReader(connection.getInputStream());
			BufferedReader br = new BufferedReader(isr);
			String response = "";
			String line;
			while ((line = br.readLine()) != null) {
				response += line;
			}
			br.close();
			isr.close();
			// Convert JSON to gson
			Gson gson = new Gson();
			this.mResult = gson.fromJson(response, YouTubeSearchResult.class);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		return null;
	}

	@Override
	protected void onPreExecute() {
		KLog.v(TAG, "onPreExecute");
		if (mParent != null) {
			mParent.showProgress(R.string.loop_main_dialog_searching);
		}
	}

	@Override
	protected void onPostExecute(String result) {
		KLog.v(TAG, "onPostExecute");
		// Dismiss dialog and start to play
		mParent.dismissProgress();
		List<Video> videos = createVideoList(this.mResult);
		if (videos != null && videos.size() > 0) {
			this.mParent.startVideoPlayer(mQuery, videos);
		} else {
			KLog.v(TAG, "Video list is empty. Network state may be bad.");
			//SimpleErrorDialog.show(mParent, R.string.loop_main_error_no_video);
			mParent.showAlert(R.string.loop_main_error_no_video, null);
		}
		
		// Save image URL in search history db
		mParent.updateHistory(mQuery, videos);
		// Update history list view
		mParent.updateHistoryUI();
	}

	/**
	 * Create list of Video objects
	 * @param result
	 * @return
	 */
	private List<Video> createVideoList(YouTubeSearchResult result) {
		List<Video> videoList = new ArrayList<Video>();
		if (result != null && result.feed != null && result.feed.entry != null) {
			for (YouTubeSearchResult.Feed.Entry entry : result.feed.entry) {
				try {
					//Create video
					Video v = new Video(
							getVideoId(entry.id.$t),
							entry.title.$t,
							Integer.parseInt(entry.media$group.yt$duration.seconds) * 1000);
					//entry.media$group.media$content[0].duration * 1000);
					if (entry.media$group.media$description != null) {
						v.setDescription(entry.media$group.media$description.$t);
					}
					if (entry.media$group.media$player != null && entry.media$group.media$player.length > 0) {
						v.setVideoUrl(entry.media$group.media$player[0].url);
					}
					if (entry.media$group.media$thumbnail != null) {
						// Use small thumbnail which MUST have field
						for (YouTubeSearchResult.Feed.Entry.Media$Group.Media$Thumbnail thumbnail: entry.media$group.media$thumbnail) {
							if (!TextUtils.isEmpty(thumbnail.time)) {
								v.setThumbnailUrl(thumbnail.url);
								break;
							}
						}
					}
					videoList.add(v);
					KLog.v(TAG, "Video added: " + v.toString());
					// Check brack words
					BlackList blackList = BlackList.getInstance();
					if (blackList.isBlackTitle(v.getTitle())) {
						KLog.v(TAG, "This video contains black word: " + v.getTitle());
						blackList.addAppBlackList(v.getId());	// TODO App? User?
					}
				} catch (NumberFormatException e) {
					KLog.e(TAG, "error", e);
				} catch (NullPointerException e) {
					KLog.e(TAG, "error", e);
				}
			}
		}
		return videoList;
	}

	/**
	 * Get video ID
	 * @param str
	 * @return
	 */
	private String getVideoId(String str) {
		return str.replaceAll("http://gdata.youtube.com/feeds/api/videos/", "");
	}

	/**
	 * For 2.2
	 */
	private void disableConnectionReuseIfNecessary() {
		// Work around pre-Froyo bugs in HTTP connection reuse.
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
			System.setProperty("http.keepAlive", "false");
		}
	}
}
