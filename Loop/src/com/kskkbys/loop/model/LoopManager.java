package com.kskkbys.loop.model;

/**
 * Looping state manager
 *
 */
public class LoopManager {

	private static final LoopManager mInstance = new LoopManager();
	
	private boolean mIsLooping;
	
	private LoopManager() {
		mIsLooping = false;
	}
	
	public static LoopManager getInstance() {
		return mInstance;
	}
	
	/**
	 * Check whether player is looping or not
	 * @return
	 */
	public boolean isLooping() {
		return mIsLooping;
	}
	
	/**
	 * Save new looping state
	 * @param isLooping
	 */
	public void setLoop(boolean isLooping) {
		mIsLooping = isLooping;
	}
}
