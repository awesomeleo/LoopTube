package com.kskkbys.loop.model;

public class Video {
	
	//private static final String TAG = Video.class.getSimpleName();

	private final String mId;
	private final String mTitle;
	private final int mDuration;
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
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ID=" + mId + ", ");
		sb.append("Title=" + mTitle + ", ");
		sb.append("Duration=" + mDuration + ", ");
		sb.append("Description=" + mDescription + ", ");
		sb.append("VideoUrl=" + mVideoUrl + ", ");
		sb.append("ThumbnailUrl=" + mThumbnailUrl + ", ");
		return sb.toString();
	}
}
