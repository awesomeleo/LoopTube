package com.kskkbys.loop.ui;

import com.kskkbys.loop.BuildConfig;
import com.kskkbys.loop.R;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceActivity;

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

		// Set version number
		String version = getVersion();
		if (BuildConfig.DEBUG) {
			version += "(DEBUG)";
		}
		findPreference(getString(R.string.loop_pref_version_key)).setSummary(version);
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
