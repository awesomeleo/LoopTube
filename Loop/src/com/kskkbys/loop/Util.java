package com.kskkbys.loop;

public class Util {

	/**
	 * Get 1:23 style duration
	 * @param msec
	 * @return
	 */
	public static String getDurationText(int msec) {
		int minutes = (msec / 1000) / 60;
		int seconds = (msec / 1000) % 60;
		String time = String.format("%d:%02d", minutes, seconds);
		return time;
	}
	
}
