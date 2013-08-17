package com.kskkbys.loop;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.kskkbys.loop.logger.KLog;
import com.kskkbys.loop.storage.SQLiteStorage;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.utils.StorageUtils;

import android.app.Application;
import android.graphics.Bitmap;

/**
 * Application class.
 * @author Keisuke Kobayashi
 *
 */
public class LoopApplication extends Application {
	
	private static final String TAG = LoopApplication.class.getSimpleName();

	private boolean mIsFirstLaunch;

	// Copied from AsyncTask
	private static final int CORE_POOL_SIZE = 5;
	private static final int MAXIMUM_POOL_SIZE = 128;
	private static final int KEEP_ALIVE = 1;
	private static final ThreadFactory sThreadFactory = new ThreadFactory() {
		private final AtomicInteger mCount = new AtomicInteger(1);

		public Thread newThread(Runnable r) {
			return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
		}
	};
	private static final BlockingQueue<Runnable> sPoolWorkQueue =
			new LinkedBlockingQueue<Runnable>(10);

	@Override
	public void onCreate() {
		KLog.v(TAG, "onCreate");
		super.onCreate();
		//
		mIsFirstLaunch = true;

		// Universal Image Loader
		File cacheDir = StorageUtils.getCacheDirectory(this);
		DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
				.resetViewBeforeLoading(true)
				// .cacheInMemory(true)
				.bitmapConfig(Bitmap.Config.RGB_565)
				.imageScaleType(ImageScaleType.IN_SAMPLE_INT)
				.cacheOnDisc(true)
				.displayer(new FadeInBitmapDisplayer(500))
				.build();

		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
				.taskExecutor(new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
				TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory))
				.threadPoolSize(5)
				.threadPriority(Thread.NORM_PRIORITY - 1)
				.denyCacheImageMultipleSizesInMemory()
				.memoryCacheSize(10 * 1024 * 1024)
				.memoryCacheSizePercentage(13) // default
				.discCache(new UnlimitedDiscCache(cacheDir)) // default
				.discCacheFileCount(100)
				.discCacheFileNameGenerator(new HashCodeFileNameGenerator()) // default
				.defaultDisplayImageOptions(defaultOptions)
				.build();

		ImageLoader.getInstance().init(config);
		
		// Restore database
		restoreDatabase();
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
	
	private void restoreDatabase() {
		KLog.v(TAG, "restoreDatabase");
		SQLiteStorage storage = SQLiteStorage.getInstance(this);
		storage.restoreArtists();
		storage.restoreBlackList();
		storage.restoreFavorites();
		KLog.v(TAG, "restoreDatabase done.");
	}
}
