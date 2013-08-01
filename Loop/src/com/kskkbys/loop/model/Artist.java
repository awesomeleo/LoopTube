package com.kskkbys.loop.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Artist {
	public String name;
	public List<String> imageUrls;
	public Date date;
	/**
	 * constructor
	 */
	public Artist() {
		name = null;
		imageUrls = new ArrayList<String>();
		date = null;
	}
}
