package com.kskkbys.loop.ui.fragments;

import java.util.List;

import com.kskkbys.loop.R;
import com.kskkbys.loop.logger.KLog;
import com.kskkbys.loop.model.Artist;
import com.kskkbys.loop.model.Playlist;
import com.kskkbys.loop.model.SearchHistory;
import com.kskkbys.loop.ui.MainActivity;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AbsListView.RecyclerListener;
import android.widget.AdapterView.OnItemLongClickListener;

/**
 * History screen
 * @author Keisuke Kobayashi
 *
 */
public class MainHistoryFragment extends Fragment {

	private static final String TAG = MainHistoryFragment.class.getSimpleName();

	// ListView
	public static final int IMAGE_COUNT_PER_ROW = 5;
	private ArtistAdapter mAdapter;
	private ListView mListView;
	private int mLongSelectedPosition;
	private View mLongSelectedItem;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		KLog.v(TAG, "onAttach");
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		KLog.v(TAG,"onCreate");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		KLog.v(TAG, "onCreateView");
		View view = inflater.inflate(R.layout.fragment_main_history, container, false);

		// ListView selection
		mLongSelectedPosition = -1;
		mLongSelectedItem = null;

		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		KLog.v(TAG, "onActivityCreated");
		super.onActivityCreated(savedInstanceState);

		// Set up listview and empty view
		View view = getView();
		mListView = (ListView)view.findViewById(R.id.main_search_history);
		View emptyView = view.findViewById(R.id.main_empty);
		emptyView.findViewById(R.id.main_search_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MainActivity activity = (MainActivity)getActivity();
				activity.expandSearchView();
			}
		});
		mListView.setEmptyView(emptyView);
		mListView.setRecyclerListener(new RecyclerListener() {
			@Override
			public void onMovedToScrapHeap(View view) {
				//
				KLog.v(TAG, "onMoveToScrapHeap");
			}
		});
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				KLog.v(TAG, "onItemClick");
				//Selected artist and current artist
				ListView listView = (ListView) parent;
				Artist artist = (Artist) listView.getItemAtPosition(position);

				// Search or go to video player
				MainActivity activity = (MainActivity)getActivity();
				activity.searchOrGoToPlayer(artist);

			}
		});
		mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View view,
					int position, long id) {
				// Show contextual action bar
				MainActivity parent = (MainActivity)getActivity();
				if (parent.startActionModeByLongClick()) {
					mLongSelectedPosition = position;
					mLongSelectedItem = view;
					view.setSelected(true);
				}
				return true;
			}
		});
		// Set adapter when activity is created
		SearchHistory history = SearchHistory.getInstance(getActivity());
		history.readHistory();	// initialize
		mAdapter = new ArtistAdapter(getActivity(), history.getArtists());
		mListView.setAdapter(mAdapter);
		
		// If a video is playing, show notification at bottom
		updatePlayingNotification();
	}

	@Override
	public void onResume() {
		KLog.v(TAG, "onResume");
		super.onResume();
		updateHistoryUI();
		updatePlayingNotification();
	}

	@Override
	public void onDetach() {
		super.onDetach();
		KLog.v(TAG, "onDetach");
	}

	/**
	 * Update history view
	 */
	public void updateHistoryUI() {
		KLog.v(TAG, "updateHistoryUI");
		mAdapter.notifyDataSetChanged();
	}

	/**
	 * Delete the selected artist from history.
	 * @param position
	 */
	public void clearLongSelectedHistory() {
		Artist artist = mAdapter.getItem(mLongSelectedPosition);
		SearchHistory.getInstance(getActivity()).removeArtist(artist);
	}

	/**
	 * Release selection.
	 */
	public void deselect() {
		KLog.v(TAG, "deselect");
		if (mLongSelectedItem != null) {
			mLongSelectedItem.setSelected(false);
		}
	}

	// TODO Playing notification will be another fragment.
	private void updatePlayingNotification() {
		if (Playlist.getInstance().getCurrentVideo() != null) {
			RelativeLayout base = (RelativeLayout)getView().findViewById(R.id.main_base);
			View notification = base.findViewById(R.id.notification_base);
			if (notification == null) {
				// Add
				final MainActivity parent = (MainActivity)getActivity();
				notification = parent.getLayoutInflater().inflate(R.layout.main_playing_notification, null);
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				params.addRule(RelativeLayout.CENTER_HORIZONTAL);
				notification.setLayoutParams(params);
				notification.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						parent.goToNextActivity();
					}
				});
				base.addView(notification);
			}
			// Update
			TextView title = (TextView)notification.findViewById(R.id.notification_title);
			title.setText(Playlist.getInstance().getCurrentVideo().getTitle());
		}
	}

	/**
	 * Adapter class of artist
	 * @author Keisuke Kobayashi
	 *
	 */
	private class ArtistAdapter extends ArrayAdapter<Artist> {

		private Activity mActivity;

		/**
		 * Constructor.
		 * @param activity
		 * @param objects
		 */
		public ArtistAdapter(Activity activity, List<Artist> objects) {
			super(activity, R.layout.row_search_history_list, R.id.search_history_artist, objects);
			mActivity = activity;
		}

		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent) {
			View view = convertView;
			String prevArtist = null;
			if (view == null) {
				LayoutInflater inflater = mActivity.getLayoutInflater();
				view = inflater.inflate(R.layout.row_search_history_list, parent, false);
			} else {
				TextView titleView = (TextView)view.findViewById(R.id.search_history_artist);
				prevArtist = titleView.getText().toString();
			}

			final Artist artist = getItem(position);

			// Set title
			TextView titleView = (TextView)view.findViewById(R.id.search_history_artist);
			titleView.setText(artist.name);

			// Set click / long click events
			setUpImageView((ListView)parent, view.findViewById(R.id.search_history_overlay), position);

			// Set background images
			LinearLayout container = (LinearLayout)view.findViewById(R.id.search_history_image_container);
			if (prevArtist == null || !prevArtist.equals(artist.name) || container.getChildCount() == 0) {
				// Reload images
				reloadImages(container, artist);
			}
			return view;
		}

		private void reloadImages(LinearLayout container, Artist artist) {
			container.removeAllViews();
			if (artist.imageUrls != null) {
				KLog.v(TAG, "Images are saved.");
				int size = Math.min(IMAGE_COUNT_PER_ROW, artist.imageUrls.size());
				for (int i=0; i<size; i++) {
					ImageView iv = new ImageView(getContext());
					LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
					iv.setLayoutParams(params);
					iv.setAdjustViewBounds(true);

					if (i % 2 == 0) {
						iv.setBackgroundColor(Color.WHITE);
					} else {
						iv.setBackgroundColor(Color.RED);
					}
					container.addView(iv);
					// Load image from URL
					ImageLoader imageLoader = ImageLoader.getInstance();
					imageLoader.displayImage(artist.imageUrls.get(i), iv);
				}
			}
		}

		/**
		 * Set click/long click events to invoke events of ListView.
		 * @param parent
		 * @param imageView
		 * @param position
		 */
		private void setUpImageView(final ListView parent, final View imageView, final int position) {
			imageView.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					KLog.v(TAG, "image onclikc");
					parent.setSelection(position);
					parent.performItemClick(v, position, v.getId());
				}
			});
			imageView.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					KLog.v(TAG, "image onlongclick");
					parent.setSelection(position);
					return false;
				}
			});
		}
	}

}