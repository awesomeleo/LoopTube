package com.kskkbys.loop;

//import android.util.Log;

public class Video {
	
	//private static final String TAG = Video.class.getSimpleName();

	private String mId;
	private String mTitle;
	private int mDuration;

	public Video(String id, String title, int duration) {
		this.mId = id;
		this.mTitle = title;
		this.mDuration = duration;
	}
	
	public String getId() {
		return mId;
	}

	public String getTitle() {
		return mTitle;
	}
	
	public int getDuration() {
		return mDuration;
	}
}
