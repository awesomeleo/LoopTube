package com.kskkbys.loop.ui;

import java.util.Timer;
import java.util.TimerTask;

import com.kskkbys.loop.LoopApplication;
import com.kskkbys.loop.R;
import com.kskkbys.loop.logger.FlurryLogger;
import com.kskkbys.loop.logger.KLog;
import com.kskkbys.loop.storage.SQLiteStorage;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Splash screen.
 * This screen should not show action bar.
 * @author Keisuke Kobayashi
 *
 */
public class SplashActivity extends Activity {

	private static final String TAG = SplashActivity.class.getSimpleName();

	// 3 seconds
	private static final long MIN_SPLASH_INTERVAL = 2000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		KLog.v(TAG, "onCreate");

		// If first launch, show splash
		LoopApplication app = (LoopApplication)getApplication();
		if (app.isFirstLaunch()) {
			FlurryLogger.logEvent(FlurryLogger.SEE_SPLASH);
			setContentView(R.layout.activity_splash);

			// Custom font
			TextView title = (TextView)findViewById(R.id.splash_title);
			title.setTypeface(Typeface.createFromAsset(getAssets(), "GeosansLight.ttf"));

			TextView desc = (TextView)findViewById(R.id.splash_description);
			desc.setTypeface(Typeface.createFromAsset(getAssets(), "GeosansLight-Oblique.ttf"));

			// Start to restore history.
			restoreHistory();
		} else {
			startMainActivity();
		}
	}

	private void startMainActivity() {
		startActivity(new Intent(SplashActivity.this, MainActivity.class));
		finish();
	}

	private void restoreHistory() {
		LoopApplication app = (LoopApplication)getApplication();
		SQLiteStorage storage = app.getSQLiteStorage();
		LoadHistoryTask task = new LoadHistoryTask();
		task.execute(storage);
	}

	/**
	 * 
	 * @author Keisuke Kobayashi
	 *
	 */
	private class LoadHistoryTask extends AsyncTask<SQLiteStorage, Integer, Boolean> {

		private long mStartTime;

		@Override
		protected void onPreExecute() {
			mStartTime = System.currentTimeMillis();
		}

		@Override
		protected Boolean doInBackground(SQLiteStorage... params) {
			SQLiteStorage storage = params[0];
			storage.restoreArtists();
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			long endTime = System.currentTimeMillis();
			long spent = endTime - mStartTime;
			KLog.v(TAG, "Loading history takes " + spent + " milliseconds.");
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					startMainActivity();
				}
			}, Math.max(0L, MIN_SPLASH_INTERVAL - spent));
		}
	}
}
