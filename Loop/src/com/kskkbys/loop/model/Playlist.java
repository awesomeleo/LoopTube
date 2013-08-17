package com.kskkbys.loop.model;

import java.util.ArrayList;
import java.util.List;


/**
 * Playlist class
 *
 */
public class Playlist {

	private static Playlist mInstance = new Playlist();
	
	private List<Video> mVideoList;
	private int mPlayingIndex;
	private String mQuery;
	
	/**
	 * Constructor
	 */
	private Playlist() {
		this.mVideoList = new ArrayList<Video>();
		this.mPlayingIndex = -1;
		this.mQuery = null;
	}
	
	/**
	 * Get instance of playlist
	 * @return
	 */
	public static Playlist getInstance() {
		return mInstance;
	}
	
	/**
	 * Get video list
	 * @return
	 */
	public List<Video> getVideos() {
		return mVideoList;
	}
	
	/**
	 * Get the current playing video
	 * @return
	 */
	public Video getCurrentVideo() {
		if (mVideoList != null && mPlayingIndex >= 0 && mPlayingIndex < mVideoList.size()) {
			return mVideoList.get(mPlayingIndex);
		} else {
			// invalid index or not initialized
			return null;
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public String getQuery() {
		return mQuery;
	}
	
	/**
	 * Set video list. The playing index is set to 0.
	 * @param query
	 * @param videos
	 * @param position
	 */
	public void setVideoList(String query, List<Video> videos, int position) {
		this.mQuery = query;
		this.mVideoList = videos;
		if (mVideoList != null && mVideoList.size() > 0) {
			this.mPlayingIndex = position;
		} else {
			this.mPlayingIndex = -1;
		}
	}
	
	/**
	 * Play previous video.
	 * If the old position is 0, set position the last index.
	 */
	public void prev() {
		this.mPlayingIndex--;
		// Go last element
		if (this.mPlayingIndex < 0 && mVideoList.size() > 0) {
			this.mPlayingIndex = mVideoList.size() - 1;
		}
	}
	
	/**
	 * Play next video.
	 * If the old position is last element, set position the first.
	 */
	public void next() {
		this.mPlayingIndex++;
		if (this.mPlayingIndex >= mVideoList.size()) {
			this.mPlayingIndex = 0;
		}
	}
	
	/**
	 * Set the playing index
	 * @param index
	 */
	public void setPlayingIndex(int index) {
		this.mPlayingIndex = index;
	}
	
	/**
	 * Get the playing index
	 * @return
	 */
	public int getPlayingIndex() {
		return mPlayingIndex;
	}
	
	/**
	 * Get count of video list
	 * @return
	 */
	public int getCount() {
		return mVideoList.size();
	}
	
	public Video getVideoAtIndex(int index) {
		return mVideoList.get(index);
	}
}
