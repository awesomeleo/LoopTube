package com.kskkbys.loop.ui.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.kskkbys.loop.R;
import com.kskkbys.loop.logger.KLog;
import com.kskkbys.loop.model.BlackList;
import com.kskkbys.loop.model.Playlist;
import com.kskkbys.loop.model.Video;
import com.kskkbys.loop.service.PlayerCommand;
import com.kskkbys.loop.ui.VideoPlayerActivity;

/**
 * This class is a fragment of playlist.
 * @author keisuke.kobayashi
 *
 */
public class PlayerListFragment extends Fragment {

	private static final String TAG = PlayerListFragment.class.getSimpleName();

	private ListView mPlayListView;
	private VideoAdapter mAdapter;

	private ActionMode mActionMode;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_player_list, container, false);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// PlayListView
		mPlayListView = (ListView)getView().findViewById(R.id.playListView);
		mPlayListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				KLog.v(TAG, "onItemClick " + position);
				Video touchedVideo = Playlist.getInstance().getVideoAtIndex(position);
				BlackList bl = BlackList.getInstance(getActivity());
				if (bl.containsByUser(touchedVideo.getId()) || bl.containsByApp(touchedVideo.getId())) {
					// In blacklist: can not play it.
					KLog.v(TAG, "This video can not be played.");
					Toast.makeText(getActivity(), R.string.loop_video_player_ignored_already, Toast.LENGTH_SHORT).show();
				} else {
					Playlist.getInstance().setPlayingIndex(position);
					PlayerCommand.play(getActivity(), true);
				}
			}
		});
		mPlayListView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (mActionMode != null) {
					return false;
				}
				mPlayListView.setItemChecked(position, true);
				startContextualActionBar(position);
				return true;
			}
		});

		// Set adapter
		mAdapter = new VideoAdapter(getActivity());
		mPlayListView.setAdapter(mAdapter);
	}

	/**
	 * Update video info
	 */
	public void updateVideoInfo() {
		mAdapter.notifyDataSetChanged();
	}
	
	/**
	 * Start contextual action bar by long click of play list.
	 * @param position
	 */
	public void startContextualActionBar(final int position) {
		final VideoPlayerActivity activity = (VideoPlayerActivity)getActivity();
		mActionMode = activity.startSupportActionMode(new ActionMode.Callback() {
			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				MenuInflater inflater = activity.getMenuInflater();
				inflater.inflate(R.menu.activity_player_cab, menu);
				return true;
			}
			@Override
			public boolean onPrepareActionMode(ActionMode mode,
					Menu menu) {
				return false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode,
					MenuItem item) {
				switch (item.getItemId()) {
				case R.id.menu_ignore:
					ignoreVideo(position);
					mode.finish(); // Action picked, so close the CAB
					return true;
				default:
					return false;
				}
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				updateVideoInfo();
				mActionMode = null;
			}
		});
	}
	
	/**
	 * Add the selected video to blacklist
	 * @param position
	 */
	private void ignoreVideo(int position) {
		KLog.v(TAG, "ignoreVideo");
		Video video = Playlist.getInstance().getVideoAtIndex(position);
		if (video != null) {
			String videoId = video.getId();
			BlackList.getInstance(getActivity()).addUserBlackList(videoId);
			// update list view
			updateVideoInfo();
			// Show toast
			Toast.makeText(getActivity(), R.string.loop_video_player_ignored, Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Adapter for playlist view
	 * @author keisuke.kobayashi
	 */
	private static class VideoAdapter extends ArrayAdapter<Video> {

		private Activity mActivity;

		public VideoAdapter(Activity activity) {
			super(activity, R.layout.row_video_player_playlist, R.id.videoTitleViewInList, Playlist.getInstance().getVideos());
			mActivity = activity;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater inflater = (LayoutInflater)mActivity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(R.layout.row_video_player_playlist, null);
			}
			Video video = getItem(position);
			if (video != null) {
				// video title
				TextView textView = (TextView)v.findViewById(R.id.videoTitleViewInList);
				textView.setText(video.getTitle());
				// now playing or blacklist or nothing
				ImageView imageView = (ImageView)v.findViewById(R.id.nowPlayingImageInList);
				if (Playlist.getInstance().getPlayingIndex() == position) {
					imageView.setImageResource(R.drawable.volume_plus2);
				} else if (BlackList.getInstance(mActivity).containsByUser(video.getId())) {
					imageView.setImageResource(R.drawable.prohibited);
				} else if (BlackList.getInstance(mActivity).containsByApp(video.getId())) {
					imageView.setImageResource(R.drawable.circle_exclamation);
				} else {
					imageView.setImageBitmap(null);
				}
			}
			return v;
		}

	}
}
