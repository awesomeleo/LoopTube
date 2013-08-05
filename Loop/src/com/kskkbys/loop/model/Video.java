package com.kskkbys.loop.model;

public class Video {
	
	//private static final String TAG = Video.class.getSimpleName();

	private final String mId;
	private final String mTitle;
	private final int mDuration;
	private String mDescription;
	private String mVideoUrl;
	private String mThumbnailUrl;

	/**
	 * Constructor.
	 * @param id
	 * @param title
	 * @param duration
	 * @param description
	 * @param videoUrl
	 * @param thumbnailUrl
	 */
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
