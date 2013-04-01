package com.kskkbys.loop.audio;

import android.content.Context;
import android.media.AudioManager;

/**
 * Manager to set/unset mute
 * @author keisuke.kobayashi
 *
 */
public class MuteManager {

	private static final MuteManager instance = new MuteManager();
	
	private boolean mIsMute;
	
	/**
	 * Constructor
	 */
	private MuteManager() {
		mIsMute = false;
	}
	
	/**
	 * get instance
	 * @return
	 */
	public static MuteManager getInstance() {
		return instance;
	}
	
	/**
	 * Set mute on/off
	 * @param context
	 * @param isMute
	 */
	public void setMute(Context context, boolean isMute) {
		AudioManager am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		am.setStreamMute(AudioManager.STREAM_MUSIC, isMute);
		mIsMute = isMute;
	}
	
	public boolean isMute() {
		return mIsMute;
	}
	
}
