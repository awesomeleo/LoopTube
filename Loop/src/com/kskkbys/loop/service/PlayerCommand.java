package com.kskkbys.loop.service;

import com.kskkbys.loop.model.LoopManager;

import android.content.Context;
import android.content.Intent;

public class PlayerCommand {

	/**
	 * Send PLAY command
	 * @param context	Context object.
	 * @param isReload	A flag whether video will be reloaded or not.
	 */
	public static void play(Context context, boolean isReload) {
		Intent intent = new Intent(context, VideoPlayerService.class);
		intent.putExtra(VideoPlayerService.COMMAND, VideoPlayerService.COMMAND_PLAY);
		intent.putExtra(VideoPlayerService.PLAY_RELOAD, isReload);
		context.startService(intent);
	}
	
	/**
	 * Send PAUSE command
	 * @param context
	 */
	public static void pause(Context context) {
		Intent intent = new Intent(context, VideoPlayerService.class);
		intent.putExtra(VideoPlayerService.COMMAND, VideoPlayerService.COMMAND_PAUSE);
		context.startService(intent);
	}
	
	/**
	 * Send NEXT command
	 * @param context
	 */
	public static void next(Context context) {
		Intent intent = new Intent(context, VideoPlayerService.class);
		intent.putExtra(VideoPlayerService.COMMAND, VideoPlayerService.COMMAND_NEXT);
		context.startService(intent);
	}
	
	/**
	 * Send PREV command
	 * @param context
	 */
	public static void prev(Context context) {
		Intent intent = new Intent(context, VideoPlayerService.class);
		intent.putExtra(VideoPlayerService.COMMAND, VideoPlayerService.COMMAND_PREV);
		context.startService(intent);
	}
	
	/**
	 * Send LOOPING command
	 * @param context
	 * @param isLooping
	 */
	public static void setLooping(Context context, boolean isLooping) {
		LoopManager.getInstance().setLoop(isLooping);
		Intent intent = new Intent(context, VideoPlayerService.class);
		intent.putExtra(VideoPlayerService.COMMAND, VideoPlayerService.COMMAND_LOOPING);
		intent.putExtra(VideoPlayerService.LOOPING, isLooping);
		context.startService(intent);
	}
	
	/**
	 * SEEK command
	 * @param context
	 * @param msec
	 */
	public static void seek(Context context, int msec) {
		Intent intent = new Intent(context, VideoPlayerService.class);
		intent.putExtra(VideoPlayerService.COMMAND, VideoPlayerService.COMMAND_SEEK);
		intent.putExtra(VideoPlayerService.SEEK_MSEC, msec);
		context.startService(intent);
	}
}
