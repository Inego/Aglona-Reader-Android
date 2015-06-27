package ru.parallelbooks.aglonareader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Path.FillType;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MainActivity extends Activity implements OnFileLoadingComplete {

	private ParallelTextView pTC;

	private ParallelTextData pTD;

	final static int REQUEST_CODE_SETTINGS = 1;
	final static int REQUEST_CODE_BOOK_INFO = 2;
	final static int REQUEST_CODE_LIBRARY = 3;
	final static int REQUEST_CODE_NAVIGATION = 4;

	final static int SEEKBAR_MODE_OFF = 0;
	final static int SEEKBAR_MODE_FONT = 1;
	final static int SEEKBAR_MODE_BRIGHTESS = 2;

	int seekBarMode;

	final static float FONT_SIZE_MIN = 8.0f;
	final static float FONT_SIZE_STEP = .025f;

	final static int DIALOG_OPEN_FILE = 1;

	FileLoaderTask<MainActivity> fileLoaderTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		// Remove notification bar
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_main);

		pTC = (ParallelTextView) findViewById(R.id.parallelTextView);

		boolean pTDExisted = ParallelTextData.instanceExists();

		pTD = ParallelTextData.getInstance();

		pTC.pTD = pTD;

		if (!pTDExisted) {

			// Set values from default settings

			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(this);

			PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

			pTD.HighlightFirstWords = prefs.getBoolean(
					"pref_key_highlight_first_words", false);
			pTD.HighlightFragments = prefs.getBoolean(
					"pref_key_highlight_fragments", false);
			
			pTD.setBrightness(prefs.getFloat("pref_key_highlight_brightness", 0.85f));

			pTD.bookOpened = prefs.getBoolean("load_file", false);

			// pTD.SetLayoutMode();

			pTD.fontProportion = prefs.getInt("font_proportion", 200);

			if (pTD.fontRangeSet)
				pTD.setFontSize(false);

			String serializedFileList = prefs.getString("files", "");

			if (serializedFileList != null && serializedFileList.length() > 0) {
				Object fromString = stringToObject(serializedFileList);
				pTD.fileUsageInfo = (ArrayList<FileUsageInfo>) fromString;
			}

			if (pTD.fileUsageInfo.size() == 0 || !pTD.bookOpened)
				startLibraryActivity();
			else
				LoadFromFile(pTD.fileUsageInfo.get(0).FileName);

		}

		seekBarMode = SEEKBAR_MODE_OFF;

		SeekBar sb = (SeekBar) findViewById(R.id.seekBar);

		sb.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {

				switch (seekBarMode) {
				case SEEKBAR_MODE_FONT:
					pTD.fontProportion = progress;
					pTD.setFontSize(true);
					break;
				case SEEKBAR_MODE_BRIGHTESS:
					pTD.brightness = 0.0004f * progress + 0.6f;
					pTD.SetColorsByBrightness();
					pTC.invalidate();
					break;
				}

			}
		});

		fileLoaderTask = (FileLoaderTask<MainActivity>) getLastNonConfigurationInstance();

		if (fileLoaderTask != null)
			fileLoaderTask.attach(this);

		pTC.updateNoBookVisibility();

	}

	@Override
	protected void onResume() {
		super.onResume();
		pTD.TurnAdvancedPopupOff();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public Object onRetainNonConfigurationInstance() {

		if (fileLoaderTask != null)
			fileLoaderTask.detach();

		return fileLoaderTask;

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		pTD.TurnAdvancedPopupOff();

	}

	@Override
	protected void onPause() {

		if (pTD.fileUsageInfo.size() > 0 && pTD.bookOpened) {
			FileUsageInfo f0 = pTD.fileUsageInfo.get(0);
			f0.layoutMode = pTD.LayoutMode;
			f0.Reversed = pTD.reversed;
			f0.SplitterRatio = pTD.SplitterRatio;
			f0.TopPair = pTD.CurrentPair;
		}

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		Editor edit = prefs.edit();

		String serializedList = objectToString(pTD.fileUsageInfo);

		edit.putString("files", serializedList);

		edit.putBoolean("load_file", pTD.bookOpened);
		
		edit.putFloat("pref_key_highlight_brightness", (float) pTD.brightness);

		edit.commit();

		super.onPause();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent intent;

		switch (item.getItemId()) {

		case R.id.menu_settings:
			intent = new Intent(this, PreferencesActivity.class);
			startActivityForResult(intent, REQUEST_CODE_SETTINGS);
			break;

		case R.id.menu_book_info:
			intent = new Intent(this, BookInfoActivity.class);
			startActivityForResult(intent, REQUEST_CODE_BOOK_INFO);
			break;

		case R.id.menu_navigation:
			intent = new Intent(this, ContentsActivity.class);
			startActivityForResult(intent, REQUEST_CODE_NAVIGATION);
			break;

		case R.id.menu_library:
			startLibraryActivity();
			break;

		}

		return super.onOptionsItemSelected(item);

	}

	public void startLibraryActivity() {
		Intent intent;
		intent = new Intent(this, LibraryActivity.class);
		startActivityForResult(intent, REQUEST_CODE_LIBRARY);
	}

	@Override
	protected void onDestroy() {

		super.onDestroy();
	}

	void setSeekBarMode(int newSeekBarMode) {

		seekBarMode = newSeekBarMode;

		SeekBar sb = (SeekBar) findViewById(R.id.seekBar);

		switch (seekBarMode) {
		case SEEKBAR_MODE_FONT:
			sb.setProgress(pTD.fontProportion);
			break;
		case SEEKBAR_MODE_BRIGHTESS:
			sb.setProgress((int) ((pTD.brightness - 0.6f) * 2500));
			break;
		}

		sb.setVisibility(seekBarMode == 0 ? View.INVISIBLE : View.VISIBLE);

		if (seekBarMode != 0) {
			pTD.TurnAdvancedPopupOff();
		} else {

			// Save font size to shared preferences

			SharedPreferences sharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(this);
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putInt("font_proportion", pTD.fontProportion);
			editor.commit();

		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == REQUEST_CODE_SETTINGS) {

			switch (resultCode) {

			case PreferencesActivity.SETTINGS_RESULT_FONT_SIZE:
				setSeekBarMode(SEEKBAR_MODE_FONT);
				return;

			case PreferencesActivity.SETTINGS_RESULT_HIGHLIGHT_BRIGHTNESS:
				setSeekBarMode(SEEKBAR_MODE_BRIGHTESS);
				return;
			}

		}

		else if (requestCode == REQUEST_CODE_LIBRARY) {
			switch (resultCode) {
			case LibraryActivity.OPEN_FILE:

				String fileName = data.getStringExtra("file_name");

				LoadFromFile(fileName);

				break;
			}
		}

		else if (requestCode == REQUEST_CODE_NAVIGATION) {
			switch (resultCode) {
			case ContentsActivity.GOTO_CHAPTER:

				if (data != null) {

					pTD.CurrentPair = data.getIntExtra("pair", 0);
					pTD.UpdateScreen();

				}

				break;
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private boolean RetrieveToTheTop(String fileName) {
		// Let's check whether there exists this file in the list

		for (int index = 0; index < pTD.fileUsageInfo.size(); index++) {
			if (pTD.fileUsageInfo.get(index).FileName.equals(fileName)) {
				if (index != 0) {
					FileUsageInfo toMove = pTD.fileUsageInfo.get(index);
					pTD.fileUsageInfo.remove(toMove);
					pTD.fileUsageInfo.add(0, toMove);
				}
				return true;
			}
		}

		FileUsageInfo fileUsageInfo = new FileUsageInfo();

		fileUsageInfo.FileName = fileName;

		pTD.fileUsageInfo.add(0, fileUsageInfo);

		return false;

	}

	private void LoadSettingsFromFileUsageInfo(FileUsageInfo f) {

		if (f.SplitterRatio == 0)
			f.SplitterRatio = 0.5F;

		pTD.reversed = f.Reversed;

		pTD.LayoutMode = f.layoutMode;

		setLayoutModeAsPreference();

		pTD.SetLayoutMode();

		pTD.SetSplitterPositionByRatio(f.SplitterRatio);

		if (pTD.Number() > 0) {
			if (f.TopPair >= pTD.Number())
				pTD.CurrentPair = pTD.Number() - 1;
			else
				pTD.CurrentPair = f.TopPair;

			pTD.ProcessLayoutChange(false);
		}

	}

	public void setLayoutModeAsPreference() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		Editor edit = prefs.edit();

		String modeString = "0";

		switch (pTD.LayoutMode) {
		case 0:
			modeString = "0";
			break;
		case 1:
			modeString = "1";
			break;
		case 2:
			modeString = "2";
			break;
		}

		edit.putString("pref_key_reading_mode", modeString);

		edit.commit();
	}

	private void LoadFromFile(String fileName) {

		fileLoaderTask = new FileLoaderTask<MainActivity>(fileName, this);

		fileLoaderTask.execute();

	}

	@Override
	public void onFileLoadingComplete(boolean success, String fileName) {

		if (success) {

			if (RetrieveToTheTop(fileName))
				LoadSettingsFromFileUsageInfo(pTD.fileUsageInfo.get(0));
			else {
				pTD.CurrentPair = 0;
			}

			pTD.ProcessLayoutChange(false);

			pTD.bookOpened = true;
			pTC.updateNoBookVisibility();

		} else {
			pTD.clearParallelText();
			Toast.makeText(this, R.string.error_loading_file, Toast.LENGTH_LONG).show();
		}
		
		fileLoaderTask = null;

	}

	public static String objectToString(Serializable object) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			new ObjectOutputStream(out).writeObject(object);
			byte[] data = out.toByteArray();
			out.close();

			out = new ByteArrayOutputStream();
			Base64OutputStream b64 = new Base64OutputStream(out, Base64.DEFAULT);
			b64.write(data);
			b64.close();
			out.close();

			return new String(out.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Object stringToObject(String encodedObject) {
		try {
			return new ObjectInputStream(new Base64InputStream(
					new ByteArrayInputStream(encodedObject.getBytes()),
					Base64.DEFAULT)).readObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
