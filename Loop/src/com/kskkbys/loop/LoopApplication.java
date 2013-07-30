package com.kskkbys.loop;


import java.io.File;
import java.util.concurrent.ThreadPoolExecutor;

import com.kskkbys.loop.model.BlackList;
import com.kskkbys.loop.storage.ArtistStorage;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.utils.StorageUtils;

import android.app.Application;
import android.os.AsyncTask;
import android.support.v4.content.AsyncTaskLoader;

/**
 * Application class.
 * @author Keisuke Kobayashi
 *
 */
public class LoopApplication extends Application {

	private boolean mIsFirstLaunch;
	private ArtistStorage mArtistStorage;

	@Override
	public void onCreate() {
		super.onCreate();
		//
		mIsFirstLaunch = true;
		// Initialize
		BlackList.getInstance().initialize(getApplicationContext());

		// Universal Image Loader
		File cacheDir = StorageUtils.getCacheDirectory(this);
		DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
		.cacheInMemory(true)
		.cacheOnDisc(true)
		.displayer(new FadeInBitmapDisplayer(500))
		.build();
		
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
		.taskExecutor(AsyncTask.THREAD_POOL_EXECUTOR)	//TODO
		.threadPoolSize(3)
		.threadPriority(Thread.NORM_PRIORITY - 1)
		.memoryCache(new LruMemoryCache(2 * 1024 * 1024))
		.memoryCacheSize(2 * 1024 * 1024)
		.memoryCacheSizePercentage(13) // default
		.discCache(new UnlimitedDiscCache(cacheDir)) // default
		.discCacheSize(50 * 1024 * 1024)
		.discCacheFileCount(100)
		.discCacheFileNameGenerator(new HashCodeFileNameGenerator()) // default
		.defaultDisplayImageOptions(defaultOptions)
		.build();
		
		ImageLoader.getInstance().init(config);
		
		// Initialize storage instance which will be accessed by several screens.
		mArtistStorage = new ArtistStorage(this);
	}

	/**
	 * Check whether this is first launch in current process or not.
	 * @return
	 */
	public boolean isFirstLaunch() {
		if (mIsFirstLaunch) {
			mIsFirstLaunch = false;
			return true;
		}
		return false;
	}
	
	public ArtistStorage getArtistStorage() {
		return mArtistStorage;
	}
}
