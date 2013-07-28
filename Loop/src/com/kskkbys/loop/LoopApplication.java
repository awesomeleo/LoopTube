package com.kskkbys.loop;

import com.kskkbys.loop.model.BlackList;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import android.app.Application;

/**
 * Application class.
 * @author Keisuke Kobayashi
 *
 */
public class LoopApplication extends Application {

	private boolean mIsFirstLaunch;

	@Override
	public void onCreate() {
		super.onCreate();
		//
		mIsFirstLaunch = true;
		// Initialize
		BlackList.getInstance().initialize(getApplicationContext());

		// グローバル設定の生成と初期化を行う
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
		.memoryCache(new LruMemoryCache(2 * 1024 * 1024))
		.memoryCacheSize(2 * 1024 * 1024)
		.build();
		ImageLoader.getInstance().init(config);
	}

	public boolean isFirstLaunch() {
		if (mIsFirstLaunch) {
			mIsFirstLaunch = false;
			return true;
		}
		return false;
	}
}
