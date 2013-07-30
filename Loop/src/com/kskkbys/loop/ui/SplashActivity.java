package com.kskkbys.loop.ui;

import com.kskkbys.loop.LoopApplication;
import com.kskkbys.loop.R;
import com.kskkbys.loop.logger.FlurryLogger;
import com.kskkbys.loop.logger.KLog;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

/**
 * Splash screen.
 * This screen should not show action bar.
 * @author Keisuke Kobayashi
 *
 */
public class SplashActivity extends Activity {

	private static final String TAG = SplashActivity.class.getSimpleName();

	private static final long SPLASH_INTERVAL = 3000;
	
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
			
			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					startMainActivity();
				}
			}, SPLASH_INTERVAL);
		} else {
			startMainActivity();
		}
	}
	
	private void startMainActivity() {
		startActivity(new Intent(SplashActivity.this, MainActivity.class));
		finish();
	}
}
