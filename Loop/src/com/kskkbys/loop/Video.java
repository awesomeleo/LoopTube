package com.kskkbys.loop;

public class Video {
	
	//private static final String TAG = Video.class.getSimpleName();

	private String mId;
	private String mTitle;
	private int mDuration;
	private String mDescription;
	private String mVideoUrl;
	private String mThumbnailUrl;

	public Video(String id, String title, int duration, String description, String videoUrl, String thumbnailUrl) {
		this.mId = id;
		this.mTitle = title;
		this.mDuration = duration;
		this.mDescription = description;
		this.mVideoUrl = videoUrl;
		this.mThumbnailUrl = thumbnailUrl;
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
	
	public String getDescription() {
		return mDescription;
	}
	
	public String getVideoUrl() {
		return mVideoUrl;
	}
	
	public String getThumbnailUrl() {
		return mThumbnailUrl;
	}
}
