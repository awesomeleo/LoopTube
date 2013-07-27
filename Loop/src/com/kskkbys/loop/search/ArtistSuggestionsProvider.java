package com.kskkbys.loop.search;

import android.content.SearchRecentSuggestionsProvider;

/**
 * Aerist search suggestions.
 * @author keisuke.kobayashi
 *
 */
public class ArtistSuggestionsProvider extends SearchRecentSuggestionsProvider {

	public final static String AUTHORITY = ArtistSuggestionsProvider.class.getName();
	
	public final static int MODE = DATABASE_MODE_QUERIES;

	public ArtistSuggestionsProvider() {
		setupSuggestions(AUTHORITY, MODE);
	}
}
