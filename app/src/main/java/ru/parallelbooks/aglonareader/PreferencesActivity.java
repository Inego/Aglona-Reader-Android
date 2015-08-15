package ru.parallelbooks.aglonareader;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

public class PreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	
	public static final String KEY_READING_MODE = "pref_key_reading_mode";
	
	private ListPreference lpReadingMode;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		lpReadingMode = (ListPreference) getPreferenceScreen().findPreference(KEY_READING_MODE);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		ParallelTextData pTD = ParallelTextData.getInstance();
		
		if (key.equals("pref_key_highlight_first_words"))
			pTD.HighlightFirstWords = sharedPreferences.getBoolean(key, false);
		else if (key.equals("pref_key_highlight_fragments"))
			pTD.HighlightFragments= sharedPreferences.getBoolean(key, false);
		else if (key.equals(KEY_READING_MODE))
		{
			lpReadingMode.setSummary(lpReadingMode.getEntry());
			
			String layoutModeString = sharedPreferences.getString(KEY_READING_MODE, "0");
			
			if (layoutModeString.equals("0"))
				pTD.LayoutMode = 0;
			else if (layoutModeString.equals("1"))
				pTD.LayoutMode = 1;
			else if (layoutModeString.equals("2"))
				pTD.LayoutMode = 2;
			
			pTD.SetLayoutMode();
			pTD.ProcessLayoutChange(false);
			
		}
		
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}
}
