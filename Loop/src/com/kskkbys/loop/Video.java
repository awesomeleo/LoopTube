package com.kskkbys.loop;

public class Video {
	
	//private static final String TAG = Video.class.getSimpleName();

	private String mId;
	private String mTitle;
	private int mDuration;
	private String mDescription;
	private String mVideoUrl;
	private String mThumbnailUrl;

	public Video(String id, String title, int duration) {
		this.mId = id;
		this.mTitle = title;
		this.mDuration = duration;
		
		this.mDescription = null;
		this.mVideoUrl = null;
		this.mThumbnailUrl = null;
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
	
	public void setDescription(String description) {
		mDescription = description;
	}
	
	public String getVideoUrl() {
		return mVideoUrl;
	}
	
	public void setVideoUrl(String videoUrl) {
		mVideoUrl = videoUrl;
	}
	
	public String getThumbnailUrl() {
		return mThumbnailUrl;
	}
	
	public void setThumbnailUrl(String thumbnailUrl) {
		mThumbnailUrl = thumbnailUrl;
	}
}
