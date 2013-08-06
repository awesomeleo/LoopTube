package com.kskkbys.loop.ui;

import com.kskkbys.loop.BuildConfig;
import com.kskkbys.loop.R;
import com.kskkbys.loop.model.BlackList;
import com.kskkbys.loop.model.FavoriteList;
import com.kskkbys.loop.model.SearchHistory;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

/**
 * Settings screen.
 * For Android 2.x, this class does not use PreferenceFragment.
 * @author Keisuke Kobayashi
 *
 */
public class SettingsActivity extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		// Set click events
		Preference clearFavorite = findPreference(getString(R.string.loop_pref_clear_favorite_key));
		clearFavorite.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
				builder.setMessage(R.string.loop_pref_clear_favorite_dialog_msg);
				builder.setPositiveButton(R.string.loop_ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						FavoriteList.getInstance(SettingsActivity.this).clearFavorites();
						Toast.makeText(SettingsActivity.this, R.string.loop_pref_clear_favorite_toast, Toast.LENGTH_SHORT).show();
					}
				});
				builder.setNegativeButton(R.string.loop_cancel, null);
				builder.create().show();
				return true;
			}
		});
		
		Preference clearHistory = findPreference(getString(R.string.loop_pref_clear_history_key));
		clearHistory.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
				builder.setMessage(R.string.loop_pref_clear_history_dialog_msg);
				builder.setPositiveButton(R.string.loop_ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						SearchHistory.getInstance(SettingsActivity.this).clearAllHistory();
						Toast.makeText(SettingsActivity.this, R.string.loop_pref_clear_history_toast, Toast.LENGTH_SHORT).show();
					}
				});
				builder.setNegativeButton(R.string.loop_cancel, null);
				builder.create().show();
				return true;
			}
		});
		
		Preference clearBlackList = findPreference(getString(R.string.loop_pref_clear_black_list_key));
		clearBlackList.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
				builder.setMessage(R.string.loop_pref_clear_black_list_dialog_msg);
				builder.setPositiveButton(R.string.loop_ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						BlackList.getInstance(SettingsActivity.this).clear();
						Toast.makeText(SettingsActivity.this, R.string.loop_pref_clear_black_list_toast, Toast.LENGTH_SHORT).show();
					}
				});
				builder.setNegativeButton(R.string.loop_cancel, null);
				builder.create().show();
				return true;
			}
		});

		// Set version number
		String version = getVersion();
		if (BuildConfig.DEBUG) {
			version += "(DEBUG)";
		}
		// TODO
		// findPreference(getString(R.string.loop_pref_version_key)).setSummary(version);
	}

	/**
	 * Get version name
	 * @return
	 */
	private String getVersion() {
		PackageInfo packageInfo = null;
		try {
			packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return packageInfo.versionName;
	}
}
