package com.kskkbys.loop;

//
public class YouTubeSearchResult {

	public String encoding;
	public Feed feed;

	// feed
	static class Feed {

		public Category[] category;
		public Entry[] entry;

		// category
		static class Category {
			public String scheme;
			public String term;
		}
		
		// entry
		static class Entry {
			public Link[] link;
			public Id id;
			public Media$Group media$group;
			public Title title;
			
			//link
			static class Link {
				public String href;
				public String rel;
				public String type;
			}
			
			// id
			static class Id {
				public String $t;
			}
			
			// media$group
			static class Media$Group {
				public Media$Content[] media$content;
				
				// media$content
				static class Media$Content {
					public int duration;
					public String expression;
					//public String isDefault;
					public String medium;
					public String type;
					public String url;
					public int ys$format;
				}
			}
			
			// title
			static class Title {
				public String $t;
				public String type;
			}
		}
	}
}
