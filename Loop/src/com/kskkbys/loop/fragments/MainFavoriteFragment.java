package com.kskkbys.loop.fragments;

import java.util.List;

import com.kskkbys.loop.R;
import com.kskkbys.loop.logger.KLog;
import com.kskkbys.loop.model.FavoriteList;
import com.kskkbys.loop.model.Video;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Fragment of favorite list.
 * @author Keisuke Kobayashi
 *
 */
public class MainFavoriteFragment extends Fragment {
	
	private static final String TAG = MainFavoriteFragment.class.getSimpleName();

	private VideoAdapter mAdapter;
	private ListView mListView;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		KLog.v(TAG, "onCreateView");
		View view = inflater.inflate(R.layout.fragment_main_favorite, container, false);
		mListView = (ListView)view.findViewById(R.id.favorite_listview);
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		KLog.v(TAG, "onActivityCreated");
		super.onActivityCreated(savedInstanceState);
		
		// Initialize list view.
		FavoriteList favlist = FavoriteList.getInstance(getActivity());
		List<Video> videos = favlist.getVideos();
		KLog.v(TAG, "videos " + videos.size());
		mAdapter = new VideoAdapter(getActivity(), videos);
		mListView.setAdapter(mAdapter);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mAdapter.notifyDataSetChanged();
	}
	
	/**
	 * Adapter class for video list.
	 * @author Keisuke Kobayashi
	 *
	 */
	private static class VideoAdapter extends ArrayAdapter<Video> {

		private LayoutInflater mInflater;
		
		public VideoAdapter(Activity activity, List<Video> objects) {
			super(activity, R.layout.fragment_main_favorite, R.id.favorite_title, objects);
			mInflater = activity.getLayoutInflater();
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			KLog.v(TAG, "getView: " + position);
			View view = null;
			if (convertView == null) {
				view = mInflater.inflate(R.layout.row_favorite_list, null);
			} else {
				view = convertView;
			}
			// Title
			TextView titleView = (TextView)view.findViewById(R.id.favorite_title);
			titleView.setText(getItem(position).getTitle());
			// Thumbnail
			ImageView imageView = (ImageView)view.findViewById(R.id.favorite_thumbnail);
			ImageLoader imageLoader = ImageLoader.getInstance();
			imageLoader.displayImage(getItem(position).getThumbnailUrl(), imageView);
			
			return view;
		}
		
	}
}
